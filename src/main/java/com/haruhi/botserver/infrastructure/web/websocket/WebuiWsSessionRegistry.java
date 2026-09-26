package com.haruhi.botserver.infrastructure.web.websocket;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * webui WebSocket 会话注册表
 * <p>
 * 职责：连接登记、按主题广播、按用户推送、登出踢线、失效会话清理。
 * <p>
 * 所有会话都包一层 {@link ConcurrentWebSocketSessionDecorator}：
 * WebSocketSession#sendMessage 不是线程安全的，推送线程和请求线程同时发同一个会话会抛
 * IllegalStateException: The remote endpoint was in state [TEXT_PARTIAL_WRITING]
 */
@Slf4j
@Component
public class WebuiWsSessionRegistry {

    private static final int SEND_TIME_LIMIT_MILLIS = 5000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final Map<String, WebuiSession> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, String username) {
        WebSocketSession decorated = new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MILLIS, BUFFER_SIZE_LIMIT_BYTES);
        sessions.put(session.getId(), new WebuiSession(session.getId(), username, decorated));
        log.info("webui websocket 连接建立 sessionId:{} user:{} 当前连接数:{}",
                session.getId(), username, sessions.size());
    }

    public void remove(String sessionId) {
        WebuiSession removed = sessions.remove(sessionId);
        if (removed != null) {
            log.info("webui websocket 连接关闭 sessionId:{} user:{} 当前连接数:{}",
                    sessionId, removed.username, sessions.size());
        }
    }

    public int count() {
        return sessions.size();
    }

    public int subscriberCount(String topic) {
        int count = 0;
        for (WebuiSession session : sessions.values()) {
            if (session.topics.contains(topic)) {
                count++;
            }
        }
        return count;
    }

    public Set<String> topicsOf(String sessionId) {
        WebuiSession session = sessions.get(sessionId);
        return session == null ? Collections.emptySet() : Set.copyOf(session.topics);
    }

    public void subscribe(String sessionId, Collection<String> topics) {
        WebuiSession session = sessions.get(sessionId);
        if (session == null || topics == null) {
            return;
        }
        topics.stream().filter(t -> t != null && !t.isBlank()).forEach(session.topics::add);
    }

    public void unsubscribe(String sessionId, Collection<String> topics) {
        WebuiSession session = sessions.get(sessionId);
        if (session == null || topics == null) {
            return;
        }
        topics.forEach(session.topics::remove);
    }

    /**
     * 发送给单个会话，失败即摘除会话
     */
    public boolean send(String sessionId, WebuiWsMessage message) {
        WebuiSession session = sessions.get(sessionId);
        if (session == null || message == null) {
            return false;
        }
        return doSend(session, message);
    }

    public void broadcastToTopic(String topic, WebuiWsMessage message) {
        broadcast(topic, null, message);
    }

    public void publishToUser(String username, WebuiWsMessage message) {
        broadcast(null, username, message);
    }

    private void broadcast(String topic, String username, WebuiWsMessage message) {
        if (message == null || sessions.isEmpty()) {
            return;
        }
        for (WebuiSession session : sessions.values()) {
            if (topic != null && !session.topics.contains(topic)) {
                continue;
            }
            if (username != null && !username.equals(session.username)) {
                continue;
            }
            doSend(session, message);
        }
    }

    private boolean doSend(WebuiSession session, WebuiWsMessage message) {
        try {
            if (!session.session.isOpen()) {
                close(session.sessionId, WebuiWsCloseCodes.SERVER_ERROR, "连接已关闭");
                return false;
            }
            session.session.sendMessage(new TextMessage(message.toJson()));
            return true;
        } catch (Exception e) {
            log.warn("webui websocket 发送失败，移除会话 sessionId:{} err:{}", session.sessionId, e.getMessage());
            close(session.sessionId, WebuiWsCloseCodes.SERVER_ERROR, "发送失败");
            return false;
        }
    }

    public void close(String sessionId, int code, String reason) {
        WebuiSession session = sessions.remove(sessionId);
        if (session == null) {
            return;
        }
        try {
            session.session.close(new CloseStatus(code, reason));
        } catch (Exception e) {
            log.warn("webui websocket 关闭会话异常 sessionId:{} err:{}", sessionId, e.getMessage());
        }
    }

    /**
     * 踢掉某个用户的全部连接，用于登出
     */
    public void closeByUsername(String username, int code, String reason) {
        if (username == null) {
            return;
        }
        List<String> sessionIds = sessions.values().stream()
                .filter(e -> username.equals(e.username))
                .map(e -> e.sessionId)
                .toList();
        sessionIds.forEach(sessionId -> close(sessionId, code, reason));
    }

    public void closeAll(int code, String reason) {
        List<String> sessionIds = List.copyOf(sessions.keySet());
        sessionIds.forEach(sessionId -> close(sessionId, code, reason));
    }

    @PreDestroy
    public void destroy() {
        closeAll(WebuiWsCloseCodes.GOING_AWAY, "服务停止");
    }

    private static final class WebuiSession {
        private final String sessionId;
        private final String username;
        private final WebSocketSession session;
        private final Set<String> topics = ConcurrentHashMap.newKeySet();

        private WebuiSession(String sessionId, String username, WebSocketSession session) {
            this.sessionId = sessionId;
            this.username = username;
            this.session = session;
        }
    }
}
