package com.haruhi.botServer.config.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * yml/yaml 读取工具
 * <p>
 * {@code Configs} 需要的是"扁平 key -> 值"，所以这里把 yml 的嵌套结构拍平成
 * {@code a.b.c=value}（与 properties 完全一致的寻址方式），并且只保留叶子节点。
 * <p>
 * 为什么不支持写：yml 的缩进/锚点/多行块很难在保留格式的前提下安全改写，
 * 因此 {@code server.yml} 这类文件由 Spring 直接消费，改动后重启即可，
 * 配置管理页上也标注为"需重启 + 请直接编辑该文件"。
 */
@Slf4j
public final class YamlFileUtil {

    private YamlFileUtil() {
    }

    /**
     * 读取外置 ${configDir}/{fileName}，不存在返回空map
     */
    public static Map<String, String> load(File file) {
        if (file == null || !file.isFile()) {
            return Collections.emptyMap();
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            return flatten(new Yaml().load(reader));
        } catch (Exception e) {
            log.error("读取yaml配置异常 path:{}", file.getAbsolutePath(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * 读取jar内 classpath:config/{fileName}，不存在返回空map
     */
    public static Map<String, String> loadFromClasspath(String fileName) {
        ClassPathResource resource = new ClassPathResource("config/" + fileName);
        if (!resource.exists()) {
            return Collections.emptyMap();
        }
        try (InputStream in = resource.getInputStream();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return flatten(new Yaml().load(reader));
        } catch (IOException e) {
            log.warn("读取内置yaml配置异常 {}", fileName, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 把嵌套结构拍平成点分key，只保留叶子节点
     */
    private static Map<String, String> flatten(Object root) {
        Map<String, String> result = new LinkedHashMap<>();
        collect(root, "", result);
        return result;
    }

    private static void collect(Object node, String prefix, Map<String, String> result) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String path = prefix.isEmpty() ? key : prefix + "." + key;
                collect(entry.getValue(), path, result);
            }
            return;
        }
        if (node instanceof Iterable<?> iterable) {
            // 列表统一用逗号连接，与 ConfigType.LIST 的解析方式保持一致
            StringBuilder sb = new StringBuilder();
            for (Object item : iterable) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(item);
            }
            result.put(prefix, sb.toString());
            return;
        }
        if (prefix.isEmpty() || node == null) {
            return;
        }
        result.put(prefix, String.valueOf(node));
    }
}
