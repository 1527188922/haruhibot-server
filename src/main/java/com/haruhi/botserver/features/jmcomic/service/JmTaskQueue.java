package com.haruhi.botserver.features.jmcomic.service;

import com.haruhi.botserver.features.jmcomic.model.JmTaskAction;
import com.haruhi.botserver.features.jmcomic.model.JmTaskInfo;
import com.haruhi.botserver.features.jmcomic.model.JmTaskSnapshot;
import com.haruhi.botserver.features.jmcomic.model.JmTaskStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JM任务内存队列
 * <p>
 * 只存在于内存中，不持久化，进程重启即清空。
 * <p>
 * 串行模式：任务先进入等待队列，由单个常驻工作线程按提交顺序执行(等价于原来的单线程池)。
 * 并行模式：任务不排队，由调用线程直接执行(保持原有行为)，仅登记状态供管理端查看。
 * <p>
 * 只有"排队中"的任务支持取消：取消即从等待队列移除、释放该JM的占用并回调业务方。
 * 正在执行的任务无法取消，避免中断已开始的图片下载。
 */
@Slf4j
public class JmTaskQueue {

    /**
     * 最近完成任务保留条数
     */
    private static final int MAX_FINISHED = 20;
    /**
     * 最近完成任务保留时长
     */
    private static final long FINISHED_TTL_MILLIS = 10 * 60 * 1000L;
    private static final long POLL_INTERVAL_MILLIS = 2000L;

    /**
     * 任务体
     * <p>
     * run 只负责执行操作，complete/onCancelled 是业务回调。
     * 回调时该JM的占用已经释放，与原有实现保持一致。
     */
    public interface TaskBody {
        TaskResult run();

        void complete(TaskResult result);

        void onCancelled(String message);
    }

    public record TaskResult(boolean success, String message) {
        public static TaskResult success(String message) {
            return new TaskResult(true, message);
        }

        public static TaskResult fail(String message) {
            return new TaskResult(false, message);
        }
    }

    /**
     * 进度上报口。业务代码通过 {@link JmTaskContext#current()} 获取，不在任务线程上时返回 null
     */
    public interface ProgressReporter {
        void stage(String stage);

        void albumName(String albumName);

        void chapter(int index, int total, String title);

        void chapterImages(int total, int downloaded);

        void imageDownloaded();
    }

    private final Object lock = new Object();
    /**
     * aid -> 动作文案，替代原来的 LOCK_ACTION_MAP，同时表示"运行中或排队中"
     */
    private final Map<String, String> occupiedActions = new HashMap<>();
    private final Deque<JmTask> pending = new ArrayDeque<>();
    /**
     * 运行中的任务，保持登记顺序
     */
    private final Map<String, JmTask> running = new LinkedHashMap<>();
    private final Map<String, JmTask> tasksById = new HashMap<>();
    /**
     * 最近完成的任务，队首最新
     */
    private final Deque<JmTask> finished = new ArrayDeque<>();
    private Thread worker;
    /**
     * 任务状态/进度变化通知，用于向webui推送。实现必须轻量且非阻塞
     */
    private volatile Runnable changeNotifier;

    /**
     * 注册变化通知(由推送服务启动时注册)
     */
    public void setChangeNotifier(Runnable changeNotifier) {
        this.changeNotifier = changeNotifier;
    }

    /**
     * 通知任务有变化。绝不能因为通知方异常影响下载主流程
     */
    private void notifyChange() {
        Runnable notifier = this.changeNotifier;
        if (notifier == null) {
            return;
        }
        try {
            notifier.run();
        } catch (Exception e) {
            log.warn("JM任务变化通知异常:{}", e.getMessage());
        }
    }

    /**
     * 原子占位，避免同一个JM重复提交
     *
     * @return null 表示占位成功，否则返回占用者的动作文案
     */
    public String tryAcquire(String aid, String actionName) {
        synchronized (lock) {
            String occupied = occupiedActions.get(aid);
            if (occupied != null) {
                return occupied;
            }
            occupiedActions.put(aid, StringUtils.defaultString(actionName));
            return null;
        }
    }

    /**
     * 释放占位。任务执行结束、排队中被取消时调用
     */
    public void release(String aid) {
        synchronized (lock) {
            occupiedActions.remove(aid);
        }
    }

    /**
     * 串行模式：进入等待队列，由工作线程执行
     *
     * @return 任务id
     */
    public String enqueue(String aid, JmTaskAction action, String albumName, String coverUrl, TaskBody body) {
        JmTask task = new JmTask(aid, action, albumName, coverUrl, body, JmTaskStatusEnum.QUEUED);
        synchronized (lock) {
            pending.addLast(task);
            tasksById.put(task.taskId, task);
            // 必须唤醒工作线程：它处理完上一个任务后可能正停在 wait() 上
            lock.notifyAll();
        }
        ensureWorker();
        notifyChange();
        return task.taskId;
    }

