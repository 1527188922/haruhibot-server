package com.haruhi.botserver.features.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekBalanceResponse;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekClient;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekMessage;
import com.haruhi.botserver.features.ai.model.AiAmbientMessage;
import com.haruhi.botserver.features.ai.model.AiIncoming;
import com.haruhi.botserver.features.ai.model.AiPrompt;
import com.haruhi.botserver.features.ai.model.AiScene;
import com.haruhi.botserver.features.ai.model.AiSessionKey;
import com.haruhi.botserver.features.ai.model.AiSessionState;
import com.haruhi.botserver.features.ai.model.AiTurn;
import com.haruhi.botserver.features.ai.model.admin.AiOverviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiPromptPreviewReq;
import com.haruhi.botserver.features.ai.model.admin.AiPromptPreviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleCopyReq;
import com.haruhi.botserver.features.ai.model.admin.AiRoleDetailResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleNameCheckReq;
import com.haruhi.botserver.features.ai.model.admin.AiRoleNameCheckResp;
import com.haruhi.botserver.features.ai.model.admin.AiRolePreviewResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleSaveReq;
import com.haruhi.botserver.features.ai.model.admin.AiRoleSaveResp;
import com.haruhi.botserver.features.ai.model.admin.AiRoleSection;
import com.haruhi.botserver.features.ai.model.admin.AiRoleView;
import com.haruhi.botserver.features.ai.model.admin.AiSessionDetailResp;
import com.haruhi.botserver.features.ai.model.admin.AiSessionQueryReq;
import com.haruhi.botserver.features.ai.model.admin.AiSessionView;
import com.haruhi.botserver.features.ai.model.admin.AiUsageBreakdownResp;
import com.haruhi.botserver.features.ai.model.admin.AiUsageTrendResp;
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
import com.haruhi.botserver.features.ai.support.prompt.PromptAssembler;
import com.haruhi.botserver.features.ai.support.prompt.RoleCardService;
import com.haruhi.botserver.shared.error.BusinessException;
import com.haruhi.botserver.shared.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * WebUI AI 管理查询与操作。
 */
