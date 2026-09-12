package com.haruhi.botServer.vo;

import lombok.Data;

/**
 * bilibili直播推送定时任务信息
 */
@Data
public class BilibiliJobInfoResp {

    /**
     * 配置文件中是否开启 job.bilibiliLive.enable = 1
     */
    private Boolean enable = false;

    /**
     * 定时任务的bean是否已创建(配置开启时为true)
     */
    private Boolean registered = false;

    /**
     * cron表达式
     */
    private String cron;
}
