package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

/**
 * b站视频下载任务（内存快照，仅用于管理端展示，不持久化）
 * <p>
 * 只关心任务状态与失败原因：下载过程不统计总字节数/已下载字节数，前端也不展示进度条。
 */
@Data
public class BilibiliVideoDownloadTask {

    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAIL = "fail";

    /**
     * 任务id，同一个 bvid+cid 用同一个id，保证同一时刻只有一个任务
     */
    private String taskId;

    /**
     * t_bilibili_video 的记录id
     */
    private Long videoId;

    private String bvid;

    private Long cid;

    private Long avid;

    private String title;

    /**
     * 视频封面url
     */
    private String coverUrl;

    /**
     * 本地视频文件名
     */
    private String fileName;

    /**
     * {@link #STATUS_RUNNING}
     */
    private String status;

    private String statusName;

    private Long startTime;

    private Long endTime;

    private Long costMillis;

    /**
     * 失败原因
     */
    private String message;

    public boolean isRunning() {
        return STATUS_RUNNING.equals(status);
    }

    public static String taskId(String bvid, Long cid) {
        return bvid + "_" + cid;
    }
}
