package com.haruhi.botserver.features.jmcomic.model;

import lombok.Data;

/**
 * JM任务的内存快照，仅用于管理端展示
 * <p>
 * 所有字段都来自内存中的队列/运行记录，不涉及数据库
 */
@Data
public class JmTaskInfo {

    private String taskId;
    /**
     * JM ID
     */
    private String aid;
    /**
     * 本子名称，可能为空(提交时本地还没有记录)，前端可回退显示 JM{aid}
     */
    private String albumName;
    /**
     * JM远程封面地址
     */
    private String coverUrl;
    /**
     * 动作code，见 {@link JmTaskAction#getCode()}
     */
    private String action;
    /**
     * 动作文案，如"下载漫画"
     */
    private String actionName;
    /**
     * 状态枚举名
     */
    private String status;
    private String statusName;
    /**
     * 是否支持取消(仅排队中支持)
     */
    private boolean cancellable;
    /**
     * 排队序号，从1开始；非排队状态为null
     */
    private Integer queuePosition;
    private long enqueueTime;
    /**
     * 开始执行时间
     */
    private Long startTime;
    private Long endTime;
    /**
     * 排队等待时长
     */
    private Long waitMillis;
    /**
     * 执行耗时
     */
    private Long costMillis;
    /**
     * 结果消息或失败原因
     */
    private String message;

    /**
     * 当前阶段，如"请求本子详情"、"下载漫画图片"、"打包zip"、"生成pdf"
     */
    private String stage;
    /**
     * 当前正在下载第几话，从1开始；非下载阶段为null
     */
    private Integer chapterIndex;
    /**
     * 本子总话数
     */
    private Integer chapterTotal;
    /**
     * 当前话标题，如"第3话"
     */
    private String chapterTitle;
    /**
     * 当前话图片总数，进度条最大值
     */
    private Integer imageTotal;
    /**
     * 当前话已下载图片数，进度条当前值
     */
    private Integer imageDownloaded;
}
