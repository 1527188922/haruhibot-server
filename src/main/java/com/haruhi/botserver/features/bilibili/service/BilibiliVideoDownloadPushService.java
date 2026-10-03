package com.haruhi.botserver.features.bilibili.service;

import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadSnapshot;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadTask;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsMessage;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsSessionRegistry;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsTopicProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * b站视频下载任务的状态推送。
 * <p>
 * 事件驱动：任务状态一变（开始下载/下载完成/下载失败）就推一次，空闲时没有任何线程在跑。
 * 下载线程本身不直接做WS广播（序列化+写连接是慢操作），只把通知丢给一个单线程执行器；
 * 用 {@link #pending} 合并同一瞬间的多次变更，避免一次状态变化推多帧。
 * <p>
 * JM任务推送用的是"定时线程+脏标记"，那是因为它下载过程中进度一直在变、需要按频率节流；
 * b站下载已经没有进度了，一次任务最多只有"开始/结束"两次状态变化，再挂个定时线程纯属浪费。
 */
@Slf4j
@Component
public class BilibiliVideoDownloadPushService implements WebuiWsTopicProvider {

    /**
     * 订阅主题
     */
    public static final String TOPIC = "bilibili.video.download";
    /**
     * 推送消息类型
     */
    public static final String MESSAGE_TYPE_SNAPSHOT = "bilibili.video.download.snapshot";
    /**
     * 前端主动拉取当前快照的命令
     */
    public static final String COMMAND_LIST = "bilibili.video.download.list";

    private final BilibiliVideoDownloadService downloadService;
    private final WebuiWsSessionRegistry sessionRegistry;
    /**
     * 是否已有一次推送在排队，用于合并连续变更
     */
    private final AtomicBoolean pending = new AtomicBoolean(false);
    /**
     * 上次推送的快照签名，用于去重
     */
    private volatile String lastSignature;
    private ExecutorService pushExecutor;

    public BilibiliVideoDownloadPushService(BilibiliVideoDownloadService downloadService,
                                            WebuiWsSessionRegistry sessionRegistry) {
        this.downloadService = downloadService;
        this.sessionRegistry = sessionRegistry;
    }

    @PostConstruct
    public void start() {
        pushExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "bili-video-download-push");
            thread.setDaemon(true);
            return thread;
        });
        downloadService.setChangeNotifier(this::onTaskChanged);
    }

    @PreDestroy
    public void stop() {
        try {
            downloadService.setChangeNotifier(null);
        } catch (Exception e) {
            log.warn("注销b站视频下载任务变化通知失败:{}", e.getMessage());
        }
        if (pushExecutor != null) {
            pushExecutor.shutdownNow();
        }
    }

    /**
     * 任务状态变更回调：只负责排队，具体推送在推送线程里做
     */
    private void onTaskChanged() {
        ExecutorService executor = this.pushExecutor;
        if (executor == null) {
            return;
        }
        // 已经排了一次就不用再排：这次推送读到的必然是最新快照
        if (!pending.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(this::push);
        } catch (Exception e) {
            pending.set(false);
            log.warn("提交b站视频下载任务推送失败:{}", e.getMessage());
        }
    }

    private void push() {
        // 先复位再取快照：这期间发生的新变更会重新排队，不会丢
        pending.set(false);
        try {
            if (sessionRegistry.subscriberCount(TOPIC) == 0) {
                return;
            }
            BilibiliVideoDownloadSnapshot snapshot = downloadService.snapshot();
            String signature = signature(snapshot);
            if (signature.equals(lastSignature)) {
                return;
            }
            lastSignature = signature;
            sessionRegistry.broadcastToTopic(TOPIC, WebuiWsMessage.of(MESSAGE_TYPE_SNAPSHOT, snapshot));
        } catch (Exception e) {
            log.error("b站视频下载任务推送异常", e);
        }
    }

    @Override
    public String topic() {
        return TOPIC;
    }

    @Override
    public WebuiWsMessage initialMessage() {
        BilibiliVideoDownloadSnapshot snapshot = downloadService.snapshot();
        // 首帧后同步签名，避免紧接着的变更推送把同一份快照再推一次
        lastSignature = signature(snapshot);
        return WebuiWsMessage.of(MESSAGE_TYPE_SNAPSHOT, snapshot);
    }

    /**
     * 快照签名。只关心状态与失败原因：下载过程没有进度，状态不变就不重复推送
     */
    static String signature(BilibiliVideoDownloadSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(256);
        appendTasks(builder, snapshot.getRunningList());
        appendTasks(builder, snapshot.getFinishedList());
        return builder.toString();
    }

    private static void appendTasks(StringBuilder builder, List<BilibiliVideoDownloadTask> tasks) {
        builder.append('#');
        if (tasks == null) {
            return;
        }
        for (BilibiliVideoDownloadTask task : tasks) {
            builder.append(task.getTaskId()).append(':')
                    .append(task.getStatus()).append(':')
                    .append(task.getMessage()).append(';');
        }
    }
}
