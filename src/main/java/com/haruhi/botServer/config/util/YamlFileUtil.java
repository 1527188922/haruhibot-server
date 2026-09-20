package com.haruhi.botServer.config.util;

import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * yml/yaml 读写工具
 * <p>
 * <b>读</b>：把嵌套结构拍平成 {@code a.b.c=value}（与 properties 完全一致的寻址方式），只保留叶子节点。
 * <b>写</b>：按行定位后<b>原地替换</b>目标行的值，保留注释、缩进与键顺序；目标路径不存在时，
 * 在文件末尾以"点分键"追加（YAML 允许 {@code a.b.c: value} 这种写法，语义等价于嵌套）。
 * <p>
 * 之所以不用 {@code Yaml.dump()} 整体重写：那样会丢掉所有注释、打乱顺序，
 * 对 application.yml 这类人也要看的文件不可接受。
 */
@Slf4j
public final class YamlFileUtil {

    private YamlFileUtil() {
    }

    // ==================================================================
    // 读
    // ==================================================================

    /**
     * 读取外置 yml 文件，不存在返回空map
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
     * 读取jar内 classpath:{fileName}，不存在返回空map
     */
    public static Map<String, String> loadFromClasspath(String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName);
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

    // ==================================================================
    // 写
    // ==================================================================

    /**
     * 写入 {@code path}（点分形式，如 {@code server.port}），值按字符串语义编码
     * <p>
     * 已存在则只替换该行的值（保留其注释与位置）；父节点存在但该键不存在时，
     * 插入到父级块的末尾（缩进与同级一致）；父节点也不存在时，在文件末尾以点分键追加。
     * 文件不存在会自动创建（含目录）。
     */
    public static synchronized void save(File file, String path, String value) throws IOException {
        saveEncoded(file, path, encodeScalar(value));
    }

