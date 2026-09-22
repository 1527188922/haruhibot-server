package com.haruhi.botServer.controller;

import com.haruhi.botServer.config.BotConfig;
import com.haruhi.botServer.config.config.ConfigFile;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.service.ConfigChange;
import com.haruhi.botServer.config.service.ConfigHub;
import com.haruhi.botServer.config.vo.ConfigFileNode;
import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.vo.HttpResp;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置管理
 * <p>
 * 配置真源是 ./config/*.properties，本接口只做"页面 <-> 文件"的双向同步：
 * <ul>
 *     <li>保存：写文件 + 刷新内存快照，热更新配置立刻生效</li>
 *     <li>key级刷新：重新读取该key所在文件，用于外部改过文件后单独刷新某一项</li>
 *     <li>文件级刷新：重新读取整个文件，用于一次性同步整个大类</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping(BotConfig.CONTEXT_PATH + "/config")
public class ConfigController {

    @Autowired
    private ConfigHub configHub;

    /**
     * 全部配置，按文件分组
     */
    @PostMapping("/list")
    public HttpResp<List<ConfigFileNode>> list() {
        return HttpResp.success(configHub.list());
    }

    /**
     * 读取单个配置文件的原始内容
     */
    @PostMapping("/file/content")
    public HttpResp<String> fileContent(@RequestBody ConfigReq request) {
        ConfigFile file = request.configFile();
        File disk = Configs.fileOf(file);
        if (!disk.isFile()) {
            return HttpResp.success("");
        }
        try {
            return HttpResp.success(Files.readString(disk.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return HttpResp.fail("读取配置文件失败：" + e.getMessage(), null);
        }
    }

    /**
     * 保存单个配置项
     */
    @PostMapping("/save")
    public HttpResp<SaveResult> save(@RequestBody SaveReq request) {
        try {
            ConfigKey key = request.configKey();
            return doSave(Map.of(key, StringUtils.defaultString(request.getValue())));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    /**
     * 批量保存（前端"保存全部"），返回每一项是否触发了热更新
     */
    @PostMapping("/batchSave")
    public HttpResp<SaveResult> batchSave(@RequestBody BatchSaveReq request) {
        if (request == null || CollectionUtils.isEmpty(request.getItems())) {
            return HttpResp.fail("缺少参数", null);
        }
        try {
            Map<ConfigKey, String> values = new LinkedHashMap<>();
            for (SaveReq item : request.getItems()) {
                values.put(item.configKey(), StringUtils.defaultString(item.getValue()));
            }
            return doSave(values);
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    /**
     * 重置为默认值
     * <p>
     * 把声明里的默认值写回该key（<b>保留该key</b>），走的是和保存同一套写文件逻辑，
     * 因此不会重复追加、也不会在 yml 里写成 properties 风格的点分行
     */
    @PostMapping("/reset")
    public HttpResp<SaveResult> reset(@RequestBody ConfigReq request) {
        try {
            ConfigKey key = request.configKey();
            return buildResult("重置", configHub.resetAll(List.of(key)), List.of(key));
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
    }

    /**
     * 单key刷新：重新读取该key所在的配置文件
     */
    @PostMapping("/refresh")
    public HttpResp<String> refresh(@RequestBody ConfigReq request) {
        ConfigKey key;
        try {
            key = request.configKey();
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        }
        List<ConfigChange> changes = configHub.refresh(key);
        return HttpResp.success(describe(changes, key.getKey()));
    }

    /**
     * 文件级刷新：重新读取整个配置文件
     */
    @PostMapping("/refreshFile")
    public HttpResp<String> refreshFile(@RequestBody ConfigReq request) {
        ConfigFile file = request.configFile();
        List<ConfigChange> changes = configHub.refreshFile(file);
        return HttpResp.success(describe(changes, file.getFileName()), null);
    }

    /**
     * 全量刷新（兜底按钮）
     */
    @PostMapping("/refreshAll")
    public HttpResp<String> refreshAll() {
        configHub.refreshAll();
        return HttpResp.success("已重新加载全部配置文件");
    }

    private HttpResp<SaveResult> doSave(Map<ConfigKey, String> values) {
        try {
            Map<ConfigKey, Boolean> applied = configHub.saveAll(values);
            return buildResult("保存", applied, values.keySet());
        } catch (BusinessException e) {
            return HttpResp.fail(e.getErrorMsg(), null);
        } catch (Exception e) {
            log.error("保存配置异常", e);
            return HttpResp.fail("保存异常：" + e.getMessage(), null);
        }
    }

    /**
     * 把"key -> 是否触发了热更新"整理成前端要的提示与标志位
     */
    private HttpResp<SaveResult> buildResult(String action, Map<ConfigKey, Boolean> applied, Collection<ConfigKey> keys) {
        SaveResult result = new SaveResult();
        result.setHot(applied.values().stream().anyMatch(Boolean::booleanValue));
        result.setHotKeys(applied.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(e -> e.getKey().getKey())
                .toList());
        List<String> needRestart = new ArrayList<>();
        for (ConfigKey key : keys) {
            if (!key.isHot()) {
                needRestart.add(key.getKey());
            }
        }
        result.setRestartKeys(needRestart);
        // 非hot项走完流程也一定不会即时生效，所以"有非hot项"就等于"需要重启"
        result.setRestartRequired(!needRestart.isEmpty());

        String message = action + "成功";
        if (!result.getHotKeys().isEmpty()) {
            message += "，已即时生效：" + String.join("、", result.getHotKeys());
        }
        if (!needRestart.isEmpty()) {
            message += "，需重启生效：" + String.join("、", needRestart);
        }
        return HttpResp.success(message, result);
    }

    private String describe(List<ConfigChange> changes, String target) {
        if (CollectionUtils.isEmpty(changes)) {
            return target + " 无变化";
        }
        List<String> names = changes.stream()
                .filter(e -> e.key().isHot())
                .map(e -> e.key().getKey())
                .toList();
        if (names.isEmpty()) {
            return target + " 已重新加载（共" + changes.size() + "项变化，均为重启生效项）";
        }
        return target + " 已重新加载并即时生效：" + String.join("、", names);
    }

    // ==================== 请求/响应 ====================

    @Data
    public static class ConfigReq {
        /** 配置key */
        private String key;
        /** 文件名 */
        private String fileName;

        public ConfigKey configKey() {
            ConfigKey configKey = ConfigKey.of(key);
            if (configKey == null) {
                throw new BusinessException("不支持的配置项：" + key);
            }
            // 同一属性名出现在多个文件（如 dev/prod 的日志级别）时，必须带上文件名
            if (ConfigKey.isAmbiguous(key)) {
                if (StringUtils.isBlank(fileName)) {
                    throw new BusinessException("配置项 " + key + " 在多个文件中存在（"
                            + ambiguousFiles(key) + "），请同时指定 fileName");
                }
                ConfigKey scoped = ConfigKey.of(configFile(), key);
                if (scoped == null) {
                    throw new BusinessException("文件 " + fileName + " 中不存在配置项：" + key);
                }
                return scoped;
            }
            return configKey;
        }

        /**
         * 列出声明了该 key 的文件名，用于错误提示
         */
        private String ambiguousFiles(String key) {
            return java.util.Arrays.stream(ConfigFile.values())
                    .filter(file -> ConfigKey.of(file, key) != null)
                    .map(ConfigFile::getFileName)
                    .collect(java.util.stream.Collectors.joining("、"));
        }

        public ConfigFile configFile() {
            for (ConfigFile file : ConfigFile.values()) {
                if (file.getFileName().equals(fileName)) {
                    return file;
                }
            }
            throw new BusinessException("不支持的配置文件：" + fileName);
        }
    }

    @Data
    public static class SaveReq extends ConfigReq {
        private String value;
    }

    @Data
    public static class BatchSaveReq {
        private List<SaveReq> items;
    }

    @Data
    public static class SaveResult {
        /** 是否有配置即时生效 */
        private boolean hot;
        /** 是否需要重启 */
        private boolean restartRequired;
        /** 即时生效的key */
        private List<String> hotKeys = new ArrayList<>();
        /** 需要重启生效的key */
        private List<String> restartKeys = new ArrayList<>();
    }
}
