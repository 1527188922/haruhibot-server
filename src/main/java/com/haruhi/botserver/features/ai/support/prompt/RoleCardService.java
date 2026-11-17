package com.haruhi.botserver.features.ai.support.prompt;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.ai.client.deepseek.DeepSeekMessage;
import com.haruhi.botserver.features.ai.model.AiPrompt;
import com.haruhi.botserver.features.ai.support.AiChatSettings;
import com.haruhi.botserver.shared.error.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final int MAX_NAME_CHARS = 30;
    private static final int MAX_SECTION_TITLE_CHARS = 40;
    private static final int MAX_SECTIONS = 20;
    private static final int MAX_LINE_CHARS = 500;
    private static final int MAX_TRIGGERS = 10;
    private static final int MAX_TRIGGER_CHARS = 20;

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

    /**
     * 清空 mtime 缓存，供 WebUI「重新扫描」使用。
     */
    public void clearCache() {
        cache.clear();
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
        RoleParse parsed = parseDefinition(raw);
        List<String> triggers = parsed.triggers().isEmpty() ? List.of(name) : parsed.triggers();
        String filtered = parsed.filteredContent();
        List<String> warnings = new ArrayList<>(parsed.warnings());
        int max = AiChatSettings.roleMaxChars();
        if (filtered.length() > max) {
            filtered = truncate(filtered, max);
            warnings.add("角色卡超过 " + max + " 字符，已截断");
        }
        return new RoleCard(name, filtered.trim(), List.copyOf(triggers), List.copyOf(warnings));
    }

    // ==================== WebUI 结构化编辑 ====================

    /**
     * 读取角色卡为章节结构；不存在返回 null。
     */
    public RoleDefinition definition(String name) {
        Path file = resolveFile(name);
        if (file == null) {
            return null;
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            RoleParse parsed = parseDefinition(raw);
            long mtime = Files.getLastModifiedTime(file).toMillis();
            return new RoleDefinition(fileNameWithoutExt(file), file.getFileName().toString(),
                    parsed.triggers(), parsed.sections(), parsed.filteredContent().length(),
                    AiChatSettings.roleMaxChars(), mtime, sha256(raw), parsed.warnings());
        } catch (IOException e) {
            log.warn("读取角色卡编辑数据失败: {}", file, e);
            return null;
        }
    }

    /**
     * 保存 / 新建 / 预览。originalName 为空表示新建。
     */
    public RoleSaveResult save(RoleSaveCommand command) {
        String original = StringUtils.trimToNull(command.originalName());
        String name = normalizeName(command.name());
        validateName(name);
        if (original != null && !name.equalsIgnoreCase(normalizeName(original))) {
            throw new BusinessException("编辑时不能修改角色名");
        }
        Path file = original == null ? null : resolveFile(original);
        if (original != null && file == null) {
            throw new BusinessException("角色卡不存在：" + original);
        }
        if (original == null && resolveFile(name) != null) {
            throw new BusinessException("角色已存在：" + name);
        }
        if (file != null) {
            checkConflict(file, command.baseMtime(), command.baseHash());
        }

        List<RoleSection> sections = sanitizeSections(command.sections());
        List<String> triggers = normalizeTriggers(command.triggers());
        String content = renderContent(sections);
        boolean tools = AiChatSettings.agentEnabled();
        boolean vision = AiChatSettings.visionEnabled();
        String filtered = filterSections(content, tools, vision);
        int max = AiChatSettings.roleMaxChars();
        if (filtered.length() > max) {
            throw new BusinessException("角色卡内容超过上限：" + filtered.length() + " / " + max);
        }
        List<String> warnings = lint(filtered);
        String markdown = renderMarkdown(name, triggers, sections);

        RoleDefinition definition;
        if (command.dryRun()) {
            long mtime = file == null ? 0L : lastModified(file);
            String hash = file == null ? "" : sha256(readAllBytes(file));
            definition = new RoleDefinition(name, name + ".md", triggers, sections,
                    filtered.length(), max, mtime, hash, warnings);
        } else {
            Path target = file != null ? file : roleDir().resolve(name + ".md");
            try {
                writeAtomic(target, markdown, file != null);
            } catch (IOException e) {
                throw new BusinessException("保存角色卡失败：" + e.getMessage());
            }
            clearCache();
            definition = definition(name);
            if (definition == null) {
                throw new BusinessException("保存后读取角色卡失败");
            }
        }
        int estimated = AiPrompt.estimate(DeepSeekMessage.system(filtered));
        return new RoleSaveResult(!command.dryRun(), definition, markdown, filtered, warnings, estimated);
    }

    /**
     * 复制角色卡；按字节复制，保留原文件格式。
     */
    public RoleDefinition copy(String sourceName, String targetName, boolean overwrite) {
        Path source = resolveFile(sourceName);
        if (source == null) {
            throw new BusinessException("源角色卡不存在：" + sourceName);
        }
        String target = StringUtils.trimToNull(targetName);
        if (target == null) {
            target = suggestCopyName(fileNameWithoutExt(source));
        }
        validateName(target);
        Path existing = resolveFile(target);
        if (existing != null && !overwrite) {
            throw new BusinessException("角色已存在：" + target);
        }
        Path targetFile = existing != null ? existing : roleDir().resolve(target + ".md");
        try {
            Files.createDirectories(roleDir());
            Path temp = Files.createTempFile(roleDir(), ".role-copy-", ".tmp");
            try {
                Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
                if (existing != null) {
                    backup(existing);
                }
                moveAtomic(temp, targetFile);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new BusinessException("复制角色卡失败：" + e.getMessage());
        }
        clearCache();
        return definition(target);
    }

    public boolean nameAvailable(String name, String originalName) {
        String normalized = normalizeName(name);
        if (!validName(normalized)) {
            return false;
        }
        if (originalName != null && normalized.equalsIgnoreCase(normalizeName(originalName))) {
            return true;
        }
        return resolveFile(normalized) == null;
    }

    public String suggestCopyName(String sourceName) {
        String source = normalizeName(sourceName);
        if (StringUtils.isBlank(source)) {
            source = "新角色";
        }
        Set<String> existing = new HashSet<>();
        for (String name : listRoleNames()) {
            existing.add(name.toLowerCase(Locale.ROOT));
        }
        String base = withSuffix(source, "-副本");
        if (!existing.contains(base.toLowerCase(Locale.ROOT))) {
            return base;
        }
        for (int i = 2; i <= 99; i++) {
            String candidate = withSuffix(source, "-副本" + i);
            if (!existing.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return withSuffix(source, "-副本" + System.currentTimeMillis());
    }

    private RoleParse parseDefinition(String raw) {
        List<String> lines = new ArrayList<>(List.of(raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)));
        LinkedHashSet<String> triggers = new LinkedHashSet<>();
        int triggerIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.startsWith("## ")) {
                break;
            }
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
                triggerIndex = i;
            }
            break;
        }
        if (triggerIndex >= 0) {
            lines.remove(triggerIndex);
        }

        List<RoleSection> sections = new ArrayList<>();
        List<String> preamble = new ArrayList<>();
        RoleSection current = null;
        boolean topSkipped = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!topSkipped && trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                topSkipped = true;
                continue;
            }
            if (trimmed.startsWith("## ")) {
                if (current != null) {
                    sections.add(current);
                } else if (!preamble.isEmpty()) {
                    sections.add(new RoleSection("前言", "base", new ArrayList<>(preamble)));
                }
                String title = trimmed.substring(3).trim();
                String mode = detectMode(title);
                String cleanTitle = MODE_TAG.matcher(title).replaceAll("").trim();
                current = new RoleSection(cleanTitle, modeCode(mode), new ArrayList<>());
            } else if (!trimmed.isEmpty()) {
                if (current != null) {
                    current.lines().add(trimmed);
                } else {
                    preamble.add(trimmed);
                }
            }
        }
        if (current != null) {
            sections.add(current);
        } else if (!preamble.isEmpty()) {
            sections.add(new RoleSection("前言", "base", new ArrayList<>(preamble)));
        }

        String content = renderContent(sections);
        String filtered = filterSections(content, AiChatSettings.agentEnabled(), AiChatSettings.visionEnabled());
        List<String> warnings = lint(filtered);
        return new RoleParse(List.copyOf(triggers), sections, filtered, warnings);
    }

    private Path resolveFile(String name) {
        String normalized = normalizeName(name);
        if (!validName(normalized)) {
            return null;
        }
        Path direct = roleDir().resolve(normalized + ".md");
        if (Files.isRegularFile(direct)) {
            return direct;
        }
        for (String roleName : listRoleNames()) {
            if (roleName.equalsIgnoreCase(normalized)) {
                return roleDir().resolve(roleName + ".md");
            }
        }
        return null;
    }

    private void validateName(String name) {
        if (StringUtils.isBlank(name)) {
            throw new BusinessException("角色名不能为空");
        }
        if (name.length() > MAX_NAME_CHARS) {
            throw new BusinessException("角色名不能超过 " + MAX_NAME_CHARS + " 字符");
        }
        if (!isSafeName(name)) {
            throw new BusinessException("角色名包含非法字符");
        }
    }

    private boolean validName(String name) {
        return StringUtils.isNotBlank(name) && name.length() <= MAX_NAME_CHARS && isSafeName(name);
    }

    private String renderContent(List<RoleSection> sections) {
        StringBuilder sb = new StringBuilder();
        if (sections == null) {
            return "";
        }
        for (RoleSection section : sections) {
            if (section == null || StringUtils.isBlank(section.title())) {
                continue;
            }
            List<String> lines = new ArrayList<>();
            if (section.lines() != null) {
                for (String line : section.lines()) {
                    if (StringUtils.isNotBlank(line)) {
                        lines.add(line.trim());
                    }
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            String mode = normalizeMode(section.mode());
            sb.append("## ").append(section.title().trim());
            if ("tools".equals(mode)) {
                sb.append("〔有工具〕");
            } else if ("vision".equals(mode)) {
                sb.append("〔有视觉〕");
            }
            sb.append('\n');
            for (String line : lines) {
                sb.append(line).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString().trim();
    }

    private String renderMarkdown(String name, List<String> triggers, List<RoleSection> sections) {
        StringBuilder sb = new StringBuilder();
        sb.append("# 角色卡：").append(name).append("\n\n");
        if (triggers != null && !triggers.isEmpty()) {
            sb.append("触发词：").append(String.join(",", triggers)).append("\n\n");
        }
        String content = renderContent(sections);
        if (!content.isEmpty()) {
            sb.append(content).append('\n');
        }
        return sb.toString();
    }

    private List<RoleSection> sanitizeSections(List<RoleSection> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new BusinessException("至少需要一个章节");
        }
        if (sections.size() > MAX_SECTIONS) {
            throw new BusinessException("章节数量不能超过 " + MAX_SECTIONS);
        }
        List<RoleSection> result = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        for (RoleSection section : sections) {
            if (section == null) {
                continue;
            }
            String title = MODE_TAG.matcher(StringUtils.trimToEmpty(section.title())).replaceAll("").trim();
            if (title.isEmpty()) {
                continue;
            }
            if (title.length() > MAX_SECTION_TITLE_CHARS) {
                throw new BusinessException("章节标题不能超过 " + MAX_SECTION_TITLE_CHARS + " 字符");
            }
            if (title.contains("\n") || title.startsWith("#")) {
                throw new BusinessException("章节标题不能包含换行或以 # 开头：" + title);
            }
            if (!titles.add(title.toLowerCase(Locale.ROOT))) {
                throw new BusinessException("章节标题重复：" + title);
            }
            List<String> lines = new ArrayList<>();
            if (section.lines() != null) {
                for (String line : section.lines()) {
                    String value = StringUtils.trimToEmpty(line);
                    if (value.isEmpty()) {
                        continue;
                    }
                    if (value.length() > MAX_LINE_CHARS) {
                        throw new BusinessException("单行内容不能超过 " + MAX_LINE_CHARS + " 字符");
                    }
                    lines.add(value);
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            result.add(new RoleSection(title, normalizeMode(section.mode()), lines));
        }
        if (result.isEmpty()) {
            throw new BusinessException("至少需要一个有内容的章节");
        }
        return result;
    }

    private List<String> normalizeTriggers(List<String> triggers) {
        if (triggers == null) {
            return List.of();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String trigger : triggers) {
            String value = StringUtils.trimToEmpty(trigger);
            if (value.isEmpty()) {
                continue;
            }
            if (value.length() > MAX_TRIGGER_CHARS) {
                throw new BusinessException("触发词不能超过 " + MAX_TRIGGER_CHARS + " 字符");
            }
            result.add(value);
            if (result.size() >= MAX_TRIGGERS) {
                break;
            }
        }
        return List.copyOf(result);
    }

    private String normalizeMode(String mode) {
        if ("tools".equalsIgnoreCase(mode)) {
            return "tools";
        }
        if ("vision".equalsIgnoreCase(mode)) {
            return "vision";
        }
        return "base";
    }

    private String modeCode(String detected) {
        if ("有工具".equals(detected)) {
            return "tools";
        }
        if ("有视觉".equals(detected)) {
            return "vision";
        }
        return "base";
    }

    private void writeAtomic(Path target, String content, boolean backup) throws IOException {
        Files.createDirectories(roleDir());
        if (backup && Files.isRegularFile(target)) {
            backup(target);
        }
        Path temp = Files.createTempFile(roleDir(), ".role-", ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            moveAtomic(temp, target);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void moveAtomic(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void backup(Path file) throws IOException {
        Path backupDir = roleDir().resolve(".backups");
        Files.createDirectories(backupDir);
        String base = fileNameWithoutExt(file);
        Path target = backupDir.resolve(base + "-" + System.currentTimeMillis() + ".md");
        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        cleanupBackups(backupDir, base);
    }

    private void cleanupBackups(Path backupDir, String base) {
        try (Stream<Path> stream = Files.list(backupDir)) {
            List<Path> backups = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(base + "-"))
                    .sorted(Comparator.comparingLong(RoleCardService::lastModified).reversed())
                    .toList();
            for (int i = 10; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        } catch (IOException e) {
            log.debug("清理角色卡备份失败: {}", backupDir, e);
        }
    }

    private void checkConflict(Path file, long baseMtime, String baseHash) {
        try {
            if (StringUtils.isNotBlank(baseHash)) {
                String current = sha256(Files.readAllBytes(file));
                if (!current.equalsIgnoreCase(baseHash)) {
                    throw new BusinessException("角色卡已被外部修改，请重新加载后再保存");
                }
                return;
            }
            if (baseMtime > 0 && Files.getLastModifiedTime(file).toMillis() != baseMtime) {
                throw new BusinessException("角色卡已被外部修改，请重新加载后再保存");
            }
        } catch (IOException e) {
            throw new BusinessException("读取角色卡失败：" + e.getMessage());
        }
    }

    private byte[] readAllBytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new BusinessException("读取角色卡失败：" + e.getMessage());
        }
    }

    private String fileNameWithoutExt(Path file) {
        String name = file.getFileName().toString();
        return name.toLowerCase(Locale.ROOT).endsWith(".md") ? name.substring(0, name.length() - 3) : name;
    }

    private String withSuffix(String source, String suffix) {
        int max = MAX_NAME_CHARS - suffix.length();
        String base = source.length() <= max ? source : source.substring(0, Math.max(1, max));
        return base + suffix;
    }

    private static String sha256(String text) {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            return "";
        }
    }

    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
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

    public record RoleSection(String title, String mode, List<String> lines) {
    }

    public record RoleDefinition(String name, String fileName, List<String> triggers,
                                 List<RoleSection> sections, int chars, int maxChars,
                                 long mtime, String hash, List<String> warnings) {
    }

    public record RoleSaveCommand(String originalName, String name, List<String> triggers,
                                  List<RoleSection> sections, long baseMtime, String baseHash,
                                  boolean dryRun) {
    }

    public record RoleSaveResult(boolean saved, RoleDefinition definition, String renderedMarkdown,
                                 String filteredContent, List<String> warnings, int estimatedTokens) {
    }

    private record RoleParse(List<String> triggers, List<RoleSection> sections,
                             String filteredContent, List<String> warnings) {
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
