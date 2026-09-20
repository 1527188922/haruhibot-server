package com.haruhi.botServer.config.util;

import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.utils.FileUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * properties 文件读写工具
 * <p>
 * 目标是<b>人可直接编辑</b>，因此不用 {@link java.util.Properties#store}（它会打乱顺序、丢掉注释），
 * 而是按行解析、按行更新，保留原有注释、空行、顺序与换行符风格。
 */
@Slf4j
public final class PropertiesFileUtil {

    private PropertiesFileUtil() {
    }

    /**
     * 读取 ./config/{fileName}，文件不存在返回空map
     */
    public static Map<String, String> load(String fileName) {
        File file = new File(Configs.configDir(), fileName);
        if (!file.isFile()) {
            return Collections.emptyMap();
        }
        return load(file);
    }

    public static Map<String, String> load(File file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取配置文件异常 path:{}", file.getAbsolutePath(), e);
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Entry entry : parse(file, lines)) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    /**
     * 解析文件内容，返回每一项所在的行号与原始文本，供更新时定位
     */
    public static List<Entry> parse(File file, List<String> lines) {
        List<Entry> entries = new ArrayList<>();
        StringBuilder pendingKey = null;
        StringBuilder pendingValue = null;
        int pendingLineNo = -1;
        String pendingRaw = null;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNo = i + 1;
            if (pendingKey != null) {
                // 上一行以未转义的反斜杠结尾，表示值在下一行继续
                pendingRaw = pendingRaw + "\n" + line;
                pendingValue.append('\n').append(line);
                if (!endsWithOddBackslash(line)) {
                    entries.add(new Entry(pendingKey.toString(), unescape(pendingValue.toString()), lineNo, pendingRaw));
                    pendingKey = null;
                    pendingValue = null;
                    pendingRaw = null;
                }
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            int sep = indexOfSeparator(line);
            if (sep < 0) {
                continue;
            }
            String rawKey = line.substring(0, sep).trim();
            String rawValue = line.substring(sep + 1).trim();
            if (rawKey.isEmpty()) {
                continue;
            }
            if (endsWithOddBackslash(line)) {
                pendingKey = new StringBuilder(rawKey);
                pendingValue = new StringBuilder(rawValue);
                pendingLineNo = lineNo;
                pendingRaw = line;
                continue;
            }
            entries.add(new Entry(rawKey, unescape(rawValue), lineNo, line));
        }

        if (pendingKey != null) {
            entries.add(new Entry(pendingKey.toString(), unescape(pendingValue.toString()), pendingLineNo, pendingRaw));
        }
        return entries;
    }

    /**
     * 将 key=value 写入 ./config/{fileName}
     * <p>
     * 已存在则只替换该行（保留其前后注释与位置），不存在则追加到文件末尾。
     * 文件不存在会自动创建（含目录）。
     */
    public static synchronized void save(String fileName, String key, String value) throws IOException {
        File file = new File(Configs.configDir(), fileName);
        save(file, key, value);
    }

    public static synchronized void save(File file, String key, String value) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            FileUtil.mkdirs(file.getParentFile().getAbsolutePath());
        }
        List<String> lines = file.isFile()
                ? new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
                : new ArrayList<>();

        String newLine = escape(value);
        int target = -1;
        for (Entry entry : parse(file, lines)) {
            if (entry.key.equals(key)) {
                target = entry.lineNo;
                break;
            }
        }
        String text = key + "=" + newLine;
        if (target > 0) {
            lines.set(target - 1, text);
        } else {
            if (!lines.isEmpty() && !lines.getLast().isBlank()) {
                lines.add("");
            }
            lines.add(text);
        }
        writeLines(file, lines);
    }

    /**
     * 从文件中删除某个key所在的行（用于"重置为默认值"）
     * <p>
     * 注意：与该key同属一个多行值的续行也会一并删除
     */
    public static synchronized void remove(String fileName, String key) throws IOException {
        File file = new File(Configs.configDir(), fileName);
        remove(file, key);
    }

    public static synchronized void remove(File file, String key) throws IOException {
        if (!file.isFile()) {
            return;
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        Entry target = null;
        for (Entry entry : parse(file, lines)) {
            if (entry.key.equals(key)) {
                target = entry;
                break;
            }
        }
        if (target == null) {
            return;
        }
        int count = target.raw.split("\n", -1).length;
        for (int i = 0; i < count; i++) {
            lines.remove(target.lineNo - 1);
        }
        writeLines(file, lines);
    }

    private static void writeLines(File file, List<String> lines) throws IOException {
        // 统一用 \n 写回：不能按平台用 \r\n，否则 readAllLines 会把 \r 留在行内容里
        FileUtil.writeText(file, String.join("\n", lines));
    }

    /**
     * 转义为 properties 单行可表示的文本
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 还原转义字符
     */
    public static String unescape(String value) {
        if (value == null || value.indexOf('\\') < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i == value.length() - 1) {
                sb.append(c);
                continue;
            }
            char next = value.charAt(++i);
            switch (next) {
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'f' -> sb.append('\f');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        try {
                            sb.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            // 不是合法的 unicode 转义，按普通反斜杠处理
                            sb.append('\\').append(next);
                        }
                    } else {
                        sb.append('\\').append(next);
                    }
                }
                // 未识别的转义序列保留反斜杠，例如 windows 路径 D:\my\bot\db
                default -> sb.append('\\').append(next);
            }
        }
        return sb.toString();
    }

    /**
     * 找到 key 与 value 的分隔符位置，支持 = 、: 以及空白分隔
     */
    private static int indexOfSeparator(String line) {
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '=' || c == ':') {
                return i;
            }
            if (Character.isWhitespace(c)) {
                return i;
            }
            if (c == '\\') {
                i++;
            }
        }
        return -1;
    }

    private static boolean endsWithOddBackslash(String line) {
        int count = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            count++;
        }
        return count % 2 == 1;
    }

    /**
     * 配置文件中的一项
     */
    @Getter
    public static class Entry {
        private final String key;
        private final String value;
        /** key所在行号，从1开始 */
        private final int lineNo;
        /** 原始文本（可能跨行） */
        private final String raw;

        Entry(String key, String value, int lineNo, String raw) {
            this.key = key;
            this.value = value;
            this.lineNo = lineNo;
            this.raw = raw;
        }
    }
}
