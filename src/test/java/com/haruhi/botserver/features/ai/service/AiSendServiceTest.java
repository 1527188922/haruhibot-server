package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.model.AiAmbientMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import com.haruhi.botserver.features.ai.model.AiScene;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.persistence.entity.AiSessionEntity;
import com.haruhi.botserver.integration.onebot.model.MessageHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSendServiceTest {

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void sendsMultipleBubblesWithHolders() throws Exception {
        Files.createDirectories(dir.resolve("config"));
        Files.writeString(dir.resolve("config/ai.properties"), "ds.chat.persistence.enable=false\n");
        Configs.useConfigDirForTest(dir.toString());

        AiSessionService sessionService = new AiSessionService(null, null, null);
        AiSendService sendService = new AiSendService(sessionService, new AiRateLimiter());
        Bot bot = mock(Bot.class);
        when(bot.isOnline()).thenReturn(true);

        AiSessionKey key = new AiSessionKey(2L, AiScene.GROUP, 10L);
        AiSessionState state = new AiSessionState(key);
        AiSessionEntity entity = new AiSessionEntity();
        entity.setStatus("active");
        state.setEntity(entity);

        AiIncoming current = new AiIncoming();
        current.setUserId(1L);
        current.setMessageId("m1");

        AiReplyMessage first = new AiReplyMessage();
        first.setText("在");
        AiReplyMessage second = new AiReplyMessage();
        second.setText("说吧");
        second.setReplyTo("m1");
        second.setAt("1");

        sendService.send(bot, state, key, List.of(first, second), current,
                List.of(new AiAmbientMessage("m0", 3L, "李四", "你好", 0)));

        verify(bot, times(2)).sendGroupMessage(eq(10L), any(List.class));
        org.junit.jupiter.api.Assertions.assertEquals(2, state.getHistory().size());
    }
}
