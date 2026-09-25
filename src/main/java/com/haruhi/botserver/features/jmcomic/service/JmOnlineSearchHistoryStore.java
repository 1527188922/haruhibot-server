package com.haruhi.botserver.features.jmcomic.service;

import com.alibaba.fastjson.JSONObject;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.jmcomic.model.JmOnlineSearchHistory;
import com.haruhi.botserver.features.jmcomic.model.JmSearchSortEnum;
import com.haruhi.botserver.infrastructure.kvstore.persistence.entity.KvEntry;
import com.haruhi.botserver.infrastructure.kvstore.service.KvStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * JM在线搜索历史的存取
 * <p>
 * 一条搜索历史 = t_dictionary 里的一行，key 为 {@value #KEY_SEARCH_HISTORY}，content 为一条记录的json。
 * 保存条数由 {@link ConfigKey#JM_SEARCH_HISTORY_LIMIT} 控制，超出后删除最旧的(自增id最小的)。
 * <p>
 * 历史只是搜索的附加能力：写失败只记日志，不影响搜索本身。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JmOnlineSearchHistoryStore {

    public static final String KEY_SEARCH_HISTORY = "jm.online_search.history";

    private final KvStoreService kvStoreService;

    /**
     * 保存一条搜索历史；
     * 配置的条数小于等于0表示不保存；
     * 与最新一条的条件(关键字+排序+页码)完全相同时只刷新时间，避免同条件反复搜索刷满历史
     */
    public void save(JmOnlineSearchHistory history) {
        if (history == null || StringUtils.isBlank(history.getName())) {
            return;
        }
        int limit = historyLimit();
        if (limit <= 0) {
            return;
        }
        try {
            String content = JSONObject.toJSONString(history);
            KvEntry latest = latestEntry();
            JmOnlineSearchHistory latestHistory = latest == null ? null : parse(latest.getContent());
            if (latestHistory != null && sameCondition(latestHistory, history)) {
                KvEntry update = new KvEntry();
                update.setId(latest.getId());
                update.setKey(KEY_SEARCH_HISTORY);
                update.setContent(content);
                kvStoreService.update(update);
            } else {
                kvStoreService.add(KEY_SEARCH_HISTORY, content);
            }
            trim(limit);
        } catch (Exception e) {
            log.error("保存JM在线搜索历史失败", e);
        }
    }

    /**
     * 按时间倒序返回历史记录(不含结果快照)，条数不超过配置值；已禁用历史时返回空
     */
    public List<JmOnlineSearchHistory> list() {
        int limit = historyLimit();
        if (limit <= 0) {
            return List.of();
        }
        try {
            List<KvEntry> entries = kvStoreService.getList(KEY_SEARCH_HISTORY);
            if (CollectionUtils.isEmpty(entries)) {
                return List.of();
            }
            return entries.stream()
                    // id自增，越大越新
                    .sorted(Comparator.comparing(KvEntry::getId).reversed())
                    .limit(limit)
                    .map(e -> fill(parse(e.getContent()), e))
                    .filter(Objects::nonNull)
                    // 列表只需要条件与概要，结果快照只在查详情时下发
                    .peek(e -> e.setItems(null))
                    .toList();
        } catch (Exception e) {
            log.error("读取JM在线搜索历史失败", e);
            return List.of();
        }
    }

    /**
     * 查询单条历史详情(含结果快照)，不存在返回null
     */
    public JmOnlineSearchHistory get(Long id) {
        if (id == null) {
            return null;
        }
        try {
            KvEntry entry = kvStoreService.getById(id);
            // 防止传入其他kv记录的id
            if (entry == null || !KEY_SEARCH_HISTORY.equals(entry.getKey())) {
                return null;
            }
            return fill(parse(entry.getContent()), entry);
        } catch (Exception e) {
            log.error("查询JM在线搜索历史详情失败 id:{}", id, e);
            return null;
        }
    }

    public void delete(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<KvEntry> entries = new ArrayList<>(ids.size());
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            KvEntry entry = new KvEntry();
            entry.setId(id);
            entries.add(entry);
        }
        kvStoreService.deleteBatch(entries);
    }

    public void clear() {
        kvStoreService.remove(KEY_SEARCH_HISTORY);
    }

    /**
     * 配置的历史保存条数
     */
    public int historyLimit() {
        return Configs.getInt(ConfigKey.JM_SEARCH_HISTORY_LIMIT, 20);
    }

    private KvEntry latestEntry() {
        List<KvEntry> entries = kvStoreService.getList(KEY_SEARCH_HISTORY);
        if (CollectionUtils.isEmpty(entries)) {
            return null;
        }
        return entries.stream().max(Comparator.comparing(KvEntry::getId)).orElse(null);
    }

    /**
     * 只保留最新的limit条
     */
    private void trim(int limit) {
        List<KvEntry> entries = kvStoreService.getList(KEY_SEARCH_HISTORY);
        if (CollectionUtils.isEmpty(entries) || entries.size() <= limit) {
            return;
        }
        List<KvEntry> overflow = entries.stream()
                .sorted(Comparator.comparing(KvEntry::getId))
                .limit(entries.size() - limit)
                .toList();
        kvStoreService.deleteBatch(overflow);
    }

    private boolean sameCondition(JmOnlineSearchHistory left, JmOnlineSearchHistory right) {
        return Objects.equals(left.getName(), right.getName())
                && Objects.equals(left.getSort(), right.getSort())
                && Objects.equals(left.getPage(), right.getPage());
    }

    /**
     * 补齐只在下发时才有值的字段
     */
    private JmOnlineSearchHistory fill(JmOnlineSearchHistory history, KvEntry entry) {
        if (history == null || entry == null) {
            return null;
        }
        history.setId(entry.getId());
        // 同条件重复搜索时刷新的是modifyTime
        history.setSearchTime(StringUtils.defaultIfBlank(entry.getModifyTime(), entry.getCreateTime()));
        history.setSortLabel(JmSearchSortEnum.getOrDefault(history.getSort()).getRemark());
        return history;
    }

    private JmOnlineSearchHistory parse(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            return JSONObject.parseObject(content, JmOnlineSearchHistory.class);
        } catch (Exception e) {
            log.warn("解析JM在线搜索历史失败 content:{}", content, e);
            return null;
        }
    }
}
