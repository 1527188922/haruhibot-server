package com.haruhi.botserver.features.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.persistence.entity.AiMessageEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiSessionEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiUsageEntity;
import com.haruhi.botserver.features.ai.persistence.mapper.AiMessageMapper;
import com.haruhi.botserver.features.ai.persistence.mapper.AiSessionMapper;
import com.haruhi.botserver.features.ai.persistence.mapper.AiUsageMapper;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话/消息持久化 + 内存缓存。
 * <p>
 * 队列与 worker 在 AiChatService 中，避免会话存储和编排互相依赖。
 */
@Service
@Slf4j
public class AiSessionService {

    private final Map<AiSessionKey, AiSessionState> cache = new ConcurrentHashMap<>();

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiUsageMapper usageMapper;

    public AiSessionService(AiSessionMapper sessionMapper,
                            AiMessageMapper messageMapper,
                            AiUsageMapper usageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.usageMapper = usageMapper;
    }

    public AiSessionState load(AiSessionKey key) {
        AiSessionState state = cache.get(key);
        if (state != null) {
            state.touch();
            return state;
        }
        cleanupExpired();
        return cache.computeIfAbsent(key, this::loadFromDb);
    }

    /**
     * 只读取已缓存会话的角色，避免每条未触发消息都打数据库。
     */
    public String cachedRoleName(AiSessionKey key) {
        AiSessionState state = key == null ? null : cache.get(key);
        return state == null ? null : state.roleName();
    }

    /**
     * ds off 后，已缓存会话不再排队；未缓存的会话由 worker 的 paused 判断兜底。
     */
    public boolean isCachedPaused(AiSessionKey key) {
        AiSessionState state = key == null ? null : cache.get(key);
        return state != null && state.paused();
    }

