package com.haruhi.botserver.features.ai.support.prompt;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 角色卡加载：只读程序目录下的 ./aichat/roles/*.md。
 */
@Component
@Slf4j
public class RoleCardService {

    private static final Pattern TRIGGER_LINE = Pattern.compile("^(?:触发词|觸發詞|trigger)\\s*[:：]\\s*(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern MODE_TAG = Pattern.compile("[\\[〔（(]\\s*(基础|有工具|有视觉|v1|v2)\\s*[\\]〕）)]", Pattern.CASE_INSENSITIVE);
    private static final List<String> LINT_WORDS = List.of("qq_", "mcp__", "[SILENT]", "空格分句", "自动转发", "qq_send_");

    private final Map<String, CachedRole> cache = new ConcurrentHashMap<>();
    private volatile boolean devFallbackLogged;

    public Path roleDir() {
        Path primary = Paths.get(Configs.baseDir(), "aichat", "roles").toAbsolutePath().normalize();
        if (hasRoles(primary)) {
            return primary;
        }
        // 开发环境 Configs.baseDir() 往往是 target/classes；仓库根目录的 aichat/roles 作为兜底
        Path devFallback = Paths.get("aichat", "roles").toAbsolutePath().normalize();
        if (!primary.equals(devFallback) && hasRoles(devFallback)) {
            if (!devFallbackLogged) {
                devFallbackLogged = true;
                log.info("使用开发目录角色卡: {}", devFallback);
            }
            return devFallback;
        }
        try {
            Files.createDirectories(primary);
        } catch (IOException e) {
            log.warn("创建角色卡目录失败: {}", primary, e);
        }
        return primary;
    }

    private boolean hasRoles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.anyMatch(path -> Files.isRegularFile(path)
                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"));
        } catch (IOException e) {
            return false;
        }
    }

