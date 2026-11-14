package com.haruhi.botserver.features.ai.service;

import com.haruhi.botserver.features.ai.model.AiScene;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiSessionEntity;
import com.haruhi.botserver.features.ai.persistence.mapper.AiMemoryMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiMemoryServiceTest {

    @Test
    void formatsMemories() {
        AiMemoryMapper mapper = mock(AiMemoryMapper.class);
        AiMemoryEntity memory = new AiMemoryEntity();
        memory.setMemKey("称呼");
        memory.setMemValue("小明");
        when(mapper.selectList(any())).thenReturn(List.of(memory));

        AiMemoryService service = new AiMemoryService(mapper);
        AiSessionState state = state();
        assertEquals("- 称呼: 小明", service.format(service.list(state, 10)));
    }

    @Test
    void rememberInsertsNewMemory() {
        AiMemoryMapper mapper = mock(AiMemoryMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.selectList(any())).thenReturn(List.of());

        AiMemoryService service = new AiMemoryService(mapper);
        assertTrue(service.remember(state(), "爱好", "喜欢咖啡"));
        verify(mapper).insert(any(AiMemoryEntity.class));
    }

    @Test
    void forgetDeletesByKey() {
        AiMemoryMapper mapper = mock(AiMemoryMapper.class);
        when(mapper.delete(any())).thenReturn(1);

        AiMemoryService service = new AiMemoryService(mapper);
        assertTrue(service.forget(state(), "爱好"));
    }

    private AiSessionState state() {
        AiSessionState state = new AiSessionState(new AiSessionKey(2L, AiScene.GROUP, 10L));
        AiSessionEntity entity = new AiSessionEntity();
        entity.setId(1L);
        entity.setStatus("active");
        state.setEntity(entity);
        return state;
    }
}
