package com.haruhi.botserver.configuration.store;

import com.haruhi.botserver.shared.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * yml/yaml 读写工具
 * <p>
 * <b>读</b>：把嵌套结构拍平成 {@code a.b.c=value}（与 properties 完全一致的寻址方式），只保留叶子节点。
 * <b>写</b>：按行定位后<b>原地替换</b>目标行的值，保留注释、缩进与键顺序。
 * <p>
 * 定位方式不是"把 key 按 {@code .} 切开再逐段找行"，而是先把文件按缩进解析成
 * "行 -&gt; 该行拍平后的完整路径"，再按路径精确匹配。原因是
 * {@code logging.level.com.haruhi.botserver} 这种 key 的<b>最后一段本身带点</b>：
 * 按点切分后拿 {@code com} 去比对行上的 {@code com.haruhi.botserver} 永远不相等，
 * 于是每次保存都定位失败、不断往文件末尾追加一条 {@code a.b.c: value}（properties 风格）的重复行。
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
     *
     * @see #saveEncoded(File, String, String)
     */
    public static synchronized void save(File file, String path, String value) throws IOException {
        saveEncoded(file, path, encodeScalar(value));
    }

    /**
     * 写入已经编码好的标量
     * <p>
     * 供按 {@code ConfigType} 决定是否加引号的调用方使用（如数字/布尔不该被加引号）。
     * <p>
     * 写入规则（始终保留注释、空行与已有顺序）：
     * <ol>
     *     <li>文件里已有该 key（嵌套写法或 {@code a.b.c: value} 点分写法都算）→ 就地改值；
     *         并把历史上被重复追加的行一并删掉，保证一个 key 只剩一行</li>
     *     <li>该 key 只有点分写法（properties 风格）→ 删掉旧行，改写成 yml 嵌套结构</li>
     *     <li>key 缺失、但它的某个父块存在 → 插入到父块末尾，缩进与同级一致</li>
     *     <li>父块也不存在 → 在文件末尾补出第一层块 + 点分叶子（{@code server:} / {@code "  port: 8090"}、
     *         {@code logging:} / {@code "  level.com.haruhi.botserver: debug"}），而不是写成
     *         {@code server.port: 8090} 这种平铺行，也不发明多余层级</li>
     * </ol>
     * 文件不存在会自动创建（含目录）。
     */
    public static synchronized void saveEncoded(File file, String path, String encoded) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            FileUtil.mkdirs(file.getParentFile().getAbsolutePath());
        }
        List<String> lines = file.isFile()
                ? new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
                : new ArrayList<>();

        List<Node> matches = leaves(lines, path);
        if (matches.isEmpty()) {
            insert(lines, path, encoded);
        } else {
            // 优先改"嵌在父块里"的那一行（点分写法是同一路径的另一种写法，视作重复）
            Node keep = matches.stream().filter(e -> !e.flat()).findFirst().orElse(matches.get(0));
            if (keep.flat()) {
                // 整条路径挤在一行的旧写法：删掉后按 yml 嵌套结构重新写
                removeLines(lines, matches);
                insert(lines, path, encoded);
            } else {
                lines.set(keep.line, replaceValue(lines.get(keep.line), encoded));
                List<Node> redundant = matches.stream().filter(e -> e.line != keep.line).toList();
                removeLines(lines, redundant);
                if (!redundant.isEmpty()) {
                    pruneEmptyAncestors(lines, path);
                }
            }
        }
        writeLines(file, lines);
    }

    /**
     * 插入一个文件里还不存在的 key
     * <p>
     * 先尽量复用已有的父块（如已存在 {@code logging: level:} 时只在下面补叶子），
     * 完全没有可用父块时再在文件末尾补出整条嵌套路径
     */
    private static void insert(List<String> lines, String path, String encoded) {
        String[] segments = path.split("\\.");
        for (int depth = segments.length - 1; depth >= 1; depth--) {
            String parentPath = join(segments, 0, depth);
            Node parent = block(lines, parentPath);
            if (parent == null) {
                continue;
            }
            if (!leaves(lines, parentPath + "." + segments[depth]).isEmpty()) {
                // 该段在文件里已经是别的 key 的值（叶子），再往里嵌会造成重复key
                appendFlat(lines, path, encoded);
                return;
            }
            // 剩余路径作为点分叶子插到父块末尾，与文件里既有的 com.haruhi.botserver: xxx 写法保持一致
            String leaf = join(segments, depth, segments.length);
            int insertAt = blockEnd(lines, parent.line);
            lines.add(insertAt, " ".repeat(parent.indent + 2) + leaf + ": " + encoded);
            return;
        }
        // 顶层段已被占用（是块或叶子）时不能再补一个同名顶层块，只能用点分键写法
        if (block(lines, segments[0]) != null || !leaves(lines, segments[0]).isEmpty()) {
            appendFlat(lines, path, encoded);
            return;
        }
        if (!lines.isEmpty() && !lines.getLast().isBlank()) {
            lines.add("");
        }
        // 没有任何父块可复用：补出第一层块，其余路径作为点分叶子。
        // 只发明一层结构，避免把 logging.level.com.haruhi.botserver 拆成 com: haruhi: botServer:
        // 这种看着像层级、实际只是 logger 名字的假结构（两者拍平后的路径完全一致）
        if (segments.length == 1) {
            lines.add(segments[0] + ": " + encoded);
        } else {
            lines.add(segments[0] + ":");
            lines.add("  " + join(segments, 1, segments.length) + ": " + encoded);
        }
    }

    /**
     * 兜底写法：文件末尾追加 {@code a.b.c: value}（YAML 允许，语义等价于嵌套）
     * <p>
     * 只有在"父级已被其它类型占用、无法嵌套"时才会用到；这种写法同样能按完整路径
     * （见 {@link #parse}）命中，因此不会重复追加
     */
    private static void appendFlat(List<String> lines, String path, String encoded) {
        if (!lines.isEmpty() && !lines.getLast().isBlank()) {
            lines.add("");
        }
        lines.add(path + ": " + encoded);
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
     * 删除 {@code path} 对应的行（含历史上重复追加的行）
     * <p>
     * 仅当该路径确实存在对应行时才删除；删完会把它残留的空父块一并清理。
     * 配置管理页的"重置"<b>不用</b>它——重置是把默认值写回该key，保留这一行
     *
     * @return 是否删除了内容
     */
    public static synchronized boolean remove(File file, String path) throws IOException {
        if (file == null || !file.isFile()) {
            return false;
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        List<Node> matches = leaves(lines, path);
        if (matches.isEmpty()) {
            return false;
        }
        removeLines(lines, matches);
        pruneEmptyAncestors(lines, path);
        writeLines(file, lines);
        return true;
    }

    /**
     * 修复被历史上"每次保存都追加一行"的缺陷写坏的 yml：同一个 key 只保留一行
     * <p>
     * 重复的 key 会让 Spring 的 yml 加载器直接抛异常（{@code found duplicate key xxx}），
     * 应用根本起不来，而这类文件又只能用配置管理页修改——起不来就改不了，
     * 所以需要在启动早期先修一遍。
     * <p>
     * 保留<b>最后一行</b>：旧的保存逻辑定位失败后一律往文件末尾追加，最后一行才是用户最新的值。
     * 保留下来的行如果是 {@code a.b.c: value} 这种点分写法，会一并改写成 yml 嵌套结构。
     *
     * @return 是否修改了文件
     */
    public static synchronized boolean repairDuplicateKeys(File file) throws IOException {
        if (file == null || !file.isFile()) {
            return false;
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        Map<String, Integer> lastLine = new LinkedHashMap<>();
        Map<String, Integer> count = new LinkedHashMap<>();
        for (Node node : parse(lines)) {
            if (!node.leaf()) {
                continue;
            }
            lastLine.put(node.path(), node.line());
            count.merge(node.path(), 1, Integer::sum);
        }
        List<String> duplicated = count.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (duplicated.isEmpty()) {
            return false;
        }
        List<Node> redundant = new ArrayList<>();
        for (Node node : parse(lines)) {
            if (node.leaf() && lastLine.get(node.path()) != node.line()) {
                redundant.add(node);
            }
        }
        removeLines(lines, redundant);

        // 重复行里保留下来的若是点分写法，说明嵌套结构已经被删空了（或本来就没有），这里补回嵌套写法
        for (String path : duplicated) {
            List<Node> matches = leaves(lines, path);
            if (matches.size() == 1 && matches.get(0).flat()) {
                Node keep = matches.get(0);
                String value = valueText(lines.get(keep.line));
                removeLines(lines, matches);
                insert(lines, path, value);
            }
        }
        for (String path : duplicated) {
            pruneEmptyAncestors(lines, path);
        }
        collapseBlankLines(lines);
        writeLines(file, lines);
        return true;
    }

    /**
     * 把连续空行压成一个（删行后容易留下一串空行）
     */
    private static void collapseBlankLines(List<String> lines) {
        for (int i = lines.size() - 1; i > 0; i--) {
            if (lines.get(i).isBlank() && lines.get(i - 1).isBlank()) {
                lines.remove(i);
            }
        }
    }

    /**
     * 取出某行冒号之后的值部分（含行尾注释）
     */
    private static String valueText(String raw) {
        int sep = separatorIndex(raw);
        return sep < 0 ? "" : raw.substring(sep + 1).trim();
    }

    /**
     * 删掉某一行后，如果它的父块已经空了就继续删父块（例如只放 {@code port} 的 {@code server:} 块）
     * <p>
     * 块里还有其它 key 或注释时保留，避免把别人写的说明一起清掉
     */
    private static void pruneEmptyAncestors(List<String> lines, String path) {
        boolean removed = true;
        while (removed) {
            removed = false;
            for (Node node : parse(lines)) {
                if (node.leaf || !path.startsWith(node.path + ".")) {
                    continue;
                }
                if (blockHasContent(lines, node.line)) {
                    continue;
                }
                lines.remove(node.line);
                removed = true;
                break;
            }
        }
    }

    /**
     * 块内是否还有内容（子key或注释）
     */
    private static boolean blockHasContent(List<String> lines, int parentLine) {
        int parentIndent = indentOf(lines.get(parentLine));
        for (int i = parentLine + 1; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) {
                continue;
            }
            return indentOf(raw) > parentIndent;
        }
        return false;
    }

    /**
     * 按下标从大到小删除，避免下标错位
     */
    private static void removeLines(List<String> lines, List<Node> nodes) {
        List<Integer> indexes = new ArrayList<>(nodes.stream().map(Node::line).toList());
        indexes.sort(Comparator.reverseOrder());
        for (int index : indexes) {
            lines.remove(index);
        }
    }

    /**
     * 保留原行的缩进与"key:"部分，只替换值（并保留行尾注释）
     * <p>
     * 传进来的 {@code encoded} 已经是编码好的标量，这里不能再编码一次
     */
    private static String replaceValue(String origin, String encoded) {
        int sep = separatorIndex(origin);
        String head = origin.substring(0, sep + 1);
        String tail = origin.substring(sep + 1);
        return head + " " + encoded + trailingComment(tail);
    }

    /**
     * 取出值后面的行尾注释（形如 {@code value # 说明}），没有则返回空串
     * <p>
     * 值本身是带引号的字符串时，只从引号闭合之后开始找注释，
     * 避免把 {@code "a # b"} 这种值误当成注释截断
     */
    static String trailingComment(String valuePart) {
        String trimmed = valuePart.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return "";
        }
        int from = valuePart.indexOf(trimmed);
        char first = trimmed.charAt(0);
        if (first == '"' || first == '\'') {
            int end = trimmed.indexOf(first, 1);
            if (end > 0) {
                from += end + 1;
            }
        }
        int idx = valuePart.indexOf(" #", from);
        return idx < 0 ? "" : " " + valuePart.substring(idx + 1).trim();
    }

    // ==================================================================
    // 定位
    // ==================================================================

    /**
     * 解析出每个 key 行的行号、缩进、key 本身与"拍平后的完整路径"
     * <p>
     * 路径按缩进层级拼接，因此 {@code com.haruhi.botserver} 这种自带点的 key 也能得到正确路径；
     * 叶子节点（有值）不会成为后续行的父级
     */
    private static List<Node> parse(List<String> lines) {
        List<Node> nodes = new ArrayList<>();
        Deque<Node> stack = new ArrayDeque<>();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (!isKeyLine(raw)) {
                continue;
            }
            int indent = indentOf(raw);
            while (!stack.isEmpty() && stack.peek().indent >= indent) {
                stack.pop();
            }
            Node parent = stack.peek();
            String key = keyOf(raw);
            boolean leaf = hasValue(raw);
            nodes.add(new Node(i, indent, key,
                    parent == null ? key : parent.path + "." + key,
                    leaf, parent == null ? -1 : parent.line));
            if (!leaf) {
                stack.push(nodes.getLast());
            }
        }
        return nodes;
    }

    /**
     * 路径命中的所有叶子行（可能有重复行，需由调用方去重）
     */
    private static List<Node> leaves(List<String> lines, String path) {
        List<Node> result = new ArrayList<>();
        for (Node node : parse(lines)) {
            if (node.leaf && node.path.equals(path)) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * 路径对应的块节点（有子级的节点，如 {@code logging: level:}），找不到返回null
     */
    private static Node block(List<String> lines, String path) {
        for (Node node : parse(lines)) {
            if (!node.leaf && node.path.equals(path)) {
                return node;
            }
        }
        return null;
    }

    private static String join(String[] segments, int from, int to) {
        return String.join(".", Arrays.copyOfRange(segments, from, to));
    }

    /**
     * 一行 yml 的解析结果
     *
     * @param line   行下标（从0开始）
     * @param indent 缩进空格数
     * @param key    本行的 key（未拍平，可能是 {@code com.haruhi.botserver}）
     * @param path   拍平后的完整路径
     * @param leaf   本行是否带值（带值的是叶子，不能作为父级）
     * @param parent 父块所在行下标，-1表示顶层
     */
    private record Node(int line, int indent, String key, String path, boolean leaf, int parent) {

        /**
         * 是否是把整条路径挤在一行的写法（{@code server.port: 8090}），即 properties 风格的平铺行
         */
        boolean flat() {
            return leaf && parent < 0 && path.indexOf('.') >= 0;
        }
    }

    private static boolean isKeyLine(String raw) {
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("-")) {
            return false;
        }
        int sep = separatorIndex(raw);
        return sep > 0 && !keyOf(raw).isEmpty();
    }

    private static boolean hasValue(String raw) {
        int sep = separatorIndex(raw);
        if (sep < 0) {
            return false;
        }
        String value = raw.substring(sep + 1).trim();
        return !value.isEmpty() && !value.startsWith("#");
    }

    private static String keyOf(String raw) {
        int sep = separatorIndex(raw);
        String key = (sep < 0 ? raw : raw.substring(0, sep)).trim();
        if (key.length() >= 2
                && (key.charAt(0) == '"' || key.charAt(0) == '\'')
                && key.charAt(key.length() - 1) == key.charAt(0)) {
            key = key.substring(1, key.length() - 1);
        }
        return key;
    }

    /**
     * key 与 value 之间那个冒号的下标，找不到返回-1
     * <p>
     * key 可能带引号（{@code "a: b": c}），此时要跳过引号内的冒号
     */
    private static int separatorIndex(String raw) {
        int start = 0;
        while (start < raw.length() && raw.charAt(start) == ' ') {
            start++;
        }
        int from = start;
        if (start < raw.length() && (raw.charAt(start) == '\'' || raw.charAt(start) == '"')) {
            char quote = raw.charAt(start);
            int i = start + 1;
            while (i < raw.length()) {
                char c = raw.charAt(i);
                if (c == quote) {
                    if (quote == '\'' && i + 1 < raw.length() && raw.charAt(i + 1) == '\'') {
                        // 单引号里连写两个单引号表示一个单引号
                        i += 2;
                        continue;
                    }
                    break;
                }
                i++;
            }
            from = i + 1;
        }
        return raw.indexOf(':', from);
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