    /**
     * 并行模式：登记为运行中并立即在当前线程执行
     */
    public TaskResult runNow(String aid, JmTaskAction action, String albumName, String coverUrl, TaskBody body) {
        JmTask task = new JmTask(aid, action, albumName, coverUrl, body, JmTaskStatusEnum.RUNNING);
        synchronized (lock) {
            task.startTime = System.currentTimeMillis();
            running.put(task.taskId, task);
            tasksById.put(task.taskId, task);
        }
        notifyChange();
        TaskResult result = null;
        try {
            JmTaskContext.set(task);
            result = body.run();
        } catch (Throwable t) {
            log.error("JM任务执行异常 aid:{} action:{}", aid, action.getActionName(), t);
            result = TaskResult.fail(action.getActionName() + "异常：" + t.getMessage());
        } finally {
            JmTaskContext.clear();
        }
        finish(task, result);
        notifyComplete(task, result);
        return result;
    }

    public JmTaskStatusEnum statusOf(String taskId) {
        synchronized (lock) {
            JmTask task = tasksById.get(taskId);
            return task == null ? null : task.status;
        }
    }

    /**
     * 取消排队中的任务
     *
     * @return true 表示取消成功
     */
    public boolean cancel(String taskId) {
        JmTask task;
        synchronized (lock) {
            task = tasksById.get(taskId);
            if (task == null || task.status != JmTaskStatusEnum.QUEUED) {
                return false;
            }
            task.status = JmTaskStatusEnum.CANCELLED;
            task.message = "已取消";
            task.endTime = System.currentTimeMillis();
            pending.remove(task);
            occupiedActions.remove(task.aid);
            addFinishedLocked(task);
            pruneLocked();
        }
        String message = "【JM" + task.aid + "】" + task.action.getActionName() + "已取消";
        try {
            task.body.onCancelled(message);
        } catch (Exception e) {
            log.error("JM任务取消回调异常 taskId:{}", taskId, e);
        }
        notifyChange();
        return true;
    }

    /**
     * 取消全部排队中的任务
     *
     * @return 实际取消数量
     */
    public int cancelQueued() {
        List<String> taskIds;
        synchronized (lock) {
            taskIds = pending.stream().map(e -> e.taskId).toList();
        }
        int count = 0;
        for (String taskId : taskIds) {
            if (cancel(taskId)) {
                count++;
            }
        }
        return count;
    }

    public JmTaskSnapshot snapshot(boolean parallel) {
        JmTaskSnapshot snapshot = new JmTaskSnapshot();
        JmTaskSnapshot.Counters counters = new JmTaskSnapshot.Counters();
        synchronized (lock) {
            pruneLocked();
            List<JmTaskInfo> queuedList = new ArrayList<>(pending.size());
            int position = 1;
            for (JmTask task : pending) {
                JmTaskInfo info = toInfo(task);
                info.setQueuePosition(position++);
                queuedList.add(info);
            }
            List<JmTaskInfo> finishedList = new ArrayList<>(finished.size());
            for (JmTask task : finished) {
                finishedList.add(toInfo(task));
                switch (task.status) {
                    case SUCCESS -> counters.setSuccess(counters.getSuccess() + 1);
                    case FAIL -> counters.setFail(counters.getFail() + 1);
                    case CANCELLED -> counters.setCancelled(counters.getCancelled() + 1);
                    default -> {
                    }
                }
            }
            counters.setRunning(running.size());
            counters.setQueued(pending.size());
            snapshot.setRunningList(running.values().stream().map(this::toInfo).toList());
            snapshot.setQueuedList(queuedList);
            snapshot.setFinishedList(finishedList);
        }
        snapshot.setParallel(parallel);
        snapshot.setCounters(counters);
        snapshot.setPollIntervalMillis(POLL_INTERVAL_MILLIS);
        return snapshot;
    }

    private void ensureWorker() {
        if (worker != null) {
            return;
        }
        synchronized (lock) {
            if (worker != null) {
                return;
            }
            Thread thread = new Thread(this::workerLoop, "jm-task-worker");
            thread.setDaemon(true);
            thread.start();
            worker = thread;
        }
    }

    private void workerLoop() {
        while (true) {
            JmTask task;
            synchronized (lock) {
                while (pending.isEmpty()) {
                    try {
                        // 带超时的等待：正常情况下由入队时的 notifyAll 唤醒，
                        // 超时兜底保证即使某次通知丢失，排队任务也不会被永久搁置
                        lock.wait(1000L);
                    } catch (InterruptedException e) {
                        // 队列线程不会被主动中断，忽略后继续等待
                        log.warn("JM任务队列等待被中断");
                    }
                }
                task = pending.pollFirst();
                task.status = JmTaskStatusEnum.RUNNING;
                task.startTime = System.currentTimeMillis();
                running.put(task.taskId, task);
            }
            notifyChange();
            TaskResult result = null;
            try {
                JmTaskContext.set(task);
                result = task.body.run();
            } catch (Throwable t) {
                log.error("JM任务执行异常 aid:{} action:{}", task.aid, task.action.getActionName(), t);
                result = TaskResult.fail(task.action.getActionName() + "异常：" + t.getMessage());
            } finally {
                JmTaskContext.clear();
            }
            finish(task, result);
            notifyComplete(task, result);
        }
    }

