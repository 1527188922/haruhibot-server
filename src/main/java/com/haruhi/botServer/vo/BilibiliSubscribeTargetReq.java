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
     * 开播消息需要@全体成员的群，传null表示不修改(仅保留仍在新群列表中的群)
     */
    private List<Long> atAllGroupIds;

    /**
     * 私聊推送给哪些人，为空表示不私聊推送
     */
    private List<Long> friendIds;
}
