package com.haruhi.botserver.features.ai.support.protocol;

import com.haruhi.botserver.features.ai.model.AiReply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReplyParserTest {

    @Test
    void parsesSayWithMultipleMessages() {
        AiReply reply = AiReplyParser.parse("""
                {"type":"say","messages":[{"text":"啊？","delay_ms":0},{"text":"你又开始了","delay_ms":900}]}
                """);
        assertTrue(reply.isSay());
        assertEquals(2, reply.getMessages().size());
        assertEquals("啊？", reply.getMessages().get(0).getText());
        assertEquals(900, reply.getMessages().get(1).getDelayMs());
    }

    @Test
    void stripsCodeFenceAndTrailingText() {
        AiReply reply = AiReplyParser.parse("""
                ```json
                {"type":"say","messages":[{"text":"在","delay_ms":0}]}
                ```
                """);
        assertTrue(reply.isSay());
        assertEquals("在", reply.getMessages().get(0).getText());
    }

    @Test
    void parsesCall() {
        AiReply reply = AiReplyParser.parse("""
                {"type":"call","call":{"name":"group_history","args":{"limit":10}}}
                """);
        assertTrue(reply.isCall());
        assertEquals("group_history", reply.getCall().getName());
        assertEquals(10, reply.getCall().getArgs().getIntValue("limit"));
    }

    @Test
    void parsesSilent() {
        AiReply reply = AiReplyParser.parse("{\"type\":\"silent\"}");
        assertTrue(reply.isSilent());
    }

    @Test
    void acceptsStringMessageArray() {
        AiReply reply = AiReplyParser.parse("{\"type\":\"say\",\"messages\":[\"草\"]}");
        assertTrue(reply.isSay());
        assertEquals("草", reply.getMessages().get(0).getText());
    }

    @Test
    void rejectsEmptyContent() {
        AiReply reply = AiReplyParser.parse("   ");
        assertTrue(reply.isInvalid());
        assertEquals("empty_content", reply.getError());
    }

    @Test
    void rejectsInvalidJson() {
        AiReply reply = AiReplyParser.parse("not json");
        assertTrue(reply.isInvalid());
        assertEquals("no_json_object", reply.getError());
    }

    @Test
    void parsesMemoryAndForget() {
        AiReply reply = AiReplyParser.parse("""
                {"type":"say","messages":[{"text":"好"}],
                 "memory":{"称呼":"小明","note":"喜欢咖啡"},
                 "forget":["旧称呼"]}
                """);
        assertTrue(reply.isSay());
        assertEquals("小明", reply.getMemory().get("称呼"));
        assertEquals("喜欢咖啡", reply.getMemory().get("note"));
        assertEquals("旧称呼", reply.getForgetMemory().get(0));
    }

    @Test
    void rejectsUnknownType() {
        AiReply reply = AiReplyParser.parse("{\"type\":\"dance\"}");
        assertTrue(reply.isInvalid());
        assertEquals("unknown_type", reply.getError());
    }
}
