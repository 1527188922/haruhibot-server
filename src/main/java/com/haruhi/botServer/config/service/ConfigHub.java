package com.haruhi.botServer.config.service;

import com.haruhi.botServer.config.config.ConfigFile;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.util.PropertiesFileUtil;
import com.haruhi.botServer.config.util.YamlFileUtil;
import com.haruhi.botServer.config.vo.ConfigFileNode;
import com.haruhi.botServer.config.vo.ConfigItem;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置中心
 * <p>
 * 职责：
 * <ol>
 *     <li><b>写</b>：把配置写入 ./config/{file}.properties（保留注释与顺序）</li>
 *     <li><b>读</b>：写入后刷新 {@link Configs} 内存快照，使配置立刻可读</li>
 *     <li><b>通知</b>：按配置项精确通知 {@link ConfigApplier}，实现"改哪个生效哪个"的粒度</li>
 *     <li><b>监听</b>：外部直接改文件后自动感知并刷新（无需重启、无需点按钮）</li>
 * </ol>
 */
@Slf4j
@Service
public class ConfigHub {

    /** 前端展示脱敏后保留的明文后缀长度 */
    private static final int SECRET_VISIBLE_LENGTH = 4;

    @Autowired
    private ApplicationContext applicationContext;

    /** 文件最后修改时间，用于感知外部改动 */
    private final Map<String, Long> fileLastModified = new ConcurrentHashMap<>();

    /** 最近一次刷新/校验的错误，前端展示 */
    private final Map<String, String> fileError = new ConcurrentHashMap<>();

    /** fileName -> 配置项（启动与文件变更时重建） */
    private volatile Map<String, List<ConfigItem>> fileItems = new LinkedHashMap<>();

    // ==================================================================
    // 查询
    // ==================================================================

    /**
     * 全部配置，按文件分组
     */
    public List<ConfigFileNode> list() {
        reloadItemsIfEmpty();
        ConfigFile[] files = ConfigFile.inLoadOrder();
        List<ConfigFileNode> nodes = new ArrayList<>(files.length);
        for (ConfigFile file : files) {
            nodes.add(buildNode(file));
        }
        return nodes;
    }

    public ConfigFileNode list(ConfigFile file) {
        return buildNode(file);
    }

    private ConfigFileNode buildNode(ConfigFile file) {
        File disk = fileOf(file);
        List<ConfigItem> items = itemMap().getOrDefault(file.getFileName(), Collections.emptyList());

        ConfigFileNode node = new ConfigFileNode();
        node.setFileName(file.getFileName());
        node.setDisplayName(file.getDisplayName());
        node.setRemark(file.getRemark());
        node.setPath(disk.getAbsolutePath());
        node.setExists(disk.isFile());
        node.setLastModified(disk.isFile() ? disk.lastModified() : 0L);
        node.setCount(items.size());
        node.setHotCount((int) items.stream().filter(ConfigItem::isHot).count());
        node.setError(fileError.get(file.getFileName()));
        node.setItems(items);
        return node;
    }

    // ==================================================================
    // 写入
    // ==================================================================

    /**
     * 保存单个配置项：落盘 + 刷新快照 + 命中热更新则通知订阅者
     *
     * @return 是否触发了热更新通知
     */
    public boolean save(ConfigKey key, String value) {
        return saveAll(Map.of(key, value)).getOrDefault(key, false);
    }

    /**
     * 批量保存（同一文件或多文件），最后统一刷新快照与通知，避免重复重建快照
     *
     * @return key -> 是否触发了热更新通知
     */
    public Map<ConfigKey, Boolean> saveAll(Map<ConfigKey, String> values) {
        Map<ConfigKey, Boolean> result = new LinkedHashMap<>();
        if (values == null || values.isEmpty()) {
            return result;
        }

        Map<ConfigKey, String> oldValues = new LinkedHashMap<>();
        Map<ConfigKey, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<ConfigKey, String> entry : values.entrySet()) {
            ConfigKey key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue();
            String error = key.getType().validate(value);
            if (error != null) {
                throw new BusinessException(StringUtils.defaultIfBlank(key.getRemark(), key.getKey()) + "：" + error);
            }
            oldValues.put(key, Configs.getRaw(key));
            normalized.put(key, value);
        }