    public List<String> listRoleNames() {
        Path dir = roleDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".md"))
                    .map(name -> name.substring(0, name.length() - 3))
                    .filter(this::isSafeName)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            log.warn("扫描角色卡目录失败: {}", dir, e);
            return List.of();
        }
    }

    /**
     * 按名称加载；名称大小写不敏感，.md 可省略。
     */
    public RoleCard load(String name) {
        if (!isSafeName(name)) {
            return null;
        }
        String normalized = normalizeName(name);
        if (normalized.isBlank()) {
            return null;
        }
        Path direct = roleDir().resolve(normalized + ".md");
        if (Files.isRegularFile(direct)) {
            return loadFile(normalized);
        }
        for (String roleName : listRoleNames()) {
            if (roleName.equalsIgnoreCase(normalized)) {
                return loadFile(roleName);
            }
        }
        return null;
    }

    /**
     * 会话角色 > 群/私聊映射 > 全局默认。
     */
    public RoleCard resolve(String sessionRole, long groupId, long userId, boolean group) {
        RoleCard role = load(sessionRole);
        if (role != null) {
            return role;
        }
        String mapped = group ? AiChatSettings.groupRole(groupId) : AiChatSettings.privateRole(userId);
        role = load(mapped);
        if (role != null) {
            return role;
        }
        return load(AiChatSettings.defaultRole());
    }

    private RoleCard loadFile(String roleName) {
        Path file = roleDir().resolve(roleName + ".md");
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            long mtime = Files.getLastModifiedTime(file).toMillis();
            String key = roleName.toLowerCase(Locale.ROOT);
            CachedRole cached = cache.get(key);
            if (cached != null && cached.mtime == mtime) {
                return cached.card;
            }
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            RoleCard card = parse(roleName, raw);
            cache.put(key, new CachedRole(mtime, card));
            return card;
        } catch (IOException e) {
            log.warn("读取角色卡失败: {}", file, e);
            return null;
        }
    }

    private RoleCard parse(String name, String raw) {
        List<String> lines = new ArrayList<>(List.of(raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)));
        List<String> triggers = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher matcher = TRIGGER_LINE.matcher(line);
            if (matcher.find()) {
                for (String word : matcher.group(1).split("[,，、]")) {
                    if (StringUtils.isNotBlank(word)) {
                        triggers.add(word.trim());
                    }
                }
                lines.remove(i);
            }
            break;
        }
        if (triggers.isEmpty()) {
            triggers.add(name);
        }

        boolean tools = AiChatSettings.agentEnabled();
        boolean vision = AiChatSettings.visionEnabled();
        String filtered = filterSections(String.join("\n", lines), tools, vision);
        List<String> warnings = lint(filtered);
        int max = AiChatSettings.roleMaxChars();
        if (filtered.length() > max) {
            filtered = truncate(filtered, max);
            warnings.add("角色卡超过 " + max + " 字符，已截断");
        }
        return new RoleCard(name, filtered.trim(), List.copyOf(triggers), List.copyOf(warnings));
    }

    private String filterSections(String text, boolean tools, boolean vision) {
        StringBuilder out = new StringBuilder();
        boolean include = true;
        int excludedUntilLevel = -1;
        for (String line : text.split("\n", -1)) {
            Matcher heading = HEADING.matcher(line);
            if (heading.matches()) {
                int level = heading.group(1).length();
                String title = heading.group(2);
                String mode = detectMode(title);
                if (mode != null) {
                    include = switch (mode) {
                        case "有工具" -> tools;
                        case "有视觉" -> vision;
                        default -> true;
                    };
                    excludedUntilLevel = include ? -1 : level;
                } else if (excludedUntilLevel > 0 && level <= excludedUntilLevel) {
                    // 同级或更高级的新小节：离开被排除的父小节
                    include = true;
                    excludedUntilLevel = -1;
                }
                if (include) {
                    out.append(heading.group(1)).append(' ').append(MODE_TAG.matcher(title).replaceAll("").trim()).append('\n');
                }
            } else if (include) {
                out.append(line).append('\n');
            }
        }
        return out.toString().replaceAll("\n{3,}", "\n\n").trim();
    }

    private String detectMode(String heading) {
        Matcher matcher = MODE_TAG.matcher(heading);
        if (!matcher.find()) {
            return null;
        }
        String tag = matcher.group(1).toLowerCase(Locale.ROOT);
        if (tag.contains("工具")) {
            return "有工具";
        }
        if (tag.contains("视觉")) {
            return "有视觉";
        }
        return "基础";
    }

    private List<String> lint(String content) {
        List<String> warnings = new ArrayList<>();
        String lower = content.toLowerCase(Locale.ROOT);
        for (String word : LINT_WORDS) {
            if (lower.contains(word.toLowerCase(Locale.ROOT))) {
                warnings.add("角色卡包含本项目不支持的指令：" + word);
            }
        }
        if (StringUtils.isBlank(content)) {
            warnings.add("角色卡内容为空");
        }
        return warnings;
    }

    private String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        int end = Math.max(0, max - 1);
        int lineBreak = text.lastIndexOf('\n', end);
        if (lineBreak > max / 2) {
            end = lineBreak;
        }
        return text.substring(0, end).trim() + "…";
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String value = name.trim();
        if (value.toLowerCase(Locale.ROOT).endsWith(".md")) {
            value = value.substring(0, value.length() - 3);
        }
        return value.trim();
    }

    private boolean isSafeName(String name) {
        if (StringUtils.isBlank(name)) {
            return false;
        }
        String normalized = normalizeName(name);
        return !normalized.isBlank()
                && !normalized.contains("/")
                && !normalized.contains("\\")
                && !normalized.contains("..")
                && !normalized.contains(":");
    }

    public record RoleCard(String name, String content, List<String> triggerWords, List<String> warnings) {
        public boolean matches(String text) {
            if (text == null) {
                return false;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            for (String word : triggerWords) {
                if (!word.isBlank() && lower.contains(word.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }

    private record CachedRole(long mtime, RoleCard card) {
    }
}
