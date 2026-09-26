package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * JM任务列表快照，供管理端轮询
 */
@Data
public class JmTaskSnapshot {

    /**
     * 是否并行执行模式。并行模式下任务不进入等待队列，因此无法取消
     */
    private boolean parallel;

    private List<JmTaskInfo> runningList = Collections.emptyList();

    /**
     * 已按排队顺序排序
     */
    private List<JmTaskInfo> queuedList = Collections.emptyList();

    /**
     * 最近完成的任务，按完成时间倒序
     */
    private List<JmTaskInfo> finishedList = Collections.emptyList();

    private Counters counters = new Counters();

    /**
     * 服务端建议的轮询间隔
     */
    private long pollIntervalMillis;

    @Data
    public static class Counters {
        private int running;
        private int queued;
        private int success;
        private int fail;
        private int cancelled;
    }
}
