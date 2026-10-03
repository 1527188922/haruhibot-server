package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

import java.util.List;

/**
 * b站视频下载任务快照，通过webui WebSocket推送给前端
 */
@Data
public class BilibiliVideoDownloadSnapshot {

    private Counters counters;

    /**
     * 正在下载的任务，按开始时间正序
     */
    private List<BilibiliVideoDownloadTask> runningList;

    /**
     * 最近完成的任务（成功/失败），按结束时间倒序
     */
    private List<BilibiliVideoDownloadTask> finishedList;

    @Data
    public static class Counters {
        private int running;
        private int success;
        private int fail;
    }
}
