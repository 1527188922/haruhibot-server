package com.haruhi.botserver.features.ai.handler;

import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.service.AiContextService;
import com.haruhi.botserver.features.ai.service.AiScopeService;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.springframework.stereotype.Component;

/**
 * 群聊环境上下文收集（W_998）。永远返回 false，不改变消息归属。
 */
@Component
public class AiContextCollectorHandler implements IAllMessageHandler {

    private final AiContextService contextService;
    private final AiScopeService scopeService;

    public AiContextCollectorHandler(AiContextService contextService, AiScopeService scopeService) {
        this.contextService = contextService;
        this.scopeService = scopeService;
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_998.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_998.getName();
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if (message != null && message.isGroupMsg() && message.getGroupId() != null
                && scopeService.groupEnabled(message.getGroupId())) {
            contextService.record(message);
        }
        return false;
    }
}
