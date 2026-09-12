package com.haruhi.botServer.vo;

import lombok.Data;

import java.util.List;

/**
 * 修改订阅的推送目标
 */
@Data
public class BilibiliSubscribeTargetReq {

    /**
     * 订阅id
     */
    private Long id;

    /**
     * 推送到哪些群，为空表示不推送群
     */
    private List<Long> groupIds;

    /**
     * 私聊推送给哪些人，为空表示不私聊推送
     */
    private List<Long> friendIds;
}
