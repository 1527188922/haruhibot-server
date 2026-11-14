package com.haruhi.botserver.features.ai.handler;

import com.haruhi.botserver.bot.dispatch.DebounceSupport;
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
 */
@Component
@Slf4j
public class DeepSeekChatHandler implements IAllMessageHandler, IDebounceable<Message> {

    private final AiScopeService scopeService;
    private final AiTriggerPolicy triggerPolicy;
    private final AiChatService chatService;
    private final AiSessionService sessionService;
    private final DebounceSupport debounceSupport;

    public DeepSeekChatHandler(AiScopeService scopeService,
                               AiTriggerPolicy triggerPolicy,
                               AiChatService chatService,
                               AiSessionService sessionService,
                               DebounceSupport debounceSupport) {
        this.scopeService = scopeService;
        this.triggerPolicy = triggerPolicy;
        this.chatService = chatService;
        this.sessionService = sessionService;
        this.debounceSupport = debounceSupport;
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
        if (message == null || !scopeService.enabled(message)) {
            return null;
        }
        AiSessionKey key = AiSessionKey.of(message);
        String id = StringUtils.defaultIfBlank(message.getMessageId(), fallbackHash(message));
        return new DebounceParam("dschat:dup:" + key.cacheKey() + ":" + id,
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
            if (debounceSupport.isDebounced(this, message)) {
                return true;
            }
            chatService.enqueue(key, turn(bot, incoming, decision, true));
            return true;
        }
        if (!decision.triggered()) {
            return false;
        }
        if (debounceSupport.isDebounced(this, message)) {
            return true;
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

    private String fallbackHash(Message message) {
        return Integer.toHexString((String.valueOf(message.getSelfId()) + message.getUserId()
                + message.getGroupId() + message.getTime() + message.getRawMessage()).hashCode());
    }
}