@Service
@Slf4j
public class AiAdminService {

    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "^【(安全与权限|输出协议|可用工具|可用表情|当前场景|角色卡)[^】]*】",
            Pattern.MULTILINE);

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;
    private final AiUsageMapper usageMapper;
    private final AiMemoryMapper memoryMapper;
    private final RoleCardService roleCardService;
    private final EmojiService emojiService;
    private final DeepSeekClient deepSeekClient;
    private final AiSessionService sessionService;
    private final PromptAssembler promptAssembler;

    public AiAdminService(AiSessionMapper sessionMapper,
                          AiMessageMapper messageMapper,
                          AiUsageMapper usageMapper,
                          AiMemoryMapper memoryMapper,
                          RoleCardService roleCardService,
                          EmojiService emojiService,
                          DeepSeekClient deepSeekClient,
                          AiSessionService sessionService,
                          PromptAssembler promptAssembler) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.usageMapper = usageMapper;
        this.memoryMapper = memoryMapper;
        this.roleCardService = roleCardService;
        this.emojiService = emojiService;
        this.deepSeekClient = deepSeekClient;
        this.sessionService = sessionService;
        this.promptAssembler = promptAssembler;
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

    public AiRoleDetailResp roleDetail(String name) {
        return toDetail(roleCardService.definition(name));
    }

    public AiRoleSaveResp roleSave(AiRoleSaveReq request) {
        List<RoleCardService.RoleSection> sections = request.getSections() == null
                ? List.of()
                : request.getSections().stream()
                .filter(Objects::nonNull)
                .map(section -> new RoleCardService.RoleSection(
                        section.getTitle(), section.getMode(), section.getLines()))
                .toList();
        RoleCardService.RoleSaveResult result = roleCardService.save(new RoleCardService.RoleSaveCommand(
                request.getOriginalName(), request.getName(), request.getTriggers(), sections,
                request.getBaseMtime(), request.getBaseHash(), request.isDryRun()));

        AiRoleSaveResp resp = new AiRoleSaveResp();
        resp.setSaved(result.saved());
        resp.setDetail(toDetail(result.definition()));
        resp.setRenderedMarkdown(result.renderedMarkdown());
        resp.setFilteredContent(result.filteredContent());
        resp.setWarnings(new ArrayList<>(result.warnings()));
        resp.setEstimatedTokens(result.estimatedTokens());
        return resp;
    }

    public AiRoleDetailResp roleCopy(AiRoleCopyReq request) {
        return toDetail(roleCardService.copy(request.getSourceName(), request.getTargetName(), request.isOverwrite()));
    }

    public AiRoleNameCheckResp roleNameCheck(AiRoleNameCheckReq request) {
        AiRoleNameCheckResp resp = new AiRoleNameCheckResp();
        if (StringUtils.isNotBlank(request.getSourceName())) {
            String suggested = roleCardService.suggestCopyName(request.getSourceName());
            resp.setSuggested(suggested);
            resp.setAvailable(roleCardService.nameAvailable(suggested, null));
            resp.setMessage(resp.isAvailable() ? "" : "建议名不可用，请手动修改");
            return resp;
        }
        boolean available = roleCardService.nameAvailable(request.getName(), request.getOriginalName());
        resp.setAvailable(available);
        resp.setMessage(available ? "" : "角色名不可用或已存在");
        return resp;
    }

    private AiRoleDetailResp toDetail(RoleCardService.RoleDefinition definition) {
        if (definition == null) {
            return null;
        }
        AiRoleDetailResp resp = new AiRoleDetailResp();
        resp.setName(definition.name());
        resp.setFileName(definition.fileName());
        resp.setTriggers(new ArrayList<>(definition.triggers()));
        resp.setChars(definition.chars());
        resp.setMaxChars(definition.maxChars());
        resp.setMtime(definition.mtime());
        resp.setHash(definition.hash());
        resp.setWarnings(new ArrayList<>(definition.warnings()));
        for (RoleCardService.RoleSection section : definition.sections()) {
            AiRoleSection item = new AiRoleSection();
            item.setTitle(section.title());
            item.setMode(section.mode());
            item.setLines(new ArrayList<>(section.lines()));
            resp.getSections().add(item);
        }
        return resp;
    }

    // ==================== P1：会话操作 ====================

    public AiSessionDetailResp updateStatus(Long id, String status) {
        String normalized = StringUtils.trimToEmpty(status).toLowerCase(Locale.ROOT);
        if (!"active".equals(normalized) && !"paused".equals(normalized)) {
            throw new BusinessException("状态只能是 active 或 paused");
        }
        AiSessionEntity entity = requireSession(id);
        AiSessionState state = sessionService.load(sessionKey(entity));
        sessionService.updateStatus(state, normalized);
        return detail(id);
    }

    public AiSessionDetailResp updateRole(Long id, String roleName) {
        String name = StringUtils.trimToNull(roleName);
        RoleCardService.RoleCard card = null;
        if (name != null) {
            card = roleCardService.load(name);
            if (card == null) {
                throw new BusinessException("角色不存在：" + name);
            }
        }
        AiSessionEntity entity = requireSession(id);
        AiSessionState state = sessionService.load(sessionKey(entity));
        sessionService.updateRole(state, card == null ? null : card.name());
        return detail(id);
    }

    public AiSessionDetailResp resetSession(Long id) {
        AiSessionEntity entity = requireSession(id);
        AiSessionState state = sessionService.load(sessionKey(entity));
        sessionService.reset(state);
        return detail(id);
    }

    private AiSessionEntity requireSession(Long id) {
        if (id == null) {
            throw new BusinessException("缺少会话 id");
        }
        AiSessionEntity entity = sessionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("会话不存在：" + id);
        }
        return entity;
    }

    private AiSessionKey sessionKey(AiSessionEntity entity) {
        AiScene scene = AiScene.of(entity.getScene());
        return new AiSessionKey(entity.getSelfId(), scene == null ? AiScene.GROUP : scene, entity.getTargetId());
    }

    // ==================== P1：用量趋势与分布 ====================

    public AiUsageTrendResp trend(Integer days) {
        int range = normalizeDays(days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        QueryWrapper<AiUsageEntity> wrapper = new QueryWrapper<>();
        wrapper.select("substr(created_at,1,10) AS day",
                        "COUNT(*) AS requests",
                        "COALESCE(SUM(prompt_tokens),0) AS prompt_tokens",
                        "COALESCE(SUM(completion_tokens),0) AS completion_tokens",
                        "COALESCE(SUM(total_tokens),0) AS total_tokens",
                        "COALESCE(SUM(cache_hit_tokens),0) AS cache_hit_tokens",
                        "COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END),0) AS failures",
                        "COALESCE(AVG(latency_ms),0) AS avg_latency_ms")
                .ge("created_at", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00")
                .last("GROUP BY substr(created_at,1,10) ORDER BY day");

        Map<String, AiUsageTrendResp.Point> byDate = new HashMap<>();
        for (Map<String, Object> row : usageMapper.selectMaps(wrapper)) {
            AiUsageTrendResp.Point point = new AiUsageTrendResp.Point();
            point.setDate(mapString(row, "day"));
            point.setRequests(mapLong(row, "requests"));
            point.setPromptTokens(mapLong(row, "prompt_tokens"));
            point.setCompletionTokens(mapLong(row, "completion_tokens"));
            point.setTotalTokens(mapLong(row, "total_tokens"));
            point.setCacheHitTokens(mapLong(row, "cache_hit_tokens"));
            point.setFailures(mapLong(row, "failures"));
            point.setAvgLatencyMs(mapDouble(row, "avg_latency_ms"));
            if (point.getDate() != null) {
                byDate.put(point.getDate(), point);
            }
        }

        AiUsageTrendResp resp = new AiUsageTrendResp();
        for (int i = 0; i < range; i++) {
            String date = startDate.plusDays(i).format(DateTimeFormatter.ISO_LOCAL_DATE);
            AiUsageTrendResp.Point point = byDate.get(date);
            if (point == null) {
                point = new AiUsageTrendResp.Point();
                point.setDate(date);
            }
            resp.getPoints().add(point);
        }
        return resp;
    }

    public AiUsageBreakdownResp breakdown(Integer days) {
        int range = normalizeDays(days);
        String start = startOfDaysAgo(range - 1);
        AiUsageBreakdownResp resp = new AiUsageBreakdownResp();
        resp.setByScene(usageMapper.aggregateByScene(start).stream()
                .map(row -> toItem(row, mapString(row, "scene"), sceneLabel(mapString(row, "scene"))))
                .toList());
        resp.setByGroup(usageMapper.aggregateByGroup(start).stream()
                .map(row -> {
                    String groupId = mapString(row, "target_id");
                    return toItem(row, groupId, StringUtils.isBlank(groupId) ? "未知群" : "群 " + groupId);
                })
                .toList());
        resp.setByPrivate(usageMapper.aggregateByPrivate(start).stream()
                .map(row -> {
                    String userId = mapString(row, "target_id");
                    return toItem(row, userId, StringUtils.isBlank(userId) ? "未知用户" : "用户 " + userId);
                })
                .toList());

        QueryWrapper<AiUsageEntity> wrapper = new QueryWrapper<>();
        wrapper.select("model",
                        "COUNT(*) AS requests",
                        "COALESCE(SUM(total_tokens),0) AS tokens",
                        "COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END),0) AS failures")
                .ge("created_at", start)
                .groupBy("model")
                .orderByDesc("tokens");
        resp.setByModel(usageMapper.selectMaps(wrapper).stream()
                .map(row -> {
                    String model = mapString(row, "model");
                    return toItem(row, model, StringUtils.defaultIfBlank(model, "未知模型"));
                })
                .toList());
        return resp;
    }

    private AiUsageBreakdownResp.Item toItem(Map<String, Object> row, String key, String label) {
        AiUsageBreakdownResp.Item item = new AiUsageBreakdownResp.Item();
        item.setKey(key);
        item.setLabel(label);
        item.setRequests(mapLong(row, "requests"));
        item.setTokens(mapLong(row, "tokens"));
        item.setFailures(mapLong(row, "failures"));
        return item;
    }

    private String sceneLabel(String scene) {
        return "private".equalsIgnoreCase(scene) ? "私聊" : "群聊";
    }

    // ==================== P1：提示词预览 ====================

    public AiPromptPreviewResp promptPreview(AiPromptPreviewReq request) {
        if (request == null || StringUtils.isBlank(request.getText())) {
            throw new BusinessException("模拟消息不能为空");
        }
        AiScene scene = AiScene.of(request.getScene());
        if (scene == null) {
            scene = AiScene.GROUP;
        }
        long selfId = request.getSelfId() == null ? 0L : request.getSelfId();
        long targetId = request.getTargetId() == null ? 0L : request.getTargetId();
        AiSessionKey key = new AiSessionKey(selfId, scene, targetId);
        AiSessionState state = new AiSessionState(key);
        AiSessionEntity entity = new AiSessionEntity();
        entity.setSelfId(selfId);
        entity.setScene(scene.getCode());
        entity.setTargetId(targetId);
        entity.setRoleName(StringUtils.trimToNull(request.getRoleName()));
        entity.setStatus("active");
        state.setEntity(entity);

        AiIncoming incoming = new AiIncoming();
        incoming.setKey(key);
        incoming.setUserId(request.getUserId() == null ? 10001L : request.getUserId());
        incoming.setUserName(StringUtils.defaultIfBlank(request.getUserName(), "预览用户"));
        incoming.setText(request.getText());
        incoming.setRawMessage(request.getText());
        incoming.setMessageId("preview");
        incoming.setTimestamp(System.currentTimeMillis());

        AiTurn turn = new AiTurn();
        turn.setIncoming(incoming);
        turn.setDirect(true);
        turn.setNotBefore(System.currentTimeMillis());
        turn.setDeadline(System.currentTimeMillis() + 1000L);

        AiPrompt prompt = promptAssembler.assemble(state, List.of(turn), parseAmbient(request.getAmbientText()));
        AiPromptPreviewResp resp = new AiPromptPreviewResp();
        resp.setSystem(prompt.getSystem());
        resp.setSections(splitSections(prompt.getSystem()));
        for (DeepSeekMessage message : prompt.getMessages()) {
            AiPromptPreviewResp.Message item = new AiPromptPreviewResp.Message();
            item.setRole(message.getRole());
            item.setContent(message.getContent());
            item.setName(message.getName());
            item.setImageUrls(message.getImageUrls());
            resp.getMessages().add(item);
        }
        resp.setEstimatedTokens(prompt.getEstimatedTokens());
        resp.setBudget(AiChatSettings.historyMaxInputTokens());
        resp.setVisionEnabled(AiChatSettings.visionEnabled());
        return resp;
    }

    private List<AiAmbientMessage> parseAmbient(String text) {
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        List<AiAmbientMessage> result = new ArrayList<>();
        long timestamp = System.currentTimeMillis();
        int index = 0;
        for (String line : text.split("\\r?\\n")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            String content = line.trim();
            String speaker = "群友";
            int ascii = content.indexOf(':');
            int chinese = content.indexOf('：');
            int separator = ascii >= 0 && (chinese < 0 || ascii < chinese) ? ascii : chinese;
            if (separator > 0) {
                speaker = content.substring(0, separator).trim();
                content = content.substring(separator + 1).trim();
            }
            if (StringUtils.isNotBlank(content)) {
                result.add(new AiAmbientMessage("preview-" + index++, 0L, speaker, content, timestamp));
            }
        }
        return result;
    }

    private List<AiPromptPreviewResp.Section> splitSections(String system) {
        List<AiPromptPreviewResp.Section> sections = new ArrayList<>();
        if (StringUtils.isBlank(system)) {
            return sections;
        }
        Matcher matcher = SECTION_PATTERN.matcher(system);
        List<Integer> starts = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
            titles.add(matcher.group(1));
        }
        if (starts.isEmpty()) {
            sections.add(section("System", system));
            return sections;
        }
        if (starts.get(0) > 0) {
            sections.add(section("前言", system.substring(0, starts.get(0))));
        }
        for (int i = 0; i < starts.size(); i++) {
            int end = i + 1 < starts.size() ? starts.get(i + 1) : system.length();
            sections.add(section(titles.get(i), system.substring(starts.get(i), end)));
        }
        return sections;
    }

    private AiPromptPreviewResp.Section section(String title, String content) {
        AiPromptPreviewResp.Section section = new AiPromptPreviewResp.Section();
        section.setTitle(title);
        section.setContent(content == null ? "" : content.trim());
        return section;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return 7;
        }
        return Math.min(days, 90);
    }

    private String startOfDaysAgo(int daysAgo) {
        return LocalDate.now().minusDays(daysAgo).format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00";
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
