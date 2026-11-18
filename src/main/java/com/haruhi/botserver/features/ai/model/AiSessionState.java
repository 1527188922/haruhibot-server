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

    /** 当前状态：由 PromptAssembler 每 5~10 轮轮换一次，仅内存保存 */
    private volatile String mood;
    /** 当前状态还能持续几轮 */
    private volatile int moodTurns;

    /** 仅在一次工具循环期间有效：当前消息引用的消息 ID */
    private volatile String currentReplyMessageId;
    /** 仅在一次工具循环期间有效：当前消息是否直接触发（@、提及、引用机器人等） */
    private volatile boolean currentDirect;

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

    public void setToolContext(String replyMessageId, boolean direct) {
        this.currentReplyMessageId = replyMessageId;
        this.currentDirect = direct;
    }

    public void clearToolContext() {
        this.currentReplyMessageId = null;
        this.currentDirect = false;
    }
}
