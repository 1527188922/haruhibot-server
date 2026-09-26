package com.haruhi.botserver.features.jmcomic.service;

/**
 * 当前线程正在执行的JM任务上下文
 * <p>
 * 只在任务执行线程上有效：串行模式是队列工作线程，并行模式是发起调用的线程。
 * 供下载流程上报"第几话 / 图片进度"，不在任何JM任务中时返回 null。
 */
public final class JmTaskContext {

    private static final ThreadLocal<JmTaskQueue.ProgressReporter> CURRENT = new ThreadLocal<>();

    private JmTaskContext() {
    }

    static void set(JmTaskQueue.ProgressReporter reporter) {
        CURRENT.set(reporter);
    }

    static void clear() {
        CURRENT.remove();
    }

    public static JmTaskQueue.ProgressReporter current() {
        return CURRENT.get();
    }
}