    private void notifyComplete(JmTask task, TaskResult result) {
        try {
            task.body.complete(result);
        } catch (Exception e) {
            log.error("JM任务完成回调异常 taskId:{}", task.taskId, e);
        }
    }

    /**
     * 落状态、释放占位、记录最近完成。调用方保证 result 非空
     */
    private void finish(JmTask task, TaskResult result) {
        TaskResult finalResult = result == null ? TaskResult.fail("任务被中断") : result;
        synchronized (lock) {
            task.status = finalResult.success() ? JmTaskStatusEnum.SUCCESS : JmTaskStatusEnum.FAIL;
            task.message = finalResult.message();
            task.endTime = System.currentTimeMillis();
            running.remove(task.taskId);
            occupiedActions.remove(task.aid);
            addFinishedLocked(task);
            pruneLocked();
        }
        notifyChange();
    }

    private void addFinishedLocked(JmTask task) {
        finished.addFirst(task);
    }

    /**
     * 清理过期的最近完成记录，避免内存无限增长
     */
    private void pruneLocked() {
        long now = System.currentTimeMillis();
        while (!finished.isEmpty()) {
            JmTask oldest = finished.peekLast();
            boolean expired = now - oldest.endTime > FINISHED_TTL_MILLIS;
            if (finished.size() <= MAX_FINISHED && !expired) {
                break;
            }
            finished.pollLast();
            tasksById.remove(oldest.taskId);
        }
    }

    private JmTaskInfo toInfo(JmTask task) {
        JmTaskInfo info = new JmTaskInfo();
        info.setTaskId(task.taskId);
        info.setAid(task.aid);
        info.setAlbumName(task.albumName);
        info.setCoverUrl(task.coverUrl);
        info.setAction(task.action.getCode());
        info.setActionName(task.action.getActionName());
        info.setStatus(task.status.name());
        info.setStatusName(task.status.getStatusName());
        info.setCancellable(task.status == JmTaskStatusEnum.QUEUED);
        info.setEnqueueTime(task.enqueueTime);
        info.setStartTime(task.startTime > 0 ? task.startTime : null);
        info.setEndTime(task.endTime > 0 ? task.endTime : null);
        if (task.startTime > 0) {
            info.setWaitMillis(task.startTime - task.enqueueTime);
        }
        if (task.startTime > 0 && task.endTime > 0) {
            info.setCostMillis(task.endTime - task.startTime);
        }
        info.setMessage(task.message);
        info.setStage(task.stage);
        info.setChapterIndex(task.chapterIndex);
        info.setChapterTotal(task.chapterTotal);
        info.setChapterTitle(task.chapterTitle);
        info.setImageTotal(task.imageTotal);
        info.setImageDownloaded(task.imageDownloaded.get());
        return info;
    }

    /**
     * 内部任务对象，同时作为进度上报口
     * <p>
     * 非静态内部类：进度变化需要回调外部队列的变化通知
     */
    private final class JmTask implements ProgressReporter {

        private final String taskId = UUID.randomUUID().toString().replace("-", "");
        private final String aid;
        private final JmTaskAction action;
        private final TaskBody body;
        private final long enqueueTime = System.currentTimeMillis();
        private final AtomicInteger imageDownloaded = new AtomicInteger(0);

        private volatile String albumName;
        private volatile String coverUrl;
        private volatile JmTaskStatusEnum status;
        private volatile long startTime;
        private volatile long endTime;
        private volatile String message;
        private volatile String stage;
        private volatile Integer chapterIndex;
        private volatile Integer chapterTotal;
        private volatile String chapterTitle;
        private volatile Integer imageTotal;

        private JmTask(String aid, JmTaskAction action, String albumName, String coverUrl, TaskBody body, JmTaskStatusEnum status) {
            this.aid = aid;
            this.action = action;
            this.albumName = albumName;
            this.coverUrl = coverUrl;
            this.body = body;
            this.status = status;
        }

        @Override
        public void stage(String stage) {
            this.stage = stage;
            notifyChange();
        }

        @Override
        public void albumName(String albumName) {
            if (StringUtils.isNotBlank(albumName)) {
                this.albumName = albumName;
                notifyChange();
            }
        }

        @Override
        public void chapter(int index, int total, String title) {
            this.chapterIndex = index;
            this.chapterTotal = total;
            this.chapterTitle = title;
            notifyChange();
        }

        @Override
        public void chapterImages(int total, int downloaded) {
            this.imageTotal = total;
            this.imageDownloaded.set(Math.max(downloaded, 0));
            notifyChange();
        }

        @Override
        public void imageDownloaded() {
            this.imageDownloaded.incrementAndGet();
            notifyChange();
        }
    }
}
