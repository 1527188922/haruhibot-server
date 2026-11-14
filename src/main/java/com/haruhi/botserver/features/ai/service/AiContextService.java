package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.features.ai.model.AiAmbientMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.AiMessageExtractor;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群聊环境上下文：只放内存，不落库；不进模型历史的“背景音”。
 */
@Service
public class AiContextService {

    private static final long EXPIRE_MILLIS = 30 * 60 * 1000L;

    private final Map<String, GroupBuffer> buffers = new ConcurrentHashMap<>();

    public void record(Message message) {
        if (message == null || !message.isGroupMsg() || message.isSelfMsg()) {
            return;
        }
        if (message.getGroupId() == null || !AiChatSettings.groupEnabled(message.getGroupId())) {
            return;
        }
        AiIncoming incoming = AiMessageExtractor.extract(message);
        if (incoming.getText() == null || incoming.getText().isBlank()) {
            return;
        }
        String key = AiSessionKey.of(message).cacheKey();
        GroupBuffer buffer = buffers.computeIfAbsent(key, ignored -> new GroupBuffer());
        synchronized (buffer) {
            buffer.messages.addLast(new AiAmbientMessage(
                    incoming.getMessageId(),
                    incoming.getUserId(),
                    incoming.getUserName(),
                    incoming.getText(),
                    incoming.getTimestamp()));
            int max = Math.max(4, AiChatSettings.ambientSize() * 2);
            while (buffer.messages.size() > max) {
                buffer.messages.removeFirst();
            }
            buffer.lastAccess = System.currentTimeMillis();
        }
    }

    public List<AiAmbientMessage> recent(AiSessionKey key, int limit, java.util.Collection<String> excludeMessageIds) {
        if (key == null || !key.group()) {
            return List.of();
        }
        GroupBuffer buffer = buffers.get(key.cacheKey());
        if (buffer == null) {
            return List.of();
        }
        synchronized (buffer) {
            if (System.currentTimeMillis() - buffer.lastAccess > EXPIRE_MILLIS) {
                buffers.remove(key.cacheKey());
                return List.of();
            }
            int size = Math.min(Math.max(0, limit), buffer.messages.size());
            List<AiAmbientMessage> result = new ArrayList<>(size);
            int skip = buffer.messages.size() - size;
            int index = 0;
            for (AiAmbientMessage message : buffer.messages) {
                if (index++ < skip) {
                    continue;
                }
                if (excludeMessageIds != null && excludeMessageIds.contains(message.getMessageId())) {
                    continue;
                }
                result.add(message);
            }
            return result;
        }
    }

    private static class GroupBuffer {
        private final Deque<AiAmbientMessage> messages = new ArrayDeque<>();
        private volatile long lastAccess = System.currentTimeMillis();
    }
}
