package com.haruhi.botserver.features.bilibili.model;

import lombok.Data;

/**
 * bilibili视频管理页卡片数据
 */
@Data
public class BilibiliVideoResp {

    private Long id;
    private String bvid;
    private Long avid;
    private Long cid;
    private String title;
    private String tag;
    private String desc;
    private String pic;
    private Long duration;
    private Long ownerMid;
    private String ownerName;
    private String ownerFace;
    private Long pubdate;
    private Long ctime;
    private String createTime;
    private String updateTime;

    /**
     * 是否已经调用过 getPlayUrlInfo（play_url_raw 是否有值）
     */
    private Boolean hasPlayUrl;

    /**
     * 本地视频文件名，如 BV1ZsaB6HE2U_42369091370.mp4
     */
    private String videoFileName;

    /**
     * 服务器本地视频的访问路径(相对路径，如 /video/bilibili/BV1ZsaB6HE2U_42369091370.mp4)，
     * 已下载时前端用它直接播放
     */
    private String videoPath;

    /**
     * 本地是否已存在该视频文件
     */
    private Boolean downloaded;

    /**
     * 是否正在下载
     */
    private Boolean downloading;

    /**
     * 最近一次下载结果：success/fail，没有下载记录时为null
     */
    private String downloadState;

    /**
     * 下载失败原因
     */
    private String downloadMessage;

    /**
     * 下载进度百分比，总大小未知时为null
     */
    private Integer downloadPercent;

    /**
     * 下载速度(字节/秒)
     */
    private Long downloadSpeed;
}
