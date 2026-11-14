package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.springframework.stereotype.Service;

/**
 * AI 启用范围：群白名单 + 私聊开关 + 总开关。
 */
@Service
public class AiScopeService {

    public boolean enabled(Message message) {
        if (message == null || !AiChatSettings.enabled()) {
            return false;
        }
        if (message.isGroupMsg()) {
            return message.getGroupId() != null && AiChatSettings.groupEnabled(message.getGroupId());
        }
        if (message.isPrivateMsg()) {
            return AiChatSettings.privateEnabled();
        }
        return false;
    }

    public boolean groupEnabled(long groupId) {
        return AiChatSettings.groupEnabled(groupId);
    }

    public boolean privateEnabled() {
        return AiChatSettings.privateEnabled();
    }
}