    private void cleanupExpired() {
        if (cache.size() < 200) {
            return;
        }
        long expire = AiChatSettings.sessionExpireMinutes() * 60_000L;
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> now - entry.getValue().getLastActiveAt() > expire);
    }

    private AiSessionState loadFromDb(AiSessionKey key) {
        AiSessionState state = new AiSessionState(key);
        if (!AiChatSettings.persistenceEnabled()) {
            AiSessionEntity transientEntity = new AiSessionEntity();
            transientEntity.setSelfId(key.selfId());
            transientEntity.setScene(key.scene().getCode());
            transientEntity.setTargetId(key.targetId());
            transientEntity.setStatus("active");
            transientEntity.setRoleName(null);
            state.setEntity(transientEntity);
            return state;
        }
        try {
            AiSessionEntity entity = sessionMapper.selectOne(new LambdaQueryWrapper<AiSessionEntity>()
                    .eq(AiSessionEntity::getSelfId, key.selfId())
                    .eq(AiSessionEntity::getScene, key.scene().getCode())
                    .eq(AiSessionEntity::getTargetId, key.targetId())
                    .last("limit 1"));
            if (entity == null) {
                entity = new AiSessionEntity();
                entity.setSelfId(key.selfId());
                entity.setScene(key.scene().getCode());
                entity.setTargetId(key.targetId());
                entity.setStatus("active");
                entity.setCreatedAt(now());
                entity.setUpdatedAt(now());
                sessionMapper.insert(entity);
            }
            state.setEntity(entity);
            int max = AiChatSettings.maxMessagesPerSession();
            List<AiMessageEntity> messages = messageMapper.selectList(new LambdaQueryWrapper<AiMessageEntity>()
                    .eq(AiMessageEntity::getSessionId, entity.getId())
                    .orderByDesc(AiMessageEntity::getId)
                    .last("limit " + max));
            messages.sort(Comparator.comparing(AiMessageEntity::getId));
            state.getHistory().addAll(messages);
        } catch (Exception e) {
            log.error("加载AI会话失败: {}", key.cacheKey(), e);
            AiSessionEntity transientEntity = new AiSessionEntity();
            transientEntity.setSelfId(key.selfId());
            transientEntity.setScene(key.scene().getCode());
            transientEntity.setTargetId(key.targetId());
            transientEntity.setStatus("active");
            state.setEntity(transientEntity);
        }
        return state;
    }

    public void appendUser(AiSessionState state, AiIncoming incoming) {
        state.touch();
        appendMessage(state, "user", incoming.getUserId(), incoming.getUserName(),
                incoming.getText(), incoming.getMessageId());
    }

    public void appendAssistant(AiSessionState state, String text) {
        state.touch();
        appendMessage(state, "assistant", state.getKey().selfId(), "你", text, null);
    }

    private void appendMessage(AiSessionState state, String role, Long speakerId, String speakerName,
                               String content, String messageId) {
        AiMessageEntity message = new AiMessageEntity();
        message.setRole(role);
        message.setSpeakerId(speakerId);
        message.setSpeakerName(speakerName);
        message.setContent(content == null ? "" : content);
        message.setMessageId(messageId);
        message.setCreatedAt(now());
        Long sessionId = state.getEntity() == null ? null : state.getEntity().getId();
        message.setSessionId(sessionId);
        if (AiChatSettings.persistenceEnabled() && sessionId != null) {
            try {
                messageMapper.insert(message);
                state.getHistory().add(message);
                touchEntity(state);
                trim(state);
            } catch (Exception e) {
                log.error("写入AI消息失败: session={}", sessionId, e);
                state.getHistory().add(message);
            }
        } else {
            state.getHistory().add(message);
            trim(state);
        }
    }

    private void touchEntity(AiSessionState state) {
        AiSessionEntity entity = state.getEntity();
        if (entity == null || entity.getId() == null || !AiChatSettings.persistenceEnabled()) {
            return;
        }
        entity.setLastActiveAt(now());
        entity.setUpdatedAt(now());
        try {
            sessionMapper.updateById(entity);
        } catch (Exception e) {
            log.debug("更新AI会话活跃时间失败: {}", entity.getId(), e);
        }
    }

    private void trim(AiSessionState state) {
        int max = AiChatSettings.maxMessagesPerSession();
        while (state.getHistory().size() > max) {
            AiMessageEntity oldest = state.getHistory().remove(0);
            if (oldest.getId() != null) {
                try {
                    messageMapper.deleteById(oldest.getId());
                } catch (Exception e) {
                    log.warn("删除过期AI消息失败: {}", oldest.getId(), e);
                }
            }
        }
    }

    public void updateRole(AiSessionState state, String roleName) {
        if (state.getEntity() == null) {
            return;
        }
        state.getEntity().setRoleName(roleName);
        state.getEntity().setUpdatedAt(now());
        if (AiChatSettings.persistenceEnabled() && state.getEntity().getId() != null) {
            try {
                sessionMapper.updateById(state.getEntity());
            } catch (Exception e) {
                log.warn("更新AI会话角色失败: {}", state.getEntity().getId(), e);
            }
        }
    }

    public void updateStatus(AiSessionState state, String status) {
        if (state.getEntity() == null) {
            return;
        }
        state.getEntity().setStatus(status);
        state.getEntity().setUpdatedAt(now());
        if (AiChatSettings.persistenceEnabled() && state.getEntity().getId() != null) {
            try {
                sessionMapper.updateById(state.getEntity());
            } catch (Exception e) {
                log.warn("更新AI会话状态失败: {}", state.getEntity().getId(), e);
            }
        }
    }

    public void reset(AiSessionState state) {
        state.getHistory().clear();
        state.nextGeneration();
        if (AiChatSettings.persistenceEnabled() && state.getEntity() != null && state.getEntity().getId() != null) {
            try {
                messageMapper.delete(new LambdaQueryWrapper<AiMessageEntity>()
                        .eq(AiMessageEntity::getSessionId, state.getEntity().getId()));
            } catch (Exception e) {
                log.error("清空AI会话消息失败: {}", state.getEntity().getId(), e);
            }
        }
    }

    public void recordUsage(AiSessionState state, String model, Integer promptTokens, Integer completionTokens,
                            Integer totalTokens, Integer cacheHitTokens, int latencyMs, int toolCalls,
                            boolean success, String errorCode) {
        AiUsageEntity usage = new AiUsageEntity();
        usage.setSessionId(state.getEntity() == null ? null : state.getEntity().getId());
        usage.setModel(model);
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(totalTokens);
        usage.setCacheHitTokens(cacheHitTokens);
        usage.setLatencyMs(latencyMs);
        usage.setToolCalls(toolCalls);
        usage.setSuccess(success ? 1 : 0);
        usage.setErrorCode(errorCode);
        usage.setCreatedAt(now());
        if (!AiChatSettings.persistenceEnabled()) {
            return;
        }
        try {
            usageMapper.insert(usage);
        } catch (Exception e) {
            log.warn("写入AI用量失败", e);
        }
    }

    public long todayTokens(AiSessionState state) {
        if (!AiChatSettings.persistenceEnabled() || state.getEntity() == null || state.getEntity().getId() == null) {
            return 0;
        }
        try {
            List<AiUsageEntity> list = usageMapper.selectList(new LambdaQueryWrapper<AiUsageEntity>()
                    .eq(AiUsageEntity::getSessionId, state.getEntity().getId())
                    .ge(AiUsageEntity::getCreatedAt,
                            DateTimeUtil.dateTimeFormat(DateTimeUtil.getStartOfDay(new Date()), DateTimeUtil.PatternEnum.yyyyMMddHHmmss)));
            return list.stream().mapToLong(item -> item.getTotalTokens() == null ? 0 : item.getTotalTokens()).sum();
        } catch (Exception e) {
            return 0;
        }
    }

    public void evict(AiSessionKey key) {
        cache.remove(key);
    }

    private String now() {
        return DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }
}
