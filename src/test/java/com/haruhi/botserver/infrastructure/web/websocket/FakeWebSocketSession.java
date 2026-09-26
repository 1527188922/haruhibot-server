package com.haruhi.botserver.infrastructure.web.websocket;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用 WebSocket 会话，记录发送内容与关闭状态，不依赖真实容器
 */
class FakeWebSocketSession implements WebSocketSession {

    final Map<String, Object> attributes = new HashMap<>();
    final List<String> payloads = new ArrayList<>();
    volatile boolean open = true;
    volatile boolean failOnSend;
    volatile CloseStatus closeStatus;

    private final String id;

    FakeWebSocketSession(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public URI getUri() {
        return URI.create("ws://localhost/api/webui/ws");
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
        return new HttpHeaders();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public String getAcceptedProtocol() {
        return null;
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getTextMessageSizeLimit() {
        return 8192;
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getBinaryMessageSizeLimit() {
        return 8192;
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
        return List.of();
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
        if (failOnSend) {
            throw new IOException("发送失败");
        }
        payloads.add(((TextMessage) message).getPayload());
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public void close(CloseStatus status) {
        open = false;
        closeStatus = status;
    }
}
