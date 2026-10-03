package com.haruhi.botserver.features.bilibili.service;

import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadSnapshot;
import com.haruhi.botserver.features.bilibili.model.BilibiliVideoDownloadTask;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsMessage;
import com.haruhi.botserver.infrastructure.web.websocket.WebuiWsSessionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * b站视频下载任务推送测试。
 * <p>
 * 签名只跟"任务id + 状态 + 失败原因"有关：只有时间字段变化不应触发推送，
 * 状态或失败原因变化必须触发推送；推送是事件驱动的，不依赖任何定时线程。
 */
class BilibiliVideoDownloadPushServiceTest {

    /**
     * 状态变化立刻推送，且空闲时不推送（证明没有定时轮询）
     */
    @Test
    void pushOnChangeOnly() throws Exception {
        StubDownloadService downloadService = new StubDownloadService();
        RecordingRegistry registry = new RecordingRegistry();
        BilibiliVideoDownloadPushService pushService = new BilibiliVideoDownloadPushService(downloadService, registry);
        pushService.start();
        try {
            Thread.sleep(800);
            assertEquals(0, registry.messages.size(), "没有状态变化时不应有任何推送");

            downloadService.snapshot = snapshot(task("BV1", BilibiliVideoDownloadTask.STATUS_RUNNING, null));
            downloadService.notifyChange();
            awaitMessages(registry, 1);

            // 签名没变（只有时间字段不同）不重复推送
            downloadService.snapshot = snapshot(task("BV1", BilibiliVideoDownloadTask.STATUS_RUNNING, null));
            downloadService.notifyChange();
            Thread.sleep(400);
            assertEquals(1, registry.messages.size(), "状态未变化不应重复推送");

            downloadService.snapshot = snapshot(task("BV1", BilibiliVideoDownloadTask.STATUS_FAIL, "连接超时"));
            downloadService.notifyChange();
            awaitMessages(registry, 2);
            BilibiliVideoDownloadSnapshot payload = (BilibiliVideoDownloadSnapshot) registry.messages.get(1).getData();
            assertEquals("连接超时", payload.getFinishedList().getFirst().getMessage());
        } finally {
            pushService.stop();
        }
    }

    /**
     * 连续快速变更要合并成尽量少的推送，但最后一次状态必须落到前端
     */
    @Test
    void rapidChangesAreCoalesced() throws Exception {
        StubDownloadService downloadService = new StubDownloadService();
        RecordingRegistry registry = new RecordingRegistry();
        BilibiliVideoDownloadPushService pushService = new BilibiliVideoDownloadPushService(downloadService, registry);
        pushService.start();
        try {
            // 让推送线程慢下来，制造"推送期间又有新变更"的排队窗口
            downloadService.snapshotDelayMillis = 150;
            for (int i = 1; i <= 10; i++) {
                downloadService.snapshot = snapshot(task("BV" + i, BilibiliVideoDownloadTask.STATUS_RUNNING, null));
                downloadService.notifyChange();
            }
            awaitLastTaskId(registry, "BV10");
            assertTrue(registry.messages.size() <= 3,
                    "10次快速变更应被合并成极少几次推送，实际=" + registry.messages.size());
        } finally {
            pushService.stop();
        }
    }

    @Test
    void signatureIgnoresTimeFields() {
        BilibiliVideoDownloadSnapshot first = snapshot(task(BilibiliVideoDownloadTask.STATUS_RUNNING, null, 100L, null));
        BilibiliVideoDownloadSnapshot later = snapshot(task(BilibiliVideoDownloadTask.STATUS_RUNNING, null, 9999L, null));

        assertEquals(BilibiliVideoDownloadPushService.signature(first), BilibiliVideoDownloadPushService.signature(later),
                "只有开始时间变化的快照不应触发推送");
    }

    @Test
    void signatureChangesWithStatus() {
        BilibiliVideoDownloadSnapshot running = snapshot(task(BilibiliVideoDownloadTask.STATUS_RUNNING, null, 100L, null));
        BilibiliVideoDownloadSnapshot success = snapshot(task(BilibiliVideoDownloadTask.STATUS_SUCCESS, null, 100L, 200L));

        assertNotEquals(BilibiliVideoDownloadPushService.signature(running),
                BilibiliVideoDownloadPushService.signature(success));
    }

