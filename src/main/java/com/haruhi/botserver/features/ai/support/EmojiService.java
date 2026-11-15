package com.haruhi.botserver.features.ai.support;

import com.haruhi.botserver.configuration.service.Configs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 表情包目录：./aichat/emojis/。文件名（去扩展名）就是表情名，AI 只能按名字选择，不能指定路径。
 */
@Component
@Slf4j
public class EmojiService {

    private static final Set<String> EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final long SCAN_INTERVAL_MS = 2000L;

    private volatile Map<String, Emoji> cache = Map.of();
    private volatile long cachedDirMtime = Long.MIN_VALUE;
    private volatile long lastScanAt;
    private volatile boolean devFallbackLogged;

    public record Emoji(String name, Path path) {
    }

    public Path emojiDir() {
        Path primary = Paths.get(Configs.baseDir(), "aichat", "emojis").toAbsolutePath().normalize();
        if (hasEmojis(primary)) {
            return primary;
        }
        Path devFallback = Paths.get("aichat", "emojis").toAbsolutePath().normalize();
        if (!primary.equals(devFallback) && hasEmojis(devFallback)) {
            if (!devFallbackLogged) {
                devFallbackLogged = true;
                log.info("使用开发目录表情包: {}", devFallback);
            }
            return devFallback;
        }
        try {
            Files.createDirectories(primary);
        } catch (IOException e) {
            log.warn("创建表情包目录失败: {}", primary, e);
        }
        return primary;
    }

    public List<Emoji> list() {
        if (!AiChatSettings.emojiEnabled()) {
            return List.of();
        }
        refresh();
        return new ArrayList<>(cache.values());
    }

    public Optional<Emoji> find(String name) {
        if (!AiChatSettings.emojiEnabled()) {
            return Optional.empty();
        }
        String key = normalize(name);
        if (key == null) {
            return Optional.empty();
        }
        refresh();
        return Optional.ofNullable(cache.get(key));
    }

    public String namesForPrompt(int max) {
        List<String> names = list().stream().map(Emoji::name).toList();
        if (names.isEmpty()) {
            return "";
        }
        int limit = max <= 0 ? 50 : max;
        List<String> shown = names.size() <= limit ? names : names.subList(0, limit);
        String joined = String.join("、", shown);
        if (names.size() <= limit) {
            return joined;
        }
        return joined + "（仅列前" + limit + "个，共" + names.size() + "个）";
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (now - lastScanAt < SCAN_INTERVAL_MS) {
            return;
        }
        lastScanAt = now;
        Path dir = emojiDir();
        long mtime;
        try {
            mtime = Files.getLastModifiedTime(dir).toMillis();
        } catch (IOException e) {
            return;
        }
        if (mtime == cachedDirMtime) {
            return;
        }
        cachedDirMtime = mtime;
        Map<String, Emoji> scanned = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                    .filter(this::isEmojiFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                String name = fileName.substring(0, fileName.lastIndexOf('.')).trim();
                String key = name.toLowerCase(Locale.ROOT);
                Emoji previous = scanned.putIfAbsent(key, new Emoji(name, file));
                if (previous != null) {
                    log.warn("表情包重名，已忽略: {}（保留 {}）", file, previous.path());
                }
            }
        } catch (IOException e) {
            log.warn("扫描表情包目录失败: {}", dir, e);
            return;
        }
        cache = Collections.unmodifiableMap(scanned);
        if (!scanned.isEmpty()) {
            log.info("已加载 {} 个表情包: {}", scanned.size(), dir);
        }
    }

    private boolean isEmojiFile(Path file) {
        String fileName = file.getFileName().toString();
        if (fileName.startsWith(".")) {
            return false;
        }
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return false;
        }
        if (!EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT))) {
            return false;
        }
        try {
            return Files.size(file) <= MAX_FILE_BYTES;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasEmojis(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.anyMatch(this::isEmojiFile);
        } catch (IOException e) {
            return false;
        }
    }

    private String normalize(String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String value = name.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1).trim();
        }
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.contains("..")) {
            return null;
        }
        int dot = value.lastIndexOf('.');
        if (dot > 0 && EXTENSIONS.contains(value.substring(dot + 1).toLowerCase(Locale.ROOT))) {
            value = value.substring(0, dot).trim();
        }
        return value.isEmpty() ? null : value.toLowerCase(Locale.ROOT);
    }
}
