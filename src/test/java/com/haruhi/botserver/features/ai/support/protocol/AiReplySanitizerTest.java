package com.haruhi.botserver.features.ai.support.protocol;

import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiReplySanitizerTest {

    @Test
    void removesCqAndMarkdown() {
        AiReplyMessage message = new AiReplyMessage();
        message.setText("**你好** [CQ:at,qq=all] 世界");
        List<AiReplyMessage> result = AiReplySanitizer.clean(List.of(message), null);
        assertEquals("你好 世界", result.get(0).getText());
    }

    @Test
    void dropsBracketOnlyMonologue() {
        AiReplyMessage message = new AiReplyMessage();
        message.setText("（不关我事，潜水）");
        assertTrue(AiReplySanitizer.clean(List.of(message), null).isEmpty());
    }

    @Test
    void dropsInternalPromptMarker() {
        AiReplyMessage message = new AiReplyMessage();
        message.setText("【安全与权限】我不能说");
        assertTrue(AiReplySanitizer.clean(List.of(message), null).isEmpty());
    }

    @Test
    void splitsLongText() {
        AiReplyMessage message = new AiReplyMessage();
        message.setText("测".repeat(700));
        List<AiReplyMessage> result = AiReplySanitizer.clean(List.of(message), null);
        assertFalse(result.isEmpty());
        assertTrue(result.size() > 1);
        for (AiReplyMessage item : result) {
            assertTrue(item.getText().length() <= 300);
        }
    }

    @Test
    void deduplicatesConsecutiveSameText() {
        AiReplyMessage first = new AiReplyMessage();
        first.setText("草");
        AiReplyMessage second = new AiReplyMessage();
        second.setText("草");
        assertEquals(1, AiReplySanitizer.clean(List.of(first, second), null).size());
    }
}
