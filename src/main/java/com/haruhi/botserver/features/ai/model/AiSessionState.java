package com.haruhi.botserver.features.ai.model;

import com.haruhi.botserver.features.ai.persistence.entity.AiMessageEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiSessionEntity;
import lombok.Data;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存中的会话状态。
 */
@Data
public class AiSessionState {

    private final AiSessionKey key;
    private AiSessionEntity entity;
    private final List<AiMessageEntity> history = new CopyOnWriteArrayList<>();
    private volatile long generation;
    private volatile long lastActiveAt = System.currentTimeMillis();

    public AiSessionState(AiSessionKey key) {
        this.key = key;
    }

    public boolean paused() {
        return entity != null && "paused".equalsIgnoreCase(entity.getStatus());
    }

    public String roleName() {
        return entity == null ? null : entity.getRoleName();
    }

    public long generation() {
        return generation;
    }

    public long nextGeneration() {
        return ++generation;
    }

    public void touch() {
        lastActiveAt = System.currentTimeMillis();
    }
}
