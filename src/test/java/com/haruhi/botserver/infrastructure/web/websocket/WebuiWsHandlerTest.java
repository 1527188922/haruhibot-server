package com.haruhi.botserver.infrastructure.web.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * webui WebSocket 消息协议测试：握手后的连接登记、订阅补发首帧、ping/pong、命令分发与清理
 */
class WebuiWsHandlerTest {

    @Test
    void sendsConnectedFrameOnEstablish() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);

        assertEquals(1, fixture.registry.count());
        JSONObject connected = fixture.lastMessage();
        assertEquals("connected", connected.getString("type"));
        assertEquals("s1", connected.getJSONObject("data").getString("sessionId"));
    }

    @Test
    void repliesPongWithSameRequestId() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);

        fixture.send("{\"type\":\"ping\",\"id\":\"c-1\"}");

        JSONObject pong = fixture.lastMessage();
        assertEquals("pong", pong.getString("type"));
        assertEquals("c-1", pong.getString("id"));
    }

    @Test
    void subscribesAndPushesInitialFrame() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);

        fixture.send("{\"type\":\"subscribe\",\"id\":\"c-2\",\"data\":{\"topics\":[\"demo.topic\"]}}");

        assertEquals(1, fixture.registry.subscriberCount("demo.topic"));
        // 建连补 connected，订阅回 subscribed，紧接着补发首帧
        assertEquals(List.of("connected", "subscribed", "demo.topic.snapshot"), fixture.messageTypes());
        assertEquals(1, fixture.lastMessage().getJSONObject("data").getIntValue("version"));
    }

    @Test
    void unsubscribeStopsTopic() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);
        fixture.send("{\"type\":\"subscribe\",\"data\":{\"topics\":[\"demo.topic\"]}}");

        fixture.send("{\"type\":\"unsubscribe\",\"data\":{\"topics\":[\"demo.topic\"]}}");

        assertEquals(0, fixture.registry.subscriberCount("demo.topic"));
    }

    @Test
    void dispatchesToCommandHandler() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);

        fixture.send("{\"type\":\"demo.echo\",\"id\":\"c-3\",\"data\":{\"value\":\"hi\"}}");

        JSONObject reply = fixture.lastMessage();
        assertEquals("demo.echo.result", reply.getString("type"));
        assertEquals("c-3", reply.getString("id"));
        assertEquals("hi", reply.getJSONObject("data").getString("echo"));
    }

    @Test
    void reportsUnknownTypeAndBrokenPayload() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);

        fixture.send("{\"type\":\"nope\"}");
        assertEquals("error", fixture.lastMessage().getString("type"));

        fixture.send("not-a-json");
        assertEquals("error", fixture.lastMessage().getString("type"));

        fixture.send("{\"id\":\"c-4\"}");
        assertEquals("error", fixture.lastMessage().getString("type"));
    }

    @Test
    void removesSessionOnClose() throws Exception {
        Fixture fixture = new Fixture();
        fixture.handler.afterConnectionEstablished(fixture.session);

        fixture.handler.afterConnectionClosed(fixture.session, null);
        assertEquals(0, fixture.registry.count());

        // 重复关闭应当幂等
        fixture.handler.afterConnectionClosed(fixture.session, null);
        assertEquals(0, fixture.registry.count());
    }

    private static final class Fixture {

        private final WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        private final FakeWebSocketSession session = new FakeWebSocketSession("s1");
        private final WebuiWsHandler handler;

        private Fixture() {
            session.attributes.put(WebuiWsHandshakeInterceptor.ATTR_USERNAME, "admin");
            WebuiWsCommandHandler echoHandler = new WebuiWsCommandHandler() {
                @Override
                public boolean supports(String type) {
                    return "demo.echo".equals(type);
                }

                @Override
                public void handle(WebuiWsCommandContext context) {
                    context.reply("demo.echo.result", Map.of("echo", context.getRequest().dataAsObject().getString("value")));
                }
            };
            WebuiWsTopicProvider topicProvider = new WebuiWsTopicProvider() {
                @Override
                public String topic() {
                    return "demo.topic";
                }

                @Override
                public WebuiWsMessage initialMessage() {
                    return WebuiWsMessage.of("demo.topic.snapshot", Map.of("version", 1));
                }
            };
            handler = new WebuiWsHandler(registry, List.of(echoHandler), List.of(topicProvider));
        }

        private void send(String payload) throws Exception {
            handler.handleTextMessage(session, new TextMessage(payload));
        }

        private JSONObject lastMessage() {
            assertTrue(!session.payloads.isEmpty(), "没有收到任何消息");
            return JSON.parseObject(session.payloads.getLast());
        }

        private List<String> messageTypes() {
            return session.payloads.stream()
                    .map(payload -> JSON.parseObject(payload).getString("type"))
                    .toList();
        }
    }
}
