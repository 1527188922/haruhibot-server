package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通知事件防抖参数
 * key 相同的通知在窗口内只处理第一条，窗口从第一条通知开始计算
 */
@Getter
@RequiredArgsConstructor
public class DebounceParam {

    /**
     * 业务唯一键，仅在同一个处理类内生效
     */
    private final String key;
    /**
     * 防抖窗口（毫秒）
     */
    private final long windowMillis;

    /**
     * 使用默认 key 构建：selfId:noticeType:subType:groupId:userId:targetId
     * key 包含 selfId，避免多个机器人实例收到同一事件时互相压制
     */
    public static DebounceParam of(NoticeEvent event, long windowMillis) {
        String key = String.join(":",
                String.valueOf(event.getSelfId()),
                String.valueOf(event.getNoticeType()),
                String.valueOf(event.getSubType()),
                String.valueOf(event.getGroupId()),
                String.valueOf(event.getUserId()),
                String.valueOf(event.getTargetId()));
        return new DebounceParam(key, windowMillis);
    }
}
