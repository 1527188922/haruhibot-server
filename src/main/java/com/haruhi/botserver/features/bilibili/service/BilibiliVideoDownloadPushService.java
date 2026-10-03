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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * b站视频下载任务的实时推送
 * <p>
 * 与JM任务推送同一套机制：下载线程只置脏标记，由单个调度线程按固定频率节流推送，
 * 快照内容没有变化时不重复发送；没有订阅者时零开销。
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

    private static final long TICK_MILLIS = 300;

    private final BilibiliVideoDownloadService downloadService;
    private final WebuiWsSessionRegistry sessionRegistry;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    /**
     * 上次推送的快照签名，用于去重
     */
    private volatile String lastSignature;
    private ScheduledExecutorService ticker;

    public BilibiliVideoDownloadPushService(BilibiliVideoDownloadService downloadService,
                                            WebuiWsSessionRegistry sessionRegistry) {
        this.downloadService = downloadService;
        this.sessionRegistry = sessionRegistry;
    }

    @PostConstruct
    public void start() {
        downloadService.setChangeNotifier(() -> dirty.set(true));
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "bili-video-download-push");
            thread.setDaemon(true);
            return thread;
        });
        ticker.scheduleWithFixedDelay(this::tick, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        try {
            downloadService.setChangeNotifier(null);
        } catch (Exception e) {
            log.warn("注销b站视频下载任务变化通知失败:{}", e.getMessage());
        }
        if (ticker != null) {
            ticker.shutdownNow();
        }
    }

    private void tick() {
        try {
            if (!dirty.compareAndSet(true, false)) {
                return;
            }
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
        // 首帧后同步签名，避免紧接着的tick把同一份快照再推一次
        lastSignature = signature(snapshot);
        return WebuiWsMessage.of(MESSAGE_TYPE_SNAPSHOT, snapshot);
    }

    /**
     * 快照签名。下载中的任务带上进度（进度变化要推送），
     * 已完成的任务只保留状态与结果，避免同一份结果反复推送
     */
    static String signature(BilibiliVideoDownloadSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(256);
        appendTasks(builder, snapshot.getRunningList(), true);
        appendTasks(builder, snapshot.getFinishedList(), false);
        return builder.toString();
    }

    private static void appendTasks(StringBuilder builder, List<BilibiliVideoDownloadTask> tasks, boolean withProgress) {
        builder.append('#');
        if (tasks == null) {
            return;
        }
        for (BilibiliVideoDownloadTask task : tasks) {
            builder.append(task.getTaskId()).append(':')
                    .append(task.getStatus()).append(':');
            if (withProgress) {
                builder.append(task.getDownloadedBytes()).append('/').append(task.getTotalBytes()).append(':')
                        .append(task.getPercent()).append(':')
                        .append(task.getSpeed()).append(':');
            }
            builder.append(task.getMessage()).append(';');
        }
    }
}
