package com.haruhi.botserver.features.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekBalanceResponse;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekClient;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekMessage;
import com.haruhi.botserver.features.ai.model.AiPrompt;
import com.haruhi.botserver.features.ai.model.admin.AiOverviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRolePreviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleView;
import com.haruhi.botserver.features.ai.model.admin.AiSessionDetailResp;
import com.haruhi.botserver.features.ai.model.admin.AiSessionQueryReq;
import com.haruhi.botserver.features.ai.model.admin.AiSessionView;
import com.haruhi.botserver.features.ai.persistence.entity.AiMemoryEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiMessageEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiSessionEntity;
import com.haruhi.botserver.features.ai.persistence.entity.AiUsageEntity;
import com.haruhi.botserver.features.ai.persistence.mapper.AiMemoryMapper;
import com.haruhi.botserver.features.ai.persistence.mapper.AiMessageMapper;
import com.haruhi.botserver.features.ai.persistence.mapper.AiSessionMapper;
import com.haruhi.botserver.features.ai.persistence.mapper.AiUsageMapper;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.features.ai.support.EmojiService;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * WebUI AI 管理只读查询（P0）。
 */
@Service
@Slf4j
public class AiAdminService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiUsageMapper usageMapper;
    private final AiMemoryMapper memoryMapper;
    private final RoleCardService roleCardService;
    private final EmojiService emojiService;
    private final DeepSeekClient deepSeekClient;

    public AiAdminService(AiSessionMapper sessionMapper,
                          AiMessageMapper messageMapper,
                          AiUsageMapper usageMapper,
                          AiMemoryMapper memoryMapper,
                          RoleCardService roleCardService,
                          EmojiService emojiService,
                          DeepSeekClient deepSeekClient) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.usageMapper = usageMapper;
        this.memoryMapper = memoryMapper;
        this.roleCardService = roleCardService;
        this.emojiService = emojiService;
        this.deepSeekClient = deepSeekClient;
    }

    public AiOverviewResp overview() {
        AiOverviewResp resp = new AiOverviewResp();
        resp.setToday(todayUsage());
        resp.setRuntime(runtime());
        resp.setBalance(balance());
        return resp;
    }

    public IPage<AiSessionView> searchSessions(AiSessionQueryReq req) {
        LambdaQueryWrapper<AiSessionEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(req.getSelfId() != null, AiSessionEntity::getSelfId, req.getSelfId())
                .eq(StringUtils.isNotBlank(req.getScene()), AiSessionEntity::getScene, req.getScene())
                .eq(req.getTargetId() != null, AiSessionEntity::getTargetId, req.getTargetId())
                .eq(StringUtils.isNotBlank(req.getRoleName()), AiSessionEntity::getRoleName, req.getRoleName())
                .eq(StringUtils.isNotBlank(req.getStatus()), AiSessionEntity::getStatus, req.getStatus());
        applyTimeRange(wrapper, req.getDatetimerange());
        wrapper.orderByDesc(AiSessionEntity::getLastActiveAt).orderByDesc(AiSessionEntity::getId);

        Page<AiSessionEntity> source = sessionMapper.selectPage(
                new Page<>(req.getCurrentPage(), req.getPageSize()), wrapper);
        List<Long> ids = source.getRecords().stream().map(AiSessionEntity::getId).filter(Objects::nonNull).toList();
        Map<Long, Long> messageCounts = messageCounts(ids);
        Map<Long, Long> todayTokens = todayTokens(ids);

        Page<AiSessionView> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords().stream()
                .map(entity -> toView(entity, messageCounts, todayTokens))
                .toList());
        return target;
    }

    public AiSessionDetailResp detail(Long id) {
        if (id == null) {
            return null;
        }
        AiSessionEntity entity = sessionMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        List<Long> ids = List.of(id);
        AiSessionDetailResp resp = new AiSessionDetailResp();
        resp.setSession(toView(entity, messageCounts(ids), todayTokens(ids)));
        resp.setEffectiveRole(effectiveRole(entity));
        resp.setMemoryCount(memoryMapper.selectCount(new LambdaQueryWrapper<AiMemoryEntity>()
                .eq(AiMemoryEntity::getSessionId, id)));
        applyTotalUsage(resp, id);
        return resp;
    }

    public List<AiMessageEntity> messages(Long id, Integer limit) {
        if (id == null) {
            return List.of();
        }
        int max = limit == null ? 50 : Math.max(1, Math.min(limit, 200));
        List<AiMessageEntity> list = messageMapper.selectList(new LambdaQueryWrapper<AiMessageEntity>()
                .eq(AiMessageEntity::getSessionId, id)
                .orderByDesc(AiMessageEntity::getId)
                .last("limit " + max));
        List<AiMessageEntity> result = new ArrayList<>(list == null ? List.of() : list);
        Collections.reverse(result);
        return result;
    }

    public List<AiMemoryEntity> memories(Long id) {
        if (id == null) {
            return List.of();
        }
        return memoryMapper.selectList(new LambdaQueryWrapper<AiMemoryEntity>()
                .eq(AiMemoryEntity::getSessionId, id)
                .orderByDesc(AiMemoryEntity::getUpdatedAt)
                .orderByDesc(AiMemoryEntity::getId));
    }

    public List<AiUsageEntity> usage(Long id, Integer limit) {
        if (id == null) {
            return List.of();
        }
        int max = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        return usageMapper.selectList(new LambdaQueryWrapper<AiUsageEntity>()
                .eq(AiUsageEntity::getSessionId, id)
                .orderByDesc(AiUsageEntity::getId)
                .last("limit " + max));
    }

    public List<AiRoleView> roleList() {
        Path dir = roleCardService.roleDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        Map<String, Long> used = roleUsage();
        try (Stream<Path> stream = Files.list(dir)) {
            List<AiRoleView> result = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                    .map(path -> toRoleView(path, used))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(AiRoleView::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            return result;
        } catch (IOException e) {
            log.warn("扫描角色卡目录失败: {}", dir, e);
            return List.of();
        }
    }

    public AiRolePreviewResp rolePreview(String name) {
        RoleCardService.RoleCard card = roleCardService.load(name);
        if (card == null) {
            return null;
        }
        AiRolePreviewResp resp = new AiRolePreviewResp();
        resp.setName(card.name());
        resp.setFileName(card.name() + ".md");
        resp.setChars(card.content().length());
        resp.setTriggerWords(card.triggerWords());
        resp.setWarnings(card.warnings());
        resp.setContent(card.content());
        resp.setEstimatedTokens(AiPrompt.estimate(DeepSeekMessage.system(card.content())));
        resp.setToolsEnabled(AiChatSettings.agentEnabled());
        resp.setVisionEnabled(AiChatSettings.visionEnabled());
        try {
            Path file = roleCardService.roleDir().resolve(card.name() + ".md");
            if (Files.isRegularFile(file)) {
                resp.setUpdatedAtText(formatDateTime(Files.getLastModifiedTime(file).toMillis()));
            }
        } catch (IOException e) {
            log.debug("读取角色卡时间失败: {}", card.name(), e);
        }
        return resp;
    }

    public List<AiRoleView> roleRescan() {
        roleCardService.clearCache();
        return roleList();
    }

    // ==================== 概览 ====================

    private AiOverviewResp.Usage todayUsage() {
        AiOverviewResp.Usage usage = new AiOverviewResp.Usage();
        QueryWrapper<AiUsageEntity> wrapper = new QueryWrapper<>();
        wrapper.select("COUNT(*) AS requests",
                        "COALESCE(SUM(prompt_tokens),0) AS prompt_tokens",
                        "COALESCE(SUM(completion_tokens),0) AS completion_tokens",
                        "COALESCE(SUM(total_tokens),0) AS total_tokens",
                        "COALESCE(SUM(cache_hit_tokens),0) AS cache_hit_tokens",
                        "COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END),0) AS failures",
                        "COALESCE(AVG(latency_ms),0) AS avg_latency_ms")
                .ge("created_at", startOfToday());
        Map<String, Object> row = firstMap(usageMapper.selectMaps(wrapper));
        usage.setRequests(mapLong(row, "requests"));
        usage.setPromptTokens(mapLong(row, "prompt_tokens"));
        usage.setCompletionTokens(mapLong(row, "completion_tokens"));
        usage.setTotalTokens(mapLong(row, "total_tokens"));
        usage.setCacheHitTokens(mapLong(row, "cache_hit_tokens"));
        usage.setFailures(mapLong(row, "failures"));
        usage.setAvgLatencyMs(mapDouble(row, "avg_latency_ms"));
        return usage;
    }

    private AiOverviewResp.Runtime runtime() {
        AiOverviewResp.Runtime runtime = new AiOverviewResp.Runtime();
        runtime.setEnabled(AiChatSettings.enabled());
        runtime.setModel(AiChatSettings.model());
        runtime.setThinking(AiChatSettings.thinkingEnabled());
        runtime.setGroupWhitelistCount(AiChatSettings.groupWhitelist().size());
        runtime.setPrivateEnabled(AiChatSettings.privateEnabled());
        runtime.setDailyTokenLimit(AiChatSettings.dailyTokenLimit());
        runtime.setRoleCount(roleCardService.listRoleNames().size());
        runtime.setEmojiCount(emojiService.list().size());
        runtime.setSearchEnabled(AiChatSettings.searchAvailable());
        runtime.setVisionEnabled(AiChatSettings.visionEnabled());
        runtime.setAgentEnabled(AiChatSettings.agentEnabled());
        return runtime;
    }

    private AiOverviewResp.Balance balance() {
        AiOverviewResp.Balance balance = new AiOverviewResp.Balance();
        try {
            DeepSeekBalanceResponse response = deepSeekClient.getBalance();
            if (response == null) {
                balance.setError("余额接口没有返回内容");
                return balance;
            }
            balance.setAvailable(response.isAvailable());
            for (DeepSeekBalanceResponse.BalanceInfo info : response.getBalances()) {
                AiOverviewResp.Info item = new AiOverviewResp.Info();
                item.setCurrency(info.getCurrency());
                item.setTotalBalance(info.getTotalBalance());
                item.setGrantedBalance(info.getGrantedBalance());
                item.setToppedUpBalance(info.getToppedUpBalance());
                balance.getItems().add(item);
            }
        } catch (Exception e) {
            balance.setAvailable(false);
            balance.setError(StringUtils.defaultIfBlank(e.getMessage(), "余额查询失败"));
        }
        return balance;
    }

    // ==================== 会话 ====================

    private AiSessionView toView(AiSessionEntity entity,
                                 Map<Long, Long> messageCounts,
                                 Map<Long, Long> todayTokens) {
        AiSessionView view = new AiSessionView();
        view.setId(entity.getId());
        view.setSelfId(entity.getSelfId());
        view.setScene(entity.getScene());
        view.setTargetId(entity.getTargetId());
        view.setRoleName(entity.getRoleName());
        view.setStatus(entity.getStatus());
        view.setLastActiveAt(entity.getLastActiveAt());
        view.setCreatedAt(entity.getCreatedAt());
        view.setMessageCount(messageCounts.getOrDefault(entity.getId(), 0L));
        view.setTodayTokens(todayTokens.getOrDefault(entity.getId(), 0L));
        return view;
    }

    private Map<Long, Long> messageCounts(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of();
        }
        QueryWrapper<AiMessageEntity> wrapper = new QueryWrapper<>();
        wrapper.select("session_id", "COUNT(*) AS cnt")
                .in("session_id", ids)
                .groupBy("session_id");
        return toLongMap(messageMapper.selectMaps(wrapper), "session_id", "cnt");
    }

    private Map<Long, Long> todayTokens(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Map.of();
        }
        QueryWrapper<AiUsageEntity> wrapper = new QueryWrapper<>();
        wrapper.select("session_id", "COALESCE(SUM(total_tokens),0) AS tokens")
                .in("session_id", ids)
                .ge("created_at", startOfToday())
                .groupBy("session_id");
        return toLongMap(usageMapper.selectMaps(wrapper), "session_id", "tokens");
    }

    private void applyTotalUsage(AiSessionDetailResp resp, Long sessionId) {
        QueryWrapper<AiUsageEntity> wrapper = new QueryWrapper<>();
        wrapper.select("COUNT(*) AS requests",
                        "COALESCE(SUM(total_tokens),0) AS tokens",
                        "COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END),0) AS failures",
                        "COALESCE(AVG(latency_ms),0) AS avg_latency_ms")
                .eq("session_id", sessionId);
        Map<String, Object> row = firstMap(usageMapper.selectMaps(wrapper));
        resp.setTotalRequests(mapLong(row, "requests"));
        resp.setTotalTokens(mapLong(row, "tokens"));
        resp.setFailures(mapLong(row, "failures"));
        resp.setAvgLatencyMs(mapDouble(row, "avg_latency_ms"));
    }

    private String effectiveRole(AiSessionEntity entity) {
        boolean group = "group".equalsIgnoreCase(entity.getScene());
        long groupId = group ? entity.getTargetId() : 0L;
        long userId = group ? 0L : entity.getTargetId();
        RoleCardService.RoleCard card = roleCardService.resolve(
                entity.getRoleName(), groupId, userId, group);
        return card == null ? null : card.name();
    }

    private void applyTimeRange(LambdaQueryWrapper<AiSessionEntity> wrapper, List<String> range) {
        if (CollectionUtils.isEmpty(range)) {
            return;
        }
        String start = range.get(0);
        if (StringUtils.isNotBlank(start)) {
            wrapper.ge(AiSessionEntity::getLastActiveAt, start);
        }
        if (range.size() > 1 && StringUtils.isNotBlank(range.get(1))) {
            wrapper.le(AiSessionEntity::getLastActiveAt, range.get(1));
        }
    }

    // ==================== 角色卡 ====================

    private AiRoleView toRoleView(Path path, Map<String, Long> used) {
        String fileName = path.getFileName().toString();
        String name = fileName.substring(0, fileName.length() - 3);
        RoleCardService.RoleCard card = roleCardService.load(name);
        if (card == null) {
            return null;
        }
        AiRoleView view = new AiRoleView();
        view.setName(card.name());
        view.setFileName(fileName);
        view.setChars(card.content().length());
        view.setTriggerWords(card.triggerWords());
        view.setWarnings(card.warnings());
        view.setUsedBySessions(used.getOrDefault(card.name().toLowerCase(Locale.ROOT), 0L));
        try {
            long mtime = Files.getLastModifiedTime(path).toMillis();
            view.setUpdatedAt(mtime);
            view.setUpdatedAtText(formatDateTime(mtime));
        } catch (IOException e) {
            view.setUpdatedAtText("");
        }
        return view;
    }

    private Map<String, Long> roleUsage() {
        QueryWrapper<AiSessionEntity> wrapper = new QueryWrapper<>();
        wrapper.select("role_name", "COUNT(*) AS cnt")
                .isNotNull("role_name")
                .ne("role_name", "")
                .groupBy("role_name");
        List<Map<String, Object>> rows = sessionMapper.selectMaps(wrapper);
        Map<String, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            String role = mapString(row, "role_name");
            if (StringUtils.isBlank(role)) {
                continue;
            }
            result.put(role.toLowerCase(Locale.ROOT), mapLong(row, "cnt"));
        }
        return result;
    }

    // ==================== 工具方法 ====================

    private String startOfToday() {
        return formatDateTime(DateTimeUtil.getStartOfDay(new Date()).getTime());
    }

    private String formatDateTime(long millis) {
        return DateTimeUtil.dateTimeFormat(new Date(millis), DateTimeUtil.PatternEnum.yyyyMMddHHmmss);
    }

    private Map<String, Object> firstMap(List<Map<String, Object>> rows) {
        return rows == null || rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private Map<Long, Long> toLongMap(List<Map<String, Object>> rows, String idKey, String valueKey) {
        Map<Long, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Long id = mapLongOrNull(row, idKey);
            if (id != null) {
                result.put(id, mapLong(row, valueKey));
            }
        }
        return result;
    }

    private Object mapValue(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private long mapLong(Map<String, Object> row, String key) {
        Long value = mapLongOrNull(row, key);
        return value == null ? 0L : value;
    }

    private Long mapLongOrNull(Map<String, Object> row, String key) {
        Object value = mapValue(row, key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double mapDouble(Map<String, Object> row, String key) {
        Object value = mapValue(row, key);
        if (value == null) {
            return 0D;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }

    private String mapString(Map<String, Object> row, String key) {
        Object value = mapValue(row, key);
        return value == null ? null : value.toString();
    }
}
