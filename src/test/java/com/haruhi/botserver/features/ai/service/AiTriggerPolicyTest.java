package com.haruhi.botserver.features.ai.service;

import com.alibaba.fastjson.JSON;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiTriggerDecision;
import com.haruhi.botserver.features.ai.support.AiMessageExtractor;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiTriggerPolicyTest {

    @TempDir
    Path dir;

    private AiTriggerPolicy policy;
    private Bot bot;

    @BeforeEach
    void setUp() throws Exception {
        Configs.useConfigDirForTest(dir.toString());
        Path roles = dir.resolve("aichat/roles");
        Files.createDirectories(roles);
        Files.writeString(roles.resolve("春日酱.md"), """
                # 角色卡：春日酱
                触发词：春日酱,团长
                ## 你是谁
                你是春日酱。
                """);
        RoleCardService roleCardService = new RoleCardService();
        AiSessionService sessionService = new AiSessionService(null, null, null);
        policy = new AiTriggerPolicy(roleCardService, sessionService);
        bot = mock(Bot.class);
        when(bot.getId()).thenReturn(2L);
    }

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void atBotTriggers() {
        Message message = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m1",
                 "message":[{"type":"at","data":{"qq":"2"}},{"type":"text","data":{"text":" 在吗"}}]}
                """, Message.class);
        AiIncoming incoming = AiMessageExtractor.extract(message);
        AiTriggerDecision decision = policy.decide(bot, message, incoming);
        assertTrue(decision.triggered());
        assertTrue(decision.direct());
    }

    @Test
    void roleTriggerNeedsAddressSignal() {
        Message message = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m2",
                 "message":[{"type":"text","data":{"text":"春日酱在吗"}}]}
                """, Message.class);
        AiIncoming incoming = AiMessageExtractor.extract(message);
        assertTrue(policy.decide(bot, message, incoming).triggered());

        Message thirdParty = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m3",
                 "message":[{"type":"text","data":{"text":"他说春日酱很可爱"}}]}
                """, Message.class);
        assertFalse(policy.decide(bot, thirdParty, AiMessageExtractor.extract(thirdParty)).triggered());
    }

    @Test
    void privateTextTriggers() {
        Message message = JSON.parseObject("""
                {"message_type":"private","self_id":2,"user_id":1,"message_id":"m4",
                 "message":[{"type":"text","data":{"text":"在吗"}}]}
                """, Message.class);
        AiIncoming incoming = AiMessageExtractor.extract(message);
        assertTrue(policy.decide(bot, message, incoming).triggered());
    }
}
