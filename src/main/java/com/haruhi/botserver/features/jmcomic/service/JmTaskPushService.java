package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.features.jmcomic.model.JmTaskInfo;
import com.haruhi.botserver.features.jmcomic.model.JmTaskSnapshot;
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
 * JM任务实时推送
 * <p>
 * 任务队列发生变化时置脏标记，由单个调度线程按固定频率节流推送：
 * 变化(入队/开始/结束/取消)会在几百毫秒内送达，图片进度按节流合并，
 * 快照内容没变化时不重复发送。没有订阅者时零开销。
 * <p>
 * 任务状态只在内存中，推送也只在内存中完成，不持久化
 */
@Slf4j
@Component
public class JmTaskPushService implements WebuiWsTopicProvider {

    /**
     * 订阅主题
     */
    public static final String TOPIC = "jm.task";
    /**
     * 推送消息类型
     */
    public static final String MESSAGE_TYPE_SNAPSHOT = "jm.task.snapshot";
    /**
     * 前端主动拉取当前快照的命令
     */
    public static final String COMMAND_LIST = "jm.task.list";

    private static final long TICK_MILLIS = 300;

    private final JmcomicService jmcomicService;
    private final WebuiWsSessionRegistry sessionRegistry;
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    /**
     * 上次推送的快照签名，用于去重
     */
    private volatile String lastSignature;
    private ScheduledExecutorService ticker;

    public JmTaskPushService(JmcomicService jmcomicService, WebuiWsSessionRegistry sessionRegistry) {
        this.jmcomicService = jmcomicService;
        this.sessionRegistry = sessionRegistry;
    }

    @PostConstruct
    public void start() {
        jmcomicService.setTaskChangeNotifier(() -> dirty.set(true));
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "jm-task-push");
            thread.setDaemon(true);
            return thread;
        });
        ticker.scheduleWithFixedDelay(this::tick, TICK_MILLIS, TICK_MILLIS, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        try {
            jmcomicService.setTaskChangeNotifier(null);
        } catch (Exception e) {
            log.warn("注销JM任务变化通知失败:{}", e.getMessage());
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
            JmTaskSnapshot snapshot = jmcomicService.listTasks();
            String signature = signature(snapshot);
            if (signature.equals(lastSignature)) {
                return;
            }
            lastSignature = signature;
            sessionRegistry.broadcastToTopic(TOPIC, WebuiWsMessage.of(MESSAGE_TYPE_SNAPSHOT, snapshot));
        } catch (Exception e) {
            log.error("JM任务推送异常", e);
        }
    }

    @Override
    public String topic() {
        return TOPIC;
    }

    @Override
    public WebuiWsMessage initialMessage() {
        JmTaskSnapshot snapshot = jmcomicService.listTasks();
        // 首帧后同步签名，避免紧接着的tick把同一份快照再推一次
        lastSignature = signature(snapshot);
        return WebuiWsMessage.of(MESSAGE_TYPE_SNAPSHOT, snapshot);
    }

    /**
     * 快照签名，只包含会影响展示的字段。
     * 刻意排除 waitMillis/costMillis 这类随时间变化的值，避免无变化也反复推送
     */
    static String signature(JmTaskSnapshot snapshot) {
        if (snapshot == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(256);
        builder.append(snapshot.isParallel()).append('|');
        JmTaskSnapshot.Counters counters = snapshot.getCounters();
        if (counters != null) {
            builder.append(counters.getRunning()).append(',')
                    .append(counters.getQueued()).append(',')
                    .append(counters.getSuccess()).append(',')
                    .append(counters.getFail()).append(',')
                    .append(counters.getCancelled());
        }
        appendTasks(builder, snapshot.getRunningList());
        appendTasks(builder, snapshot.getQueuedList());
        appendTasks(builder, snapshot.getFinishedList());
        return builder.toString();
    }

    private static void appendTasks(StringBuilder builder, List<JmTaskInfo> tasks) {
        builder.append('#');
        if (tasks == null) {
            return;
        }
        for (JmTaskInfo task : tasks) {
            builder.append(task.getTaskId()).append(':')
                    .append(task.getStatus()).append(':')
                    .append(task.getStage()).append(':')
                    .append(task.getChapterIndex()).append('/').append(task.getChapterTotal()).append(':')
                    .append(task.getImageDownloaded()).append('/').append(task.getImageTotal()).append(':')
                    .append(task.getQueuePosition()).append(':')
                    .append(task.getMessage()).append(';');
        }
    }
}
