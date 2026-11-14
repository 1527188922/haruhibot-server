package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiAmbientMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 分条/延迟/引用/@ 发送，并把 assistant 气泡逐条落库。
 */
@Service
@Slf4j
public class AiSendService {

    private final AiSessionService sessionService;
    private final AiRateLimiter rateLimiter;

    public AiSendService(AiSessionService sessionService, AiRateLimiter rateLimiter) {
        this.sessionService = sessionService;
        this.rateLimiter = rateLimiter;
    }

    public void send(Bot bot, AiSessionState state, AiSessionKey key, List<AiReplyMessage> messages,
                     AiIncoming current, List<AiAmbientMessage> ambient) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        if (current != null && !rateLimiter.allow(key, current)) {
            log.debug("AI发送被限流: {}", key.cacheKey());
            return;
        }
        long generation = state.generation();
        Set<String> replyIds = new HashSet<>();
        Set<String> atQqs = new HashSet<>();
        if (current != null) {
            if (StringUtils.isNotBlank(current.getMessageId())) {
                replyIds.add(current.getMessageId());
            }
            if (current.getUserId() > 0) {
                atQqs.add(String.valueOf(current.getUserId()));
            }
        }
        if (ambient != null) {
            for (AiAmbientMessage item : ambient) {
                if (StringUtils.isNotBlank(item.getMessageId())) {
                    replyIds.add(item.getMessageId());
                }
                if (item.getSpeakerId() > 0) {
                    atQqs.add(String.valueOf(item.getSpeakerId()));
                }
            }
        }

        int count = Math.min(messages.size(), AiChatSettings.messageMaxCount());
        long totalDelay = 0;
        for (int i = 0; i < count; i++) {
            AiReplyMessage message = messages.get(i);
            int delay = i == 0 ? AiChatSettings.messageFirstDelayMillis() : message.getDelayMs();
            delay = Math.max(0, Math.min(delay, AiChatSettings.messageMaxDelayMillis()));
            totalDelay += delay;
        }        long cap = AiChatSettings.messageTotalTimeoutMillis();
        double scale = totalDelay > cap ? (double) cap / totalDelay : 1.0;

        for (int i = 0; i < count; i++) {
            AiReplyMessage message = messages.get(i);
            if (!bot.isOnline() || state.generation() != generation) {
                log.debug("机器人离线或会话已重置，中止剩余AI消息");
                return;
            }
            int delay = i == 0 ? AiChatSettings.messageFirstDelayMillis() : message.getDelayMs();
            delay = Math.max(0, Math.min(delay, AiChatSettings.messageMaxDelayMillis()));
            delay = (int) (delay * scale);
            if (delay > 0) {
                try {
                    Thread.sleep(delay + ThreadLocalRandom.current().nextLong(0, 200));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (!bot.isOnline() || state.generation() != generation) {
                log.debug("机器人离线或会话已重置，中止剩余AI消息");
                return;
            }

            List<MessageHolder> holders = new ArrayList<>();
            if (StringUtils.isNotBlank(message.getReplyTo()) && replyIds.contains(message.getReplyTo())) {
                holders.addAll(MessageHolder.instanceReply(message.getReplyTo()));
            }
            if (StringUtils.isNotBlank(message.getAt())
                    && atQqs.contains(message.getAt())
                    && !message.getAt().equals(String.valueOf(key.selfId()))) {
                holders.addAll(MessageHolder.instanceAt(message.getAt()));
            }
            holders.addAll(MessageHolder.instanceText(message.getText()));

            try {
                if (key.group()) {
                    bot.sendGroupMessage(key.targetId(), holders);
                } else {
                    bot.sendPrivateMessage(key.targetId(), holders);
                }
                sessionService.appendAssistant(state, message.getText());
            } catch (Exception e) {
                log.error("发送AI消息失败: session={}, text={}", key.cacheKey(), message.getText(), e);
                return;
            }
        }
    }
}
