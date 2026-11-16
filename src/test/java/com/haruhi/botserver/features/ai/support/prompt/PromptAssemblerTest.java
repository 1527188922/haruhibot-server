package com.haruhi.botserver.features.ai.support.prompt;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiPrompt;
import com.haruhi.botserver.features.ai.model.AiScene;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.model.AiTurn;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiSessionEntity;
import com.haruhi.botserver.features.ai.service.AiMemoryService;
import com.haruhi.botserver.features.ai.support.EmojiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromptAssemblerTest {

    @TempDir
    Path dir;

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void injectsLongTermMemoryIntoContext() {
        Configs.useConfigDirForTest(dir.toString());
        RoleCardService roleCardService = new RoleCardService();
        AiMemoryService memoryService = mock(AiMemoryService.class);
        AiMemoryEntity memory = new AiMemoryEntity();
        memory.setMemKey("称呼");
        memory.setMemValue("小明");
        when(memoryService.list(any(), anyInt())).thenReturn(List.of(memory));

        AiSessionState state = new AiSessionState(new AiSessionKey(2L, AiScene.GROUP, 10L));
        AiSessionEntity entity = new AiSessionEntity();
        entity.setStatus("active");
        state.setEntity(entity);

        AiIncoming incoming = new AiIncoming();
        incoming.setKey(state.getKey());
        incoming.setUserId(1L);
        incoming.setUserName("张三");
        incoming.setMessageId("m1");
        incoming.setText("在吗");
        AiTurn turn = new AiTurn();
        turn.setIncoming(incoming);

        AiPrompt prompt = new PromptAssembler(roleCardService, memoryService, new EmojiService())
                .assemble(state, List.of(turn), List.of());

        String context = prompt.getMessages().get(prompt.getMessages().size() - 1).getContent();
        assertTrue(context.contains("【长期记忆】"));
        assertTrue(context.contains("- 称呼: 小明"));
    }

    @Test
    void injectsWebSearchWhenConfigured() throws Exception {
        Files.createDirectories(dir.resolve("config"));
        Files.writeString(dir.resolve("config/ai.properties"), """
                ds.api.key=test-key
                ds.chat.agent.enable=true
                ds.chat.agent.tools=group_history,web_search
                ds.chat.search.enable=true
                """);
        Configs.useConfigDirForTest(dir.toString());
        AiMemoryService memoryService = mock(AiMemoryService.class);
        when(memoryService.list(any(), anyInt())).thenReturn(List.of());

        AiSessionState state = new AiSessionState(new AiSessionKey(2L, AiScene.GROUP, 10L));
        AiSessionEntity entity = new AiSessionEntity();
        entity.setStatus("active");
        state.setEntity(entity);

        AiIncoming incoming = new AiIncoming();
        incoming.setKey(state.getKey());
        incoming.setUserId(1L);
        incoming.setUserName("张三");
        incoming.setMessageId("m1");
        incoming.setText("今天有什么新闻");
        AiTurn turn = new AiTurn();
        turn.setIncoming(incoming);

        AiPrompt prompt = new PromptAssembler(new RoleCardService(), memoryService, new EmojiService())
                .assemble(state, List.of(turn), List.of());

        String system = prompt.getMessages().get(0).getContent();
        assertTrue(system.contains("web_search"));
    }
}
