package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的会话/用户/群级限流。只做内存计数，重启重置。
 */
@Service
public class AiRateLimiter {

    private final Map<String, Long> sessionLastReply = new ConcurrentHashMap<>();
    private final Map<String, Long> userLastTrigger = new ConcurrentHashMap<>();
    private final Map<String, Window> groupWindow = new ConcurrentHashMap<>();

    public boolean allow(AiSessionKey key, AiIncoming incoming) {
        long now = System.currentTimeMillis();
        String sessionKey = key.cacheKey();
        if (key.group()) {
            long min = AiChatSettings.groupMinReplyIntervalMillis();
            Long last = sessionLastReply.get(sessionKey);
            if (last != null && now - last < min) {
                return false;
            }
            long cooldown = AiChatSettings.groupUserCooldownMillis();
            String userKey = sessionKey + ":" + incoming.getUserId();
            Long userLast = userLastTrigger.get(userKey);
            if (userLast != null && now - userLast < cooldown) {
                return false;
            }
            Window window = groupWindow.computeIfAbsent(sessionKey, ignored -> new Window());
            synchronized (window) {
                if (window.minute != now / 60000) {
                    window.minute = now / 60000;
                    window.count = 0;
                }
                if (window.count >= AiChatSettings.groupMaxRepliesPerMinute()) {
                    return false;
                }
            }
            sessionLastReply.put(sessionKey, now);
            userLastTrigger.put(userKey, now);
            synchronized (window) {
                window.count++;
            }
            return true;
        }
        long min = AiChatSettings.privateMinReplyIntervalMillis();
        Long last = sessionLastReply.get(sessionKey);
        if (last != null && now - last < min) {
            return false;
        }
        sessionLastReply.put(sessionKey, now);
        return true;
    }

    private static class Window {
        private long minute;
        private int count;
    }
}
