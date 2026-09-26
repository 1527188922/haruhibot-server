package com.haruhi.botserver.infrastructure.web.websocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * webui WebSocket 入口
 * <p>
 * 路径 {@link com.haruhi.botserver.bootstrap.SysConstants#WEBUI_WEB_SOCKET_PATH}，与机器人反向ws(/api/ws)相互独立
 */
@Slf4j
@Component
public class WebuiWsHandler extends TextWebSocketHandler {

    private final WebuiWsSessionRegistry sessionRegistry;
    private final List<WebuiWsCommandHandler> commandHandlers;
    private final List<WebuiWsTopicProvider> topicProviders;

    public WebuiWsHandler(WebuiWsSessionRegistry sessionRegistry,
                          List<WebuiWsCommandHandler> commandHandlers,
                          List<WebuiWsTopicProvider> topicProviders) {
        this.sessionRegistry = sessionRegistry;
        this.commandHandlers = commandHandlers;
        this.topicProviders = topicProviders;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = (String) session.getAttributes().get(WebuiWsHandshakeInterceptor.ATTR_USERNAME);
        if (StringUtils.isBlank(username)) {
            // 正常流程不会走到这里(握手已校验)，兜底关闭
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        sessionRegistry.register(session, username);

        Map<String, Object> data = new HashMap<>(4);
        data.put("sessionId", session.getId());
        data.put("serverTime", System.currentTimeMillis());
        sessionRegistry.send(session.getId(), WebuiWsMessage.of(WebuiWsMessage.TYPE_CONNECTED, data));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebuiWsMessage request;
        try {
            request = WebuiWsMessage.parse(message.getPayload());
        } catch (Exception e) {
            sessionRegistry.send(session.getId(), WebuiWsMessage.error(null, "消息格式错误"));
            return;
        }
        if (request == null || StringUtils.isBlank(request.getType())) {
            sessionRegistry.send(session.getId(), WebuiWsMessage.error(null, "缺少消息类型"));
            return;
        }

        String type = request.getType();
        try {
            switch (type) {
                case WebuiWsMessage.TYPE_PING -> sessionRegistry.send(session.getId(),
                        WebuiWsMessage.reply(WebuiWsMessage.TYPE_PONG, request.getId(), null));
                case WebuiWsMessage.TYPE_SUBSCRIBE -> handleSubscribe(session, request);
                case WebuiWsMessage.TYPE_UNSUBSCRIBE -> sessionRegistry.unsubscribe(
                        session.getId(), parseTopics(request));
                default -> dispatchCommand(session, request);
            }
        } catch (Exception e) {
            log.error("webui websocket 处理消息异常 sessionId:{} type:{}", session.getId(), type, e);
            sessionRegistry.send(session.getId(), WebuiWsMessage.error(request.getId(), e.getMessage()));
        }
    }

    private void dispatchCommand(WebSocketSession session, WebuiWsMessage request) {
        for (WebuiWsCommandHandler handler : commandHandlers) {
            if (!handler.supports(request.getType())) {
                continue;
            }
            String username = (String) session.getAttributes().get(WebuiWsHandshakeInterceptor.ATTR_USERNAME);
            handler.handle(new WebuiWsCommandContext(session.getId(), username, request, sessionRegistry));
            return;
        }
        sessionRegistry.send(session.getId(), WebuiWsMessage.error(request.getId(),
                "不支持的消息类型：" + request.getType()));
    }

    private void handleSubscribe(WebSocketSession session, WebuiWsMessage request) {
        List<String> topics = parseTopics(request);
        if (topics.isEmpty()) {
            sessionRegistry.send(session.getId(), WebuiWsMessage.error(request.getId(), "缺少订阅主题"));
            return;
        }
        sessionRegistry.subscribe(session.getId(), topics);
        sessionRegistry.send(session.getId(), WebuiWsMessage.reply(WebuiWsMessage.TYPE_SUBSCRIBED,
                request.getId(), Map.of("topics", sessionRegistry.topicsOf(session.getId()))));
        // 订阅成功后补发首帧，前端不用再发一次请求
        for (String topic : topics) {
            for (WebuiWsTopicProvider provider : topicProviders) {
                if (!topic.equals(provider.topic())) {
                    continue;
                }
                WebuiWsMessage initial = provider.initialMessage();
                if (initial != null) {
                    sessionRegistry.send(session.getId(), initial);
                }
            }
        }
    }

    private List<String> parseTopics(WebuiWsMessage request) {
        JSONObject data = request.dataAsObject();
        JSONArray array = data.getJSONArray("topics");
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<String> topics = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            String topic = array.getString(i);
            if (StringUtils.isNotBlank(topic)) {
                topics.add(topic);
            }
        }
        return topics;
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("webui websocket 传输异常 sessionId:{} err:{}", session.getId(), exception.getMessage());
        sessionRegistry.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.remove(session.getId());
    }
}
