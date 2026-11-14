package com.haruhi.botserver.features.ai.handler;

import com.alibaba.fastjson.JSON;
import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekClient;
import com.haruhi.botserver.features.ai.service.AiSessionService;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import com.haruhi.botserver.integration.onebot.model.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeepSeekCommandHandlerTest {

    @TempDir
    Path dir;

    private DeepSeekCommandHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        Path config = dir.resolve("config");
        Files.createDirectories(config);
        Files.writeString(config.resolve("bot.properties"), "bot.superusers=1\n");
        Files.writeString(config.resolve("ai.properties"), "ds.chat.persistence.enable=false\n");
        Configs.useConfigDirForTest(dir.toString());
        handler = new DeepSeekCommandHandler(
                new AiSessionService(null, null, null),
                new RoleCardService(),
                mock(DeepSeekClient.class));
    }

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void helpOnlyForSuperAdminInGroup() {
        Bot bot = mock(Bot.class);
        Message message = JSON.parseObject("""
                {"message_type":"group","self_id":2,"group_id":10,"user_id":1,"message_id":"m1",
                 "message":[{"type":"at","data":{"qq":"2"}},{"type":"text","data":{"text":" ds help"}}]}
                """, Message.class);
        assertTrue(handler.onMessage(bot, message));
        verify(bot).sendGroupMessage(any(), anyList());
    }
}
