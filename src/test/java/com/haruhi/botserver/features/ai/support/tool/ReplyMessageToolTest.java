package com.haruhi.botserver.features.ai.support.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.features.ai.model.AiScene;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.integration.onebot.model.SyncResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReplyMessageToolTest {

    private final ReplyMessageTool tool = new ReplyMessageTool();

    @Test
    void readsRepliedMessageInPrivate() {
        Bot bot = mock(Bot.class);
        when(bot.getMsg("r1", 2500L)).thenReturn(ok(replyMessage()));

        AiSessionState state = state(new AiSessionKey(2L, AiScene.PRIVATE, 1L));
        state.setToolContext("r1", true);

        String result = tool.execute(bot, state, new JSONObject());
        assertTrue(result.contains("引用消息"));
        assertTrue(result.contains("三儿(1)"));
        assertTrue(result.contains("原文内容"));
    }

    @Test
    void readsRepliedMessageInGroupWhenDirect() {
        Bot bot = mock(Bot.class);
        when(bot.getMsg("r1", 2500L)).thenReturn(ok(replyMessage()));

        AiSessionState state = state(new AiSessionKey(2L, AiScene.GROUP, 10L));
        state.setToolContext("r1", true);

        String result = tool.execute(bot, state, new JSONObject());
        assertTrue(result.contains("原文内容"));
    }

    @Test
    void rejectsGroupReplyWithoutMention() {
        AiSessionState state = state(new AiSessionKey(2L, AiScene.GROUP, 10L));
        state.setToolContext("r1", false);

        String result = tool.execute(mock(Bot.class), state, new JSONObject());
        assertTrue(result.contains("没有 @ 或提及机器人"));
    }

    @Test
    void rejectsWhenNoReplyId() {
        AiSessionState state = state(new AiSessionKey(2L, AiScene.PRIVATE, 1L));
        state.setToolContext(null, true);

        String result = tool.execute(mock(Bot.class), state, new JSONObject());
        assertTrue(result.contains("没有引用"));
    }

    @Test
    void reportsFailureWhenResponseIsNotSuccess() {
        Bot bot = mock(Bot.class);
        SyncResponse<Message> failed = new SyncResponse<>();
        failed.setRetcode(500);
        failed.setStatus(SyncResponse.STATUS_FAILED);
        when(bot.getMsg("r1", 2500L)).thenReturn(failed);

        AiSessionState state = state(new AiSessionKey(2L, AiScene.PRIVATE, 1L));
        state.setToolContext("r1", true);

        String result = tool.execute(bot, state, new JSONObject());
        assertFalse(result.contains("原文内容"));
        assertTrue(result.contains("获取失败"));
    }

    private AiSessionState state(AiSessionKey key) {
        return new AiSessionState(key);
    }

    private SyncResponse<Message> ok(Message message) {
        SyncResponse<Message> response = new SyncResponse<>();
        response.setRetcode(0);
        response.setStatus(SyncResponse.STATUS_OK);
        response.setData(message);
        return response;
    }

    private Message replyMessage() {
        return JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"r1","time":1700000000,
                 "sender":{"nickname":"张三","card":"三儿"},
                 "message":[{"type":"text","data":{"text":"原文内容"}}]}
                """, Message.class);
    }
}