        // 1. 落盘（properties 与 yml 都按行原地改值，保留注释与顺序）
        Set<ConfigFile> touched = new LinkedHashSet<>();
        for (Map.Entry<ConfigKey, String> entry : normalized.entrySet()) {
            ConfigKey key = entry.getKey();
            try {
                Configs.writeToFile(key, entry.getValue());
                touched.add(key.getFile());
                fileError.remove(key.getFile().getFileName());
            } catch (IOException e) {
                log.error("保存配置失败 key:{}", key.getKey(), e);
                throw new BusinessException("保存配置失败：" + e.getMessage());
            }
        }

        // 2. 刷新内存快照
        for (ConfigFile file : touched) {
            Configs.reloadFile(file);
            markModified(file);
        }

        // 3. 逐项通知（仅 hot 配置需要通知；非hot配置重启后才生效）
        List<ConfigChange> changes = new ArrayList<>();
        for (Map.Entry<ConfigKey, String> entry : normalized.entrySet()) {
            ConfigKey key = entry.getKey();
            String oldValue = oldValues.get(key);
            String newValue = Configs.getRaw(key);
            boolean hot = key.isHot();
            if (hot && !Objects.equals(oldValue, newValue)) {
                changes.add(new ConfigChange(key, oldValue, newValue));
            }
            result.put(key, hot && !Objects.equals(oldValue, newValue));
        }
        fire(changes);

