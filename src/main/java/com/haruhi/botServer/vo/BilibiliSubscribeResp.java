package com.haruhi.botServer.vo;

import com.haruhi.botServer.entity.BilibiliSubscribeSqlite;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * bilibili订阅列表展示数据
 * 直播状态来自定时任务内存中的状态(com.haruhi.botServer.job.BilibiliLiveJob)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BilibiliSubscribeResp extends BilibiliSubscribeSqlite {

    /**
     * 是否正在直播
     */
    private Boolean living = false;

    /**
     * 本次开播时间 单位秒
     */
    private Long liveStartTime;

    /**
     * 本次开播时间 格式化后的字符串
     */
    private String liveStartTimeText;

    /**
     * 已开播时长
     */
    private String liveDuration;

    /**
     * 推送的群
     */
    private List<PushTargetInfo> groupInfos = new ArrayList<>();

    /**
     * 私聊推送的好友
     */
    private List<PushTargetInfo> friendInfos = new ArrayList<>();
}
