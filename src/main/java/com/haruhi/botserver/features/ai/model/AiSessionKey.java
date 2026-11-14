package com.haruhi.botserver.features.ai.model;

import com.haruhi.botserver.integration.onebot.model.Message;

/**
 * AI 会话唯一键：机器人 + 场景 + 目标（群号或用户 QQ）。
 * <p>
 * 不要用字符串直接拼接 selfId + groupId，避免 123+456 与 12+3456 这类碰撞。
 */
public record AiSessionKey(long selfId, AiScene scene, long targetId) {

    public static AiSessionKey of(Message message) {
        long selfId = message.getSelfId() == null ? 0L : message.getSelfId();
        if (message.isGroupMsg()) {
            long groupId = message.getGroupId() == null ? 0L : message.getGroupId();
            return new AiSessionKey(selfId, AiScene.GROUP, groupId);
        }
        long userId = message.getUserId() == null ? 0L : message.getUserId();
        return new AiSessionKey(selfId, AiScene.PRIVATE, userId);
    }

    public boolean group() {
        return scene == AiScene.GROUP;
    }

    public String cacheKey() {
        return selfId + ":" + scene.getCode() + ":" + targetId;
    }
}