        // 4. 重建配置项缓存
        rebuildItems();
        return result;
    }

    /**
     * 重置为声明中的默认值
     * <p>
     * 等价于把该key从配置文件里删掉（配置文件是唯一真源，删掉即回落到声明默认值），
     * 同时刷新快照并按需通知订阅者
     */
    public boolean reset(ConfigKey key) {
        boolean applied = save(key, key.getDefaultValue());
        try {
            if (key.getFile().isYaml()) {
                // yml 里如果该key是显式写的，删掉这一行；删不掉（键不在文件里）也无所谓
                YamlFileUtil.remove(Configs.fileOf(key), key.getKey());
            } else {
                PropertiesFileUtil.remove(key.getFile().getFileName(), key.getKey());
            }
        } catch (IOException e) {
            log.error("重置配置失败 key:{}", key.getKey(), e);
            throw new BusinessException("重置配置失败：" + e.getMessage());
        }
        Configs.reloadFile(key.getFile());
        markModified(key.getFile());
        rebuildItems();
        return applied;
    }

    // ==================================================================
    // 刷新
    // ==================================================================

    /**
     * 单key刷新：重新读取该key所在的配置文件
     * <p>
     * 与"保存"的区别是不写文件，用于外部直接改了文件、或用户手动点某个key的刷新按钮
     *
     * @return 变更列表（未发生变化时为空）
     */
    public List<ConfigChange> refresh(ConfigKey key) {
        return refreshFile(key.getFile(), Set.of(key.getKey()));
    }

    /**
     * 文件级刷新：重新读取整个配置文件
     *
     * @return 变更列表
     */
    public List<ConfigChange> refreshFile(ConfigFile file) {
        return refreshFile(file, ConfigKey.of(file).stream().map(ConfigKey::getKey).collect(java.util.stream.Collectors.toSet()));
    }

    private List<ConfigChange> refreshFile(ConfigFile file, Set<String> filter) {
        Map<String, String> before = Configs.snapshot();
        Configs.reloadFile(file);
        markModified(file);
        fileError.remove(file.getFileName());

        List<ConfigChange> changes = new ArrayList<>();
        for (ConfigKey key : ConfigKey.of(file)) {
            if (!filter.contains(key.getKey())) {
                continue;
            }
            String oldValue = before.get(key.getKey());
            String newValue = Configs.getRaw(key);
            String oldFinal = oldValue == null ? key.getDefaultValue() : oldValue;
            String newFinal = newValue == null ? key.getDefaultValue() : newValue;
            if (!Objects.equals(oldFinal, newFinal)) {
                changes.add(new ConfigChange(key, oldFinal, newFinal));
            }
        }
        fire(changes.stream().filter(e -> e.key().isHot()).toList());
        rebuildItems();
        return changes;
    }

    /**
     * 全量刷新（含非hot项，用于兜底）
     */
    public void refreshAll() {
        Configs.reloadAll();
        markAllModified();
        fireAll();
        rebuildItems();
    }

    // ==================================================================
    // 外部文件变更监听
    // ==================================================================

    /**
     * 每2秒检查一次配置文件是否被外部修改（直接改文件、其他进程写入等）
     * <p>
     * 这是"用编辑器改配置也能立刻生效"的关键：检测到变更后自动刷新快照并通知订阅者
     */
    @Scheduled(fixedDelay = 2000L, initialDelay = 5000L)
    public void watchFiles() {
        for (ConfigFile file : ConfigFile.values()) {
            File disk = fileOf(file);
            if (!disk.isFile()) {
                continue;
            }
            long modified = disk.lastModified();
            Long last = fileLastModified.get(file.getFileName());
            if (last == null) {
                fileLastModified.put(file.getFileName(), modified);
                continue;
            }
            if (last == modified) {
                continue;
            }
            log.info("检测到配置文件变更，自动重载：{}", disk.getName());
            try {
                refreshFile(file);
            } catch (Exception e) {
                fileError.put(file.getFileName(), e.getMessage());
                log.error("自动重载配置文件异常 {}", disk.getName(), e);
            }
        }
    }

    // ==================================================================
    // 通知
    // ==================================================================

    /**
     * 按key通知订阅者
     */
    private void fire(Collection<ConfigChange> changes) {
        if (changes.isEmpty()) {
            return;
        }
        Collection<ConfigApplier> appliers;
        try {
            appliers = applicationContext.getBeansOfType(ConfigApplier.class).values();
        } catch (Exception e) {
            log.error("获取配置订阅者异常", e);
            return;
        }
        for (ConfigChange change : changes) {
            for (ConfigApplier applier : appliers) {
                try {
                    Collection<ConfigKey> keys = applier.keys();
                    if (keys == null || !keys.contains(change.key())) {
                        continue;
                    }
                    if (applier.onlyOnChanged() && !change.changed()) {
                        continue;
                    }
                    applier.onConfigChange(change);
                } catch (Exception e) {
                    log.error("配置变更处理异常 key:{} applier:{}", change.key().getKey(), applier.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * 全量通知（用于 refreshAll，会给订阅者发出它关心的每个key的"变更"）
     */
    private void fireAll() {
        List<ConfigChange> changes = new ArrayList<>();
        for (ConfigKey key : ConfigKey.values()) {
            changes.add(new ConfigChange(key, Configs.getRaw(key), Configs.getRaw(key)));
        }
        Collection<ConfigApplier> appliers;
        try {
            appliers = applicationContext.getBeansOfType(ConfigApplier.class).values();
        } catch (Exception e) {
            log.error("获取配置订阅者异常", e);
            return;
        }
        for (ConfigApplier applier : appliers) {
            try {
                Collection<ConfigKey> keys = applier.keys();
                if (keys == null) {
                    continue;
                }
                for (ConfigChange change : changes) {
                    if (keys.contains(change.key())) {
                        applier.onConfigChange(change);
                    }
                }
            } catch (Exception e) {
                log.error("配置全量刷新异常 applier:{}", applier.getClass().getSimpleName(), e);
            }
        }
    }

    // ==================================================================
    // 内部
    // ==================================================================

    private void markModified(ConfigFile file) {
        File disk = fileOf(file);
        fileLastModified.put(file.getFileName(), disk.isFile() ? disk.lastModified() : 0L);
    }

    private void markAllModified() {
        for (ConfigFile file : ConfigFile.values()) {
            markModified(file);
        }
    }

    private File fileOf(ConfigFile file) {
        return Configs.fileOf(file);
    }

    private Map<String, List<ConfigItem>> itemMap() {
        Map<String, List<ConfigItem>> map = fileItems;
        if (map.isEmpty()) {
            rebuildItems();
            map = fileItems;
        }
        return map;
    }

    private void reloadItemsIfEmpty() {
        if (fileItems.isEmpty()) {
            rebuildItems();
        }
    }

    /**
     * 重建前端展示用的配置项，按 文件 -> sort 排序
     */
    private synchronized void rebuildItems() {
        Map<String, List<ConfigItem>> map = new LinkedHashMap<>();
        for (ConfigFile file : ConfigFile.inLoadOrder()) {
            List<ConfigKey> keys = new ArrayList<>(ConfigKey.of(file));
            keys.sort((a, b) -> Integer.compare(a.getSort(), b.getSort()));
            List<ConfigItem> items = new ArrayList<>(keys.size());
            for (ConfigKey key : keys) {
                items.add(toItem(key));
            }
            map.put(file.getFileName(), Collections.unmodifiableList(items));
        }
        fileItems = Collections.unmodifiableMap(map);
    }

    private ConfigItem toItem(ConfigKey key) {
        ConfigItem item = new ConfigItem();
        item.setKey(key.getKey());
        item.setName(key.name());
        item.setDisplayName(displayName(key));
        item.setType(key.getType());
        // 敏感配置不下发明文：前端展示掩码，用户输入新值才会覆盖
        boolean secret = key.getType() == com.haruhi.botServer.config.config.ConfigType.SECRET;
        String raw = Configs.getRaw(key);
        item.setValue(secret ? null : raw);
        item.setHasValue(StringUtils.isNotBlank(raw));
        item.setMaskedValue(secret ? mask(raw) : null);
        item.setDefaultValue(secret ? mask(key.getDefaultValue()) : key.getDefaultValue());
        item.setHot(key.isHot());
        item.setConfigured(Configs.isConfigured(key));
        item.setSource(Configs.source(key).name());
        item.setRemark(key.getRemark());
        item.setFileName(key.getFile().getFileName());
        item.setSort(key.getSort());
        return item;
    }

    /**
     * 敏感值脱敏：保留尾部若干字符
     */
    public static String mask(String value) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        if (value.length() <= SECRET_VISIBLE_LENGTH) {
            return "****";
        }
        return "****" + value.substring(value.length() - SECRET_VISIBLE_LENGTH);
    }

    /**
     * 从key推导中文名，避免每个配置项都要手写
     * <p>
     * 优先匹配"去掉第一段后的完整后缀"（如 {@code agefans.url}），
     * 其次按最长前缀分组（如 {@code bot.ws}），最后用最后一段兜底
     */
    private static String displayName(ConfigKey key) {
        String full = key.getKey();
        int firstDot = full.indexOf('.');
        if (firstDot > 0) {
            String suffix = full.substring(firstDot + 1);
            String word = WORD_MAP.get(suffix);
            if (word != null) {
                return word;
            }
        }
        for (String prefix : PREFIXES_BY_LENGTH_DESC) {
            if (full.startsWith(prefix + ".")) {
                String tail = full.substring(prefix.length() + 1);
                String tailName = WORD_MAP.getOrDefault(tail, tail);
                return PREFIX_MAP.get(prefix) + tailName;
            }
        }
        int lastDot = full.lastIndexOf('.');
        String tail = lastDot < 0 ? full : full.substring(lastDot + 1);
        return WORD_MAP.getOrDefault(tail, tail);
    }

    private static final Map<String, String> WORD_MAP = new HashMap<>();
    private static final Map<String, String> PREFIX_MAP = new HashMap<>();
    /** 前缀按长度降序，保证最长前缀优先匹配 */
    private static final List<String> PREFIXES_BY_LENGTH_DESC = new ArrayList<>();

    static {
        WORD_MAP.put("username", "账号");
        WORD_MAP.put("password", "密码");
        WORD_MAP.put("max", "最大数量");
        WORD_MAP.put("expire", "过期时长");
        WORD_MAP.put("secret", "密钥");
        WORD_MAP.put("grace", "宽限时长");
        WORD_MAP.put("enabled", "开关");
        WORD_MAP.put("sessdata", "SESSDATA");
        WORD_MAP.put("bili_jct", "bili_jct");
        WORD_MAP.put("ticket", "ticket");
        WORD_MAP.put("duration_limit", "时长限制");
        WORD_MAP.put("apikey", "API KEY");
        WORD_MAP.put("baseurl", "接口地址");
        WORD_MAP.put("timeout", "超时时长");
        WORD_MAP.put("zip", "zip解压密码");
        WORD_MAP.put("pdf", "pdf密码");
        WORD_MAP.put("threads", "下载线程数");
        WORD_MAP.put("name_max_length", "名称最大长度");
        WORD_MAP.put("api_domain", "API域名");
        WORD_MAP.put("image_mode", "结果图片模式");
        WORD_MAP.put("limit", "条数上限");
        WORD_MAP.put("enable", "开启");
        WORD_MAP.put("cron", "cron表达式");
        WORD_MAP.put("port", "http端口");
        WORD_MAP.put("access_token", "连接认证token");
        WORD_MAP.put("superusers", "超级管理员");
        WORD_MAP.put("max_connections", "最大连接数");
        WORD_MAP.put("access_groups", "可访问群");
        WORD_MAP.put("parallel", "并发发送");
        WORD_MAP.put("same-machine-qqclient", "qq客户端同机部署");
        WORD_MAP.put("internet-host", "对外ip或域名");
        WORD_MAP.put("disable_group", "禁用所有群功能");
        WORD_MAP.put("qingyunke_chat", "青云可聊天");
        WORD_MAP.put("search_image_allow_group", "群聊识图");
        WORD_MAP.put("search_bt_allow_group", "群聊bt搜索");
        WORD_MAP.put("group_increase", "加群提示");
        WORD_MAP.put("group_decrease", "离群提示");
        WORD_MAP.put("raw_compress", "聊天记录压缩存储");
        WORD_MAP.put("bt_search", "磁力搜索站点地址");
        WORD_MAP.put("btbtla_search", "bt影视搜索站点地址");
        WORD_MAP.put("parallel.enabled", "允许多JM并发");
        WORD_MAP.put("saucenao.baseurl", "SauceNao识图接口地址");
        WORD_MAP.put("saucenao.apikey", "SauceNao识图API KEY");
        WORD_MAP.put("agefans.url", "agefans站点地址");

        PREFIX_MAP.put("login", "登录");
        PREFIX_MAP.put("druid", "Druid");
        PREFIX_MAP.put("druid.monitor.url", "Druid URL监控");
        PREFIX_MAP.put("druid.monitor.spring", "Druid Spring监控");
        PREFIX_MAP.put("job.downloadPixiv", "Pixiv下载任务");
        PREFIX_MAP.put("job.bilibiliLive", "B站直播推送任务");
        PREFIX_MAP.put("bilibili.cookies", "B站cookie");
        PREFIX_MAP.put("bilibili.upload_video", "B站上传视频");
        PREFIX_MAP.put("bilibili.download_video", "B站下载视频");
        PREFIX_MAP.put("bot.upload_file", "机器人上传文件");
        PREFIX_MAP.put("bot.switch", "机器人功能开关");
        PREFIX_MAP.put("bot.ws", "Websocket");
        PREFIX_MAP.put("searchimg.saucenao", "SauceNao识图");
        PREFIX_MAP.put("searchimg.agefans", "Agefans");
        PREFIX_MAP.put("ds.api", "DeepSeek");
        PREFIX_MAP.put("jm", "JM");
        PREFIX_MAP.put("url_conf", "站点地址");
        PREFIX_MAP.put("db.chat_extend", "聊天记录");
        PREFIX_MAP.put("server", "服务端");

        PREFIXES_BY_LENGTH_DESC.addAll(PREFIX_MAP.keySet());
        PREFIXES_BY_LENGTH_DESC.sort((a, b) -> Integer.compare(b.length(), a.length()));
    }
}
