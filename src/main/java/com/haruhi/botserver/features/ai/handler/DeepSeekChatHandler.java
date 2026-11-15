package com.haruhi.botserver.features.ai.handler;

import com.haruhi.botserver.bot.handler.DebounceParam;
import com.haruhi.botserver.bot.handler.HandlerWeightEnum;
import com.haruhi.botserver.bot.handler.IAllMessageHandler;
import com.haruhi.botserver.bot.handler.IDebounceable;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiTriggerDecision;
import com.haruhi.botserver.features.ai.model.AiTurn;
import com.haruhi.botserver.features.ai.service.AiChatService;
import com.haruhi.botserver.features.ai.service.AiScopeService;
import com.haruhi.botserver.features.ai.service.AiSessionService;
import com.haruhi.botserver.features.ai.service.AiTriggerPolicy;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.AiMessageExtractor;
import com.haruhi.botserver.integration.onebot.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 兜底聊天入口（W_170）。只负责判定/入队，实际生成在会话 worker。
 * 事件去重由 MessageDispatcher 统一入口通过 IDebounceable 完成，本类不再自行判定。
 */
@Component
@Slf4j
public class DeepSeekChatHandler implements IAllMessageHandler, IDebounceable<Message> {

    private final AiScopeService scopeService;
    private final AiTriggerPolicy triggerPolicy;
    private final AiChatService chatService;
    private final AiSessionService sessionService;

    public DeepSeekChatHandler(AiScopeService scopeService,
                               AiTriggerPolicy triggerPolicy,
                               AiChatService chatService,
                               AiSessionService sessionService) {
        this.scopeService = scopeService;
        this.triggerPolicy = triggerPolicy;
        this.chatService = chatService;
        this.sessionService = sessionService;
    }

    @Override
    public int weight() {
        return HandlerWeightEnum.W_170.getWeight();
    }

    @Override
    public String funName() {
        return HandlerWeightEnum.W_170.getName();
    }

    @Override
    public DebounceParam debounceParam(Message message) {
        if (message == null || message.isSelfMsg() || !scopeService.enabled(message)) {
            return null;
        }
        String messageId = message.getMessageId();
        if (StringUtils.isBlank(messageId)) {
            // 无消息id时放弃去重，避免哈希兜底误判导致静默吞消息
            return null;
        }
        AiSessionKey key = AiSessionKey.of(message);
        return new DebounceParam("dschat:dup:" + key.cacheKey() + ":" + messageId,
                AiChatSettings.dedupeTtlMillis());
    }

    @Override
    public boolean onMessage(Bot bot, Message message) {
        if (!scopeService.enabled(message) || message.isSelfMsg()) {
            return false;
        }
        AiIncoming incoming = AiMessageExtractor.extract(message);
        if (incoming.getUserId() > 0 && AiChatSettings.ignoreUserIds().contains(incoming.getUserId())) {
            return false;
        }
        AiSessionKey key = incoming.getKey();
        if (sessionService.isCachedPaused(key)) {
            return true;
        }
        AiTriggerDecision decision = triggerPolicy.decide(bot, message, incoming);

        if (incoming.isAtOnly()) {
            chatService.enqueue(key, turn(bot, incoming, decision, true));
            return true;
        }
        if (!decision.triggered()) {
            return false;
        }
        chatService.enqueue(key, turn(bot, incoming, decision, decision.direct()));
        return true;
    }

    private AiTurn turn(Bot bot, AiIncoming incoming, AiTriggerDecision decision, boolean direct) {
        long now = System.currentTimeMillis();
        AiTurn turn = new AiTurn();
        turn.setBot(bot);
        turn.setIncoming(incoming);
        turn.setDirect(direct);
        turn.setNotBefore(now + Math.max(0, decision.waitMillis()));
        long cap = decision.atOnly() ? AiChatSettings.mentionCapMillis() : AiChatSettings.debounceMaxWaitMillis();
        turn.setDeadline(now + cap);
        return turn;
    }
}
