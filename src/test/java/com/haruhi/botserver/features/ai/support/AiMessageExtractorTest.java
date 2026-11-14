package com.haruhi.botserver.features.ai.support;

import com.alibaba.fastjson.JSON;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMessageExtractorTest {

    @Test
    void extractsAtAndText() {
        Message message = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m1","time":1700000000,
                 "sender":{"nickname":"张三"},
                 "message":[{"type":"at","data":{"qq":"2"}},{"type":"text","data":{"text":" 你好"}}]}
                """, Message.class);
        AiIncoming incoming = AiMessageExtractor.extract(message);
        assertTrue(incoming.isAtBot());
        assertFalse(incoming.isAtOnly());
        assertEquals("张三", incoming.getUserName());
        assertTrue(incoming.getText().contains("你好"));
    }

    @Test
    void detectsAtOnly() {
        Message message = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m2",
                 "sender":{"nickname":"张三"},
                 "message":[{"type":"at","data":{"qq":"2"}}]}
                """, Message.class);
        AiIncoming incoming = AiMessageExtractor.extract(message);
        assertTrue(incoming.isAtOnly());
        assertEquals("", incoming.getText());
    }

    @Test
    void usesImagePlaceholder() {
        Message message = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m3",
                 "message":[{"type":"image","data":{"file":"x.jpg"}}]}
                """, Message.class);
        AiIncoming incoming = AiMessageExtractor.extract(message);
        assertTrue(incoming.isHasImage());
        assertEquals("[图片]", incoming.getText());
    }
}