    /**
     * 写入已经编码好的标量
     * <p>
     * 供按 {@code ConfigType} 决定是否加引号的调用方使用（如数字/布尔不该被加引号）
     */
    public static synchronized void saveEncoded(File file, String path, String encoded) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            FileUtil.mkdirs(file.getParentFile().getAbsolutePath());
        }
        List<String> lines = file.isFile()
                ? new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
                : new ArrayList<>();

        int target = locate(lines, path);
        if (target >= 0) {
            lines.set(target, replaceValue(lines.get(target), encoded));
        } else {
            int lastDot = path.lastIndexOf('.');
            String parentPath = lastDot < 0 ? null : path.substring(0, lastDot);
            String leaf = path.substring(lastDot + 1);
            int parentLine = parentPath == null ? -1 : locate(lines, parentPath);
            if (parentLine >= 0) {
                // 父节点存在：插入到父级块末尾，缩进与同级一致
                // 注意 indentOf 返回 int，必须显式拼成字符串，否则会变成整数加法
                String childIndent = " ".repeat(indentOf(lines.get(parentLine)) + 2);
                int insertAt = blockEnd(lines, parentLine);
                lines.add(insertAt, childIndent + leaf + ": " + encoded);
            } else {
                // 父节点也不存在：文件末尾以点分键追加（YAML 允许，语义等价于嵌套）
                if (!lines.isEmpty() && !lines.getLast().isBlank()) {
                    lines.add("");
                }
                lines.add(path + ": " + encoded);
            }
        }
        writeLines(file, lines);
    }

    /**
     * 父级块的结束位置（即应该插入同级子键的下标）
     * <p>
     * 返回最后一行非空内容之后的位置，避免插到块尾空行之后而产生的错位
     */
    private static int blockEnd(List<String> lines, int parentLine) {
        int parentIndent = indentOf(lines.get(parentLine));
        int last = parentLine + 1;
        for (int i = parentLine + 1; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (indentOf(lines.get(i)) <= parentIndent) {
                break;
            }
            last = i + 1;
        }
        return last;
    }

    /**
     * 删除 {@code path} 对应的行
     * <p>
     * 仅当该路径确实存在对应行时才删除
     *
     * @return 是否删除了内容
     */
    public static synchronized boolean remove(File file, String path) throws IOException {
        if (file == null || !file.isFile()) {
            return false;
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        int target = locate(lines, path);
        if (target < 0) {
            return false;
        }
        lines.remove(target);
        writeLines(file, lines);
        return true;
    }

    /**
     * 保留原行的缩进与"key:"部分，只替换值（并保留行尾注释）
     * <p>
     * 传进来的 {@code encoded} 已经是编码好的标量，这里不能再编码一次
     */
    private static String replaceValue(String origin, String encoded) {
        int sep = origin.indexOf(':');
        String head = origin.substring(0, sep + 1);
        String tail = origin.substring(sep + 1);
        return head + " " + encoded + trailingComment(tail);
    }

    /**
     * 取出值后面的行尾注释（形如 {@code value # 说明}），没有则返回空串
     */
    static String trailingComment(String valuePart) {
        int idx = valuePart.indexOf(" #");
        if (idx < 0 || valuePart.trim().startsWith("#")) {
            return "";
        }
        return " " + valuePart.substring(idx + 1).trim();
    }

    /**
     * 定位点分路径对应的行号，找不到返回-1
     * <p>
     * 每段必须是父节点的<b>直接子节点</b>：缩进要大于父节点，且不能超过该层级第一个
     * 子节点的缩进（否则会误匹配到更深层的同名键，例如把 logging 取到
     * logging.level.com.haruhi.botServer 那一行）
     */
    private static int locate(List<String> lines, String path) {
        String[] segments = path.split("\\.");
        int blockFrom = 0;
        int blockTo = lines.size();
        int parentIndent = -1;
        int matched = -1;

        for (String segment : segments) {
            int childIndent = -1;
            boolean found = false;
            for (int i = blockFrom; i < blockTo; i++) {
                String raw = lines.get(i);
                if (!isKeyLine(raw)) {
                    continue;
                }
                int indent = indentOf(raw);
                if (indent <= parentIndent) {
                    break;
                }
                if (childIndent < 0) {
                    childIndent = indent;
                } else if (indent > childIndent) {
                    // 更深层级的内容，不属于当前节点
                    continue;
                }
                if (keyOf(raw).equals(segment)) {
                    matched = i;
                    parentIndent = indent;
                    blockFrom = i + 1;
                    blockTo = blockEnd(lines, i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                return -1;
            }
        }
        return matched;
    }

    private static boolean isKeyLine(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
            return false;
        }
        return !keyOf(raw).isEmpty();
    }

    private static String keyOf(String raw) {
        int sep = raw.indexOf(':');
        return raw.substring(0, sep < 0 ? raw.length() : sep).trim();
    }

    private static int indentOf(String raw) {
        int i = 0;
        while (i < raw.length() && raw.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    /**
     * 生成 YAML 标量文本：需要时加双引号，避免被解析成布尔/数字或触发语法歧义
     */
    public static String encodeScalar(String value) {
        if (value == null || value.isEmpty()) {
            return "\"\"";
        }
        String v = value;
        boolean needQuote = v.startsWith(" ")
                || v.endsWith(" ")
                || v.contains(": ")
                || v.endsWith(":")
                || v.contains(" #")
                || v.contains("\n")
                || v.contains("\t")
                || "#&*!|>%@`{}[]\"'-?".indexOf(v.charAt(0)) >= 0
                || looksLikeNonString(v);
        if (!needQuote) {
            return v;
        }
        return "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\t", "\\t") + "\"";
    }

    /**
     * 纯数字/布尔/null 形式的值必须加引号，否则会被 YAML 解析成非字符串
     */
    private static boolean looksLikeNonString(String v) {
        String lower = v.toLowerCase();
        if ("true".equals(lower) || "false".equals(lower) || "null".equals(lower) || "~".equals(lower)
                || "yes".equals(lower) || "no".equals(lower) || "on".equals(lower) || "off".equals(lower)) {
            return true;
        }
        return v.matches("[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?");
    }

    /**
     * 统一用 \n 写回
     * <p>
     * 不能按平台用 \r\n：readAllLines 会把 \r 留在行内容里，而 YAML 解析时行尾的 \r 会被
     * 当成标量的一部分（"dev\r" != "dev"），导致读回来的值多一个回车
     */
    private static void writeLines(File file, List<String> lines) throws IOException {
        FileUtil.writeText(file, String.join("\n", lines));
    }
}
