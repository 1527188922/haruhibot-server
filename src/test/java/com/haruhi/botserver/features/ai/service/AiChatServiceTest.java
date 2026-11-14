package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekChatResponse;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekClient;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiReplyMessage;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.model.AiTurn;
import com.haruhi.botserver.features.ai.support.prompt.PromptAssembler;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void processesSayAndCallsSend() throws Exception {
        Files.createDirectories(dir.resolve("config"));
        Files.writeString(dir.resolve("config/ai.properties"), "ds.chat.persistence.enable=false\n");
        Configs.useConfigDirForTest(dir.toString());
        RoleCardService roleCardService = new RoleCardService();
        AiSessionService sessionService = new AiSessionService(null, null, null);
        AiContextService contextService = new AiContextService();
        PromptAssembler promptAssembler = new PromptAssembler(roleCardService);
        DeepSeekClient client = mock(DeepSeekClient.class);
        AiSendService sendService = mock(AiSendService.class);
        AiToolService toolService = mock(AiToolService.class);

        DeepSeekChatResponse response = new DeepSeekChatResponse();
        response.setContent("{\"type\":\"say\",\"messages\":[{\"text\":\"在\",\"delay_ms\":0}]}");
        response.setFinishReason("stop");
        when(client.chat(anyList(), anyString(), any())).thenReturn(response);

        CountDownLatch sent = new CountDownLatch(1);
        doAnswer(invocation -> {
            sent.countDown();
            return null;
        }).when(sendService).send(any(), any(), any(), anyList(), any(), anyList());

        AiChatService service = new AiChatService(sessionService, contextService, promptAssembler,
                client, sendService, toolService);

        AiIncoming incoming = new AiIncoming();
        incoming.setKey(new AiSessionKey(2L, com.haruhi.botserver.features.ai.model.AiScene.GROUP, 10L));
        incoming.setUserId(1L);
        incoming.setUserName("张三");
        incoming.setMessageId("m1");
        incoming.setText("在吗");
        AiTurn turn = new AiTurn();
        turn.setBot(mock(Bot.class));
        turn.setIncoming(incoming);
        turn.setNotBefore(System.currentTimeMillis());
        turn.setDeadline(System.currentTimeMillis() + 1000);

        service.enqueue(incoming.getKey(), turn);
        assertTrue(sent.await(5, TimeUnit.SECONDS));
    }
}
