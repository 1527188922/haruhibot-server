package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.features.jmcomic.model.JmTaskAction;
import com.haruhi.botserver.features.jmcomic.model.JmTaskInfo;
import com.haruhi.botserver.features.jmcomic.model.JmTaskSnapshot;
import com.haruhi.botserver.features.jmcomic.model.JmTaskStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JM任务队列的行为测试，纯内存，不依赖Spring与网络
 */
class JmTaskQueueTest {

    @Test
    void executesQueuedTasksInSubmitOrder() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch completed = new CountDownLatch(2);

        queue.tryAcquire("100", JmTaskAction.DOWNLOAD.getActionName());
        queue.enqueue("100", JmTaskAction.DOWNLOAD, "album100", null, recordingBody("100", order, completed));
        queue.tryAcquire("200", JmTaskAction.GENERATE_ZIP.getActionName());
        queue.enqueue("200", JmTaskAction.GENERATE_ZIP, "album200", null, recordingBody("200", order, completed));

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(List.of("100", "200"), order);

        JmTaskSnapshot snapshot = queue.snapshot(false);
        assertEquals(0, snapshot.getCounters().getRunning());
        assertEquals(0, snapshot.getCounters().getQueued());
        assertEquals(2, snapshot.getCounters().getSuccess());
        assertEquals(2, snapshot.getFinishedList().size());
        assertFalse(snapshot.isParallel());
    }

    /**
     * 回归：工作线程处理完任务后会停在 wait()，之后提交的任务必须被唤醒执行，不能一直卡在"排队中"
     */
    @Test
    void executesTaskSubmittedAfterWorkerBecomesIdle() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch firstCompleted = new CountDownLatch(1);

        queue.tryAcquire("100", JmTaskAction.DOWNLOAD.getActionName());
        queue.enqueue("100", JmTaskAction.DOWNLOAD, "album100", null, recordingBody("100", order, firstCompleted));
        assertTrue(firstCompleted.await(5, TimeUnit.SECONDS));

        // 让工作线程确实回到等待状态，再提交下一个任务
        Thread.sleep(200);
        assertEquals(0, queue.snapshot(false).getCounters().getQueued());

        CountDownLatch secondCompleted = new CountDownLatch(1);
        queue.tryAcquire("200", JmTaskAction.GENERATE_PDF.getActionName());
        queue.enqueue("200", JmTaskAction.GENERATE_PDF, "album200", null, recordingBody("200", order, secondCompleted));

        assertTrue(secondCompleted.await(5, TimeUnit.SECONDS), "工作线程空闲后提交的任务没有被执行");
        assertEquals(List.of("100", "200"), order);
        assertEquals(JmTaskStatusEnum.SUCCESS.name(), queue.snapshot(false).getFinishedList().getFirst().getStatus());
    }

    @Test
    void cancelsQueuedTaskAndReleasesAlbum() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        CountDownLatch runningStarted = new CountDownLatch(1);
        CountDownLatch releaseRunning = new CountDownLatch(1);
        CountDownLatch firstCompleted = new CountDownLatch(1);
        CountDownLatch queuedCancelled = new CountDownLatch(1);

        queue.tryAcquire("100", JmTaskAction.DOWNLOAD.getActionName());
        String runningTaskId = queue.enqueue("100", JmTaskAction.DOWNLOAD, "album100", null, new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                runningStarted.countDown();
                try {
                    releaseRunning.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return JmTaskQueue.TaskResult.success("ok");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
                firstCompleted.countDown();
            }

            @Override
            public void onCancelled(String message) {
            }
        });
        assertTrue(runningStarted.await(5, TimeUnit.SECONDS));

        queue.tryAcquire("200", JmTaskAction.GENERATE_ZIP.getActionName());
        String queuedTaskId = queue.enqueue("200", JmTaskAction.GENERATE_ZIP, "album200", null, new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                return JmTaskQueue.TaskResult.success("should not run");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
            }

            @Override
            public void onCancelled(String message) {
                queuedCancelled.countDown();
            }
        });

        JmTaskSnapshot beforeCancel = queue.snapshot(false);
        assertEquals(1, beforeCancel.getCounters().getRunning());
        assertEquals(1, beforeCancel.getCounters().getQueued());
        assertEquals(1, beforeCancel.getQueuedList().getFirst().getQueuePosition());
        assertTrue(beforeCancel.getQueuedList().getFirst().isCancellable());

        // 排队中的任务可以取消，并回调业务方
        assertTrue(queue.cancel(queuedTaskId));
        assertTrue(queuedCancelled.await(5, TimeUnit.SECONDS));
        assertEquals(JmTaskStatusEnum.CANCELLED, queue.statusOf(queuedTaskId));
        // 取消后同一个JM可以重新提交
        assertNull(queue.tryAcquire("200", JmTaskAction.GENERATE_ZIP.getActionName()));
        queue.release("200");

        // 正在执行的任务不支持取消
        assertFalse(queue.cancel(runningTaskId));
        assertEquals(JmTaskStatusEnum.RUNNING, queue.statusOf(runningTaskId));

        releaseRunning.countDown();
        assertTrue(firstCompleted.await(5, TimeUnit.SECONDS));
        assertEquals(JmTaskStatusEnum.SUCCESS, queue.statusOf(runningTaskId));

        JmTaskSnapshot afterFinish = queue.snapshot(false);
        assertEquals(1, afterFinish.getCounters().getSuccess());
        assertEquals(1, afterFinish.getCounters().getCancelled());
    }

    @Test
    void cancelAllQueuedTasks() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        CountDownLatch runningStarted = new CountDownLatch(1);
        CountDownLatch releaseRunning = new CountDownLatch(1);

        queue.tryAcquire("100", JmTaskAction.DOWNLOAD.getActionName());
        queue.enqueue("100", JmTaskAction.DOWNLOAD, "album100", null, new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                runningStarted.countDown();
                try {
                    releaseRunning.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return JmTaskQueue.TaskResult.success("ok");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
            }

            @Override
            public void onCancelled(String message) {
            }
        });
        assertTrue(runningStarted.await(5, TimeUnit.SECONDS));

        queue.tryAcquire("200", JmTaskAction.GENERATE_ZIP.getActionName());
        queue.enqueue("200", JmTaskAction.GENERATE_ZIP, "album200", null, simpleBody());
        queue.tryAcquire("300", JmTaskAction.GENERATE_PDF.getActionName());
        queue.enqueue("300", JmTaskAction.GENERATE_PDF, "album300", null, simpleBody());

        assertEquals(2, queue.cancelQueued());
        assertEquals(0, queue.snapshot(false).getCounters().getQueued());
        // 被取消的JM占位都已释放
        assertNull(queue.tryAcquire("200", JmTaskAction.GENERATE_ZIP.getActionName()));
        assertNull(queue.tryAcquire("300", JmTaskAction.GENERATE_PDF.getActionName()));
        releaseRunning.countDown();
    }

    @Test
    void reportsChapterAndImageProgress() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        CountDownLatch completed = new CountDownLatch(1);

        queue.tryAcquire("300", JmTaskAction.DOWNLOAD.getActionName());
        queue.enqueue("300", JmTaskAction.DOWNLOAD, null, null, new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                JmTaskQueue.ProgressReporter reporter = JmTaskContext.current();
                assertNotNull(reporter, "任务线程上应该能拿到进度上报口");
                reporter.stage("下载漫画图片");
                reporter.albumName("补全的本子名");
                reporter.chapter(2, 5, "第2话");
                reporter.chapterImages(10, 3);
                reporter.imageDownloaded();
                reporter.imageDownloaded();
                return JmTaskQueue.TaskResult.success("ok");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
                // 任务结束后必须清理线程上下文，避免常驻工作线程残留
                assertNull(JmTaskContext.current());
                completed.countDown();
            }

            @Override
            public void onCancelled(String message) {
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        JmTaskInfo info = queue.snapshot(false).getFinishedList().getFirst();
        assertEquals("下载漫画图片", info.getStage());
        assertEquals("补全的本子名", info.getAlbumName());
        assertEquals(2, info.getChapterIndex());
        assertEquals(5, info.getChapterTotal());
        assertEquals("第2话", info.getChapterTitle());
        assertEquals(10, info.getImageTotal());
        assertEquals(5, info.getImageDownloaded());
        assertEquals(JmTaskStatusEnum.SUCCESS.name(), info.getStatus());
        assertFalse(info.isCancellable());
    }

    /**
     * 任务的生命周期与进度变化都要通知到推送方
     */
    @Test
    void changeNotifierFiresOnLifecycleAndProgress() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        AtomicInteger notified = new AtomicInteger();
        queue.setChangeNotifier(notified::incrementAndGet);

        CountDownLatch completed = new CountDownLatch(1);
        queue.tryAcquire("300", JmTaskAction.DOWNLOAD.getActionName());
        queue.enqueue("300", JmTaskAction.DOWNLOAD, "album300", null, new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                JmTaskQueue.ProgressReporter reporter = JmTaskContext.current();
                reporter.stage("下载漫画图片");
                reporter.chapter(1, 2, "第1话");
                reporter.chapterImages(10, 0);
                reporter.imageDownloaded();
                return JmTaskQueue.TaskResult.success("ok");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
                completed.countDown();
            }

            @Override
            public void onCancelled(String message) {
            }
        });

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        // 入队 + 开始执行 + 4次进度上报 + 完成
        assertTrue(notified.get() >= 7, "任务变化通知次数不足:" + notified.get());
    }

    /**
     * 通知方异常不能影响任务执行
     */
    @Test
    void brokenChangeNotifierDoesNotBreakQueue() throws Exception {
        JmTaskQueue queue = new JmTaskQueue();
        queue.setChangeNotifier(() -> {
            throw new IllegalStateException("notifier boom");
        });

        CountDownLatch completed = new CountDownLatch(1);
        List<String> order = Collections.synchronizedList(new ArrayList<>());
        queue.tryAcquire("100", JmTaskAction.DOWNLOAD.getActionName());
        String taskId = queue.enqueue("100", JmTaskAction.DOWNLOAD, "album100", null,
                recordingBody("100", order, completed));

        assertTrue(completed.await(5, TimeUnit.SECONDS));
        assertEquals(JmTaskStatusEnum.SUCCESS, queue.statusOf(taskId));
    }

    private JmTaskQueue.TaskBody recordingBody(String aid, List<String> order, CountDownLatch completed) {
        return new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                order.add(aid);
                return JmTaskQueue.TaskResult.success("ok");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
                completed.countDown();
            }

            @Override
            public void onCancelled(String message) {
            }
        };
    }

    private JmTaskQueue.TaskBody simpleBody() {
        return new JmTaskQueue.TaskBody() {
            @Override
            public JmTaskQueue.TaskResult run() {
                return JmTaskQueue.TaskResult.success("ok");
            }

            @Override
            public void complete(JmTaskQueue.TaskResult result) {
            }

            @Override
            public void onCancelled(String message) {
            }
        };
    }
}
