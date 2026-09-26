package com.haruhi.botserver.infrastructure.web.websocket;

import lombok.Getter;

import java.util.Collection;

/**
 * webui WebSocket 命令上下文
 */
@Getter
public class WebuiWsCommandContext {

    private final String sessionId;
    private final String username;
    private final WebuiWsMessage request;
    private final WebuiWsSessionRegistry sessionRegistry;

    public WebuiWsCommandContext(String sessionId, String username, WebuiWsMessage request,
                                WebuiWsSessionRegistry sessionRegistry) {
        this.sessionId = sessionId;
        this.username = username;
        this.request = request;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * 用请求的 type 作为响应 type，并带上请求id(前端据此 resolve)
     */
    public void reply(Object data) {
        reply(request.getType(), data);
    }

    /**
     * 指定响应 type 与请求id，前端既能把 Promise 配对，也能走普通监听
     */
    public void reply(String type, Object data) {
        sessionRegistry.send(sessionId, WebuiWsMessage.reply(type, request.getId(), data));
    }

    public void error(String message) {
        sessionRegistry.send(sessionId, WebuiWsMessage.error(request.getId(), message));
    }

    public void subscribe(Collection<String> topics) {
        sessionRegistry.subscribe(sessionId, topics);
    }

    public void unsubscribe(Collection<String> topics) {
        sessionRegistry.unsubscribe(sessionId, topics);
    }
}
