package com.haruhi.botserver.features.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import com.haruhi.botserver.features.ai.persistence.mapper.AiMemoryMapper;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 会话级长期记忆。所有读写都以 t_ai_session.id 为作用域，群/私聊互不串。
 */
@Service
@Slf4j
public class AiMemoryService {

    private final AiMemoryMapper memoryMapper;

    public AiMemoryService(AiMemoryMapper memoryMapper) {
        this.memoryMapper = memoryMapper;
    }

    public boolean enabled() {
        return AiChatSettings.memoryEnabled() && AiChatSettings.persistenceEnabled();
    }

    /**
     * 注入 prompt 用，按最近更新时间倒序。
     */
    public List<AiMemoryEntity> list(AiSessionState state, int limit) {
        Long sessionId = sessionId(state);
        if (!enabled() || sessionId == null || memoryMapper == null) {
            return List.of();
        }
        try {
            int max = Math.max(1, Math.min(limit, AiChatSettings.memoryMaxEntries()));
            List<AiMemoryEntity> list = memoryMapper.selectList(new LambdaQueryWrapper<AiMemoryEntity>()
                    .eq(AiMemoryEntity::getSessionId, sessionId)
                    .orderByDesc(AiMemoryEntity::getUpdatedAt)
                    .orderByDesc(AiMemoryEntity::getId)
                    .last("limit " + max));
            return list == null ? List.of() : list;
        } catch (Exception e) {
            log.warn("读取AI长期记忆失败: session={}", sessionId, e);
            return List.of();
        }
    }

    public int rememberAll(AiSessionState state, Map<String, String> memories) {
        if (!enabled() || memories == null || memories.isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (Map.Entry<String, String> entry : memories.entrySet()) {
            if (remember(state, entry.getKey(), entry.getValue())) {
                saved++;
            }
        }
        return saved;
    }

    public int forgetAll(AiSessionState state, List<String> keys) {
        if (!enabled() || keys == null || keys.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (String key : keys) {
            if (forget(state, key)) {
                removed++;
            }
        }
        return removed;
    }

    public boolean remember(AiSessionState state, String key, String value) {
        Long sessionId = sessionId(state);
        if (!enabled() || sessionId == null || memoryMapper == null) {
            return false;
        }
        String safeKey = normalizeKey(key);
        String safeValue = normalizeValue(value);
        if (safeKey == null || safeValue == null) {
            return false;
        }
        try {
            AiMemoryEntity existing = memoryMapper.selectOne(new LambdaQueryWrapper<AiMemoryEntity>()
                    .eq(AiMemoryEntity::getSessionId, sessionId)
                    .eq(AiMemoryEntity::getMemKey, safeKey)
                    .last("limit 1"));
            String now = now();
            if (existing == null) {
                AiMemoryEntity entity = new AiMemoryEntity();
                entity.setSessionId(sessionId);
                entity.setMemKey(safeKey);
                entity.setMemValue(safeValue);
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                memoryMapper.insert(entity);
            } else {
                existing.setMemValue(safeValue);
                existing.setUpdatedAt(now);
                memoryMapper.updateById(existing);
            }
            trim(sessionId);
            return true;
        } catch (Exception e) {
            log.warn("写入AI长期记忆失败: session={}, key={}", sessionId, safeKey, e);
            return false;
        }
    }

    public boolean forget(AiSessionState state, String key) {
        Long sessionId = sessionId(state);
        if (!enabled() || sessionId == null || memoryMapper == null) {
            return false;
        }
        String safeKey = normalizeKey(key);
        if (safeKey == null) {
            return false;
        }
        try {
            return memoryMapper.delete(new LambdaQueryWrapper<AiMemoryEntity>()
                    .eq(AiMemoryEntity::getSessionId, sessionId)
                    .eq(AiMemoryEntity::getMemKey, safeKey)) > 0;
        } catch (Exception e) {
            log.warn("删除AI长期记忆失败: session={}, key={}", sessionId, safeKey, e);
            return false;
        }
    }

    public boolean clear(AiSessionState state) {
        Long sessionId = sessionId(state);
        if (!enabled() || sessionId == null || memoryMapper == null) {
            return false;
        }
        try {
            memoryMapper.delete(new LambdaQueryWrapper<AiMemoryEntity>()
                    .eq(AiMemoryEntity::getSessionId, sessionId));
            return true;
        } catch (Exception e) {
            log.warn("清空AI长期记忆失败: session={}", sessionId, e);
            return false;
        }
    }

    public String format(List<AiMemoryEntity> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (AiMemoryEntity memory : memories) {
            if (memory == null || StringUtils.isBlank(memory.getMemKey())) {
                continue;
            }
            sb.append("- ").append(memory.getMemKey()).append(": ")
                    .append(StringUtils.defaultString(memory.getMemValue())).append('\n');
        }
        return sb.toString().trim();
    }

    private void trim(Long sessionId) {
        int max = AiChatSettings.memoryMaxEntries();
        List<AiMemoryEntity> all = memoryMapper.selectList(new LambdaQueryWrapper<AiMemoryEntity>()
                .eq(AiMemoryEntity::getSessionId, sessionId)
                .orderByDesc(AiMemoryEntity::getUpdatedAt)
                .orderByDesc(AiMemoryEntity::getId));
        if (all == null || all.size() <= max) {
            return;
        }
        for (int i = max; i < all.size(); i++) {
            AiMemoryEntity old = all.get(i);
            if (old.getId() != null) {
                memoryMapper.deleteById(old.getId());
            }
        }
    }

    private Long sessionId(AiSessionState state) {
        if (state == null || state.getEntity() == null) {
            return null;
        }
        return state.getEntity().getId();
    }

    private String normalizeKey(String key) {
        if (StringUtils.isBlank(key)) {
            return null;
        }
        String value = key.replaceAll("[\\r\\n\\t]", " ").replaceAll("\\s{2,}", " ").trim();
        if (value.isBlank()) {
            return null;
        }
        return value.length() <= 30 ? value : value.substring(0, 30);
    }

    private String normalizeValue(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String text = value.replace("\r\n", "\n").replace('\r', '\n')
                .replaceAll("[\\p{Cntrl}&&[^\\n]]", "")
                .replaceAll("\\n+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        if (text.isBlank()) {
            return null;
        }
        int max = AiChatSettings.memoryMaxValueChars();
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String now() {
        return DateTimeUtil.dateTimeFormat(new Date(), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }
}
