package com.haruhi.botserver.infrastructure.web.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * webui WebSocket 会话注册表测试，不依赖Spring与网络
 */
class WebuiWsSessionRegistryTest {

    @Test
    void broadcastsOnlyToSubscribedSessions() {
        WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        FakeWebSocketSession first = new FakeWebSocketSession("s1");
        FakeWebSocketSession second = new FakeWebSocketSession("s2");
        registry.register(first, "u1");
        registry.register(second, "u2");

        assertEquals(2, registry.count());
        registry.subscribe("s1", List.of("jm.task"));
        assertEquals(1, registry.subscriberCount("jm.task"));
        assertEquals(0, registry.subscriberCount("other"));

        registry.broadcastToTopic("jm.task", WebuiWsMessage.of("jm.task.snapshot", Map.of("running", 1)));

        assertEquals(1, first.payloads.size());
        assertEquals(0, second.payloads.size());
        JSONObject json = JSON.parseObject(first.payloads.getFirst());
        assertEquals("jm.task.snapshot", json.getString("type"));
        assertEquals(1, json.getJSONObject("data").getIntValue("running"));
    }

    @Test
    void unsubscribeStopsBroadcast() {
        WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        FakeWebSocketSession session = new FakeWebSocketSession("s1");
        registry.register(session, "u1");
        registry.subscribe("s1", List.of("jm.task"));
        registry.unsubscribe("s1", List.of("jm.task"));

        assertEquals(0, registry.subscriberCount("jm.task"));
        registry.broadcastToTopic("jm.task", WebuiWsMessage.of("jm.task.snapshot", null));
        assertEquals(0, session.payloads.size());
    }

    @Test
    void removesSessionWhenSendFails() {
        WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        FakeWebSocketSession session = new FakeWebSocketSession("s1");
        registry.register(session, "u1");
        session.failOnSend = true;

        assertFalse(registry.send("s1", WebuiWsMessage.of("ping", null)));
        assertEquals(0, registry.count(), "发送失败的会话应被摘除");
        assertEquals(WebuiWsCloseCodes.SERVER_ERROR, session.closeStatus.getCode());
    }

    @Test
    void skipsClosedSession() {
        WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        FakeWebSocketSession session = new FakeWebSocketSession("s1");
        registry.register(session, "u1");
        session.open = false;

        assertFalse(registry.send("s1", WebuiWsMessage.of("ping", null)));
        assertEquals(0, registry.count());
    }

    @Test
    void closesAllSessionsOfUser() {
        WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        FakeWebSocketSession first = new FakeWebSocketSession("s1");
        FakeWebSocketSession second = new FakeWebSocketSession("s2");
        FakeWebSocketSession other = new FakeWebSocketSession("s3");
        registry.register(first, "u1");
        registry.register(second, "u1");
        registry.register(other, "u2");

        registry.closeByUsername("u1", WebuiWsCloseCodes.LOGOUT, "logout");

        assertEquals(1, registry.count());
        assertEquals(WebuiWsCloseCodes.LOGOUT, first.closeStatus.getCode());
        assertEquals(WebuiWsCloseCodes.LOGOUT, second.closeStatus.getCode());
        assertNull(other.closeStatus, "其他用户的连接不应被关闭");
    }

    @Test
    void publishToUserHitsOnlyThatUser() {
        WebuiWsSessionRegistry registry = new WebuiWsSessionRegistry();
        FakeWebSocketSession first = new FakeWebSocketSession("s1");
        FakeWebSocketSession other = new FakeWebSocketSession("s2");
        registry.register(first, "u1");
        registry.register(other, "u2");

        registry.publishToUser("u1", WebuiWsMessage.of("sys.notice", Map.of("text", "hi")));

        assertEquals(1, first.payloads.size());
        assertEquals(0, other.payloads.size());
        assertTrue(first.payloads.getFirst().contains("sys.notice"));
    }
}