    @Test
    void signatureChangesWithFailMessage() {
        BilibiliVideoDownloadSnapshot first = snapshot(task(BilibiliVideoDownloadTask.STATUS_FAIL, "连接超时", 100L, 200L));
        BilibiliVideoDownloadSnapshot second = snapshot(task(BilibiliVideoDownloadTask.STATUS_FAIL, "未获取到视频下载链接", 100L, 200L));

        assertNotEquals(BilibiliVideoDownloadPushService.signature(first),
                BilibiliVideoDownloadPushService.signature(second));
    }

    @Test
    void signatureHandlesNullAndEmpty() {
        assertEquals("", BilibiliVideoDownloadPushService.signature(null));
        BilibiliVideoDownloadPushService.signature(new BilibiliVideoDownloadSnapshot());
    }

    private BilibiliVideoDownloadSnapshot snapshot(BilibiliVideoDownloadTask task) {
        BilibiliVideoDownloadSnapshot snapshot = new BilibiliVideoDownloadSnapshot();
        snapshot.setRunningList(task.isRunning() ? List.of(task) : List.of());
        snapshot.setFinishedList(task.isRunning() ? List.of() : List.of(task));
        BilibiliVideoDownloadSnapshot.Counters counters = new BilibiliVideoDownloadSnapshot.Counters();
        counters.setRunning(task.isRunning() ? 1 : 0);
        snapshot.setCounters(counters);
        return snapshot;
    }

    private BilibiliVideoDownloadTask task(String status, String message, Long startTime, Long endTime) {
        BilibiliVideoDownloadTask task = new BilibiliVideoDownloadTask();
        task.setTaskId("BV1ZsaB6HE2U_42369091370");
        task.setStatus(status);
        task.setStatusName(status);
        task.setMessage(message);
        task.setStartTime(startTime);
        task.setEndTime(endTime);
        return task;
    }

    /**
     * 用例里自己指定任务id，便于观察"最后一次状态有没有推出去"
     */
    private BilibiliVideoDownloadTask task(String taskId, String status, String message) {
        BilibiliVideoDownloadTask task = task(status, message, System.currentTimeMillis(), null);
        task.setTaskId(taskId);
        return task;
    }

    private void awaitMessages(RecordingRegistry registry, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && registry.messages.size() < expected) {
            Thread.sleep(20);
        }
        assertEquals(expected, registry.messages.size(), "等待推送超时");
    }

    /**
     * 推送是异步的，这里等到最后一次变更真的推出去了
     */
    private void awaitLastTaskId(RecordingRegistry registry, String taskId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            List<WebuiWsMessage> messages = registry.messages;
            if (!messages.isEmpty()) {
                BilibiliVideoDownloadSnapshot last = (BilibiliVideoDownloadSnapshot) messages.getLast().getData();
                if (!last.getRunningList().isEmpty() && taskId.equals(last.getRunningList().getFirst().getTaskId())) {
                    return;
                }
            }
            Thread.sleep(20);
        }
        throw new AssertionError("最后一次状态没有推送出去：" + taskId);
    }

    /**
     * 只记录广播，不依赖真实WebSocket会话
     */
    private static class RecordingRegistry extends WebuiWsSessionRegistry {

        final List<WebuiWsMessage> messages = new CopyOnWriteArrayList<>();

        @Override
        public int subscriberCount(String topic) {
            return 1;
        }

        @Override
        public void broadcastToTopic(String topic, WebuiWsMessage message) {
            messages.add(message);
        }
    }

    /**
     * 可控的下载服务：快照由用例指定，变更通知由用例触发
     */
    private static class StubDownloadService extends BilibiliVideoDownloadService {

        final AtomicReference<Runnable> notifier = new AtomicReference<>();
        volatile BilibiliVideoDownloadSnapshot snapshot = new BilibiliVideoDownloadSnapshot();
        volatile long snapshotDelayMillis;

        @Override
        public void setChangeNotifier(Runnable changeNotifier) {
            notifier.set(changeNotifier);
        }

        @Override
        public BilibiliVideoDownloadSnapshot snapshot() {
            long delay = this.snapshotDelayMillis;
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return snapshot;
        }

        void notifyChange() {
            Runnable runnable = notifier.get();
            assertTrue(runnable != null, "推送服务启动后应注册变更通知");
            runnable.run();
        }
    }
}
