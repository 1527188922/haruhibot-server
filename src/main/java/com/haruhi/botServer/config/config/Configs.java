package com.haruhi.botServer.config.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.haruhi.botServer.config.util.PropertiesFileUtil;
import com.haruhi.botServer.config.util.YamlFileUtil;
import com.haruhi.botServer.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 配置读取入口（静态可用）
 * <p>
 * 与 {@link PropertiesFileUtil} 的分工：本类负责<b>内存快照</b>，文件读写与热更新通知在
 * {@link com.haruhi.botServer.config.service.ConfigHub}。
 * <p>
 * <b>设计要点</b>
 * <ul>
 *     <li>快照是不可变 Map，读取无锁、任意时机可调用（静态块、静态方法、构造器中都安全）</li>
 *     <li>类加载时即完成首次加载，因此不依赖 Spring 容器，也就没有"bean未创建前读到null"的问题</li>
 *     <li>配置缺省时回落到 {@link ConfigKey#getDefaultValue()}，<b>永远不会返回null</b></li>
 *     <li>热更新时整体替换快照引用，读到的要么是旧快照要么是新快照，不会读到半成品</li>
 * </ul>
 * 注意：不要在 {@code static final} 字段初始化时取值（那会把启动瞬间的值永久固化），
 * 请在方法内部调用 {@code Configs.getXxx(...)}。
 */
@Slf4j
public class Configs {

    private Configs() {
    }

    private static final String APP_NAME = "haruhibot";

    /** 配置文件所在目录，默认取程序目录下的 config；单元测试可临时覆盖 */
    private static volatile String configDir;

    /** 不可变快照：配置key -> 值 */
    private static volatile Map<String, String> snapshot = Collections.emptyMap();

    /** key -> 值来源，仅用于前端展示 */
    private static volatile Map<String, ConfigSource> sources = Collections.emptyMap();

    /** 值来源 */
    public enum ConfigSource {
        /** 来自 ./config/*.properties */
        FILE,
        /** 未配置，使用声明中的默认值 */
        DEFAULT
    }

    /**
     * 配置目录，默认 {@code 程序目录/config}
     */
    public static String configDir() {
        String dir = configDir;
        if (dir == null) {
            dir = FileUtil.getConfigDir();
        }
        return dir;
    }

    /**
     * 仅在单元测试中使用：把配置目录指向临时目录并重新加载
     */
    public static synchronized void useConfigDirForTest(String dir) {
        configDir = dir;
        reloadAll();
    }

    /**
     * 仅在单元测试中使用：恢复默认配置目录并重新加载
     */
    public static synchronized void resetConfigDirForTest() {
        configDir = null;
        reloadAll();
    }

    // ==================================================================
    // 初始化与重载（由 ConfigHub 调用）
    // ==================================================================

    static {
        // 类首次被访问时即完成加载：不依赖 Spring，也不会有"容器就绪前读到默认值"的空窗期
        try {
            reloadAll();
            log.info("配置加载完成，共{}项，配置文件目录：{}", snapshot.size(), configDir());
        } catch (Throwable e) {
            log.error("配置加载异常，将使用声明中的默认值", e);
        }
    }

    /**
     * 从磁盘重新加载全部配置文件，整体替换快照（不含通知，通知由 ConfigHub 负责）
     */
    public static synchronized void reloadAll() {
        Map<String, String> next = new HashMap<>();
        Map<String, ConfigSource> nextSource = new HashMap<>();

        // 1. 声明中的默认值打底
        for (ConfigKey key : ConfigKey.values()) {
            next.put(key.getKey(), key.getDefaultValue());
            nextSource.put(key.getKey(), ConfigSource.DEFAULT);
        }
        // 2. 打包在jar内的默认配置文件（首次运行、外置文件缺失时兜底）
        apply(next, nextSource, loadClasspathConfigs(), ConfigSource.DEFAULT);
        // 3. 外置配置文件（唯一真源，优先级最高）
        for (ConfigFile file : ConfigFile.values()) {
            Map<String, String> loaded = loadExternal(file);
            apply(next, nextSource, loaded, ConfigSource.FILE);
            append(next, loaded);
        }
        // 4. 兜底：文件里出现、但声明中没有的key，只有存在同名系统属性时才采纳
        applySystemProperties(next, nextSource);
        snapshot = Collections.unmodifiableMap(next);
        sources = Collections.unmodifiableMap(nextSource);
    }

    /**
     * 只重新加载某一个配置文件，其余保持不变
     */
    public static synchronized void reloadFile(ConfigFile file) {
        Map<String, String> next = new HashMap<>(snapshot);
        Map<String, ConfigSource> nextSource = new HashMap<>(sources);

        // 先移除该文件声明的所有key，这样文件里删掉的配置能回落到默认值
        for (ConfigKey key : ConfigKey.of(file)) {
            next.remove(key.getKey());
            nextSource.remove(key.getKey());
        }
        Map<String, String> loaded = loadExternal(file);
        apply(next, nextSource, loaded, ConfigSource.FILE);
        append(next, loaded);

        snapshot = Collections.unmodifiableMap(next);
        sources = Collections.unmodifiableMap(nextSource);
    }

    // ==================================================================
    // 文件加载（properties / yml 统一入口）
    // ==================================================================

    /**
     * 读取外置配置文件，按扩展名分派到 properties 或 yaml 解析器
     */
    static Map<String, String> loadExternal(ConfigFile file) {
        if (file.isYaml()) {
            return YamlFileUtil.load(new File(configDir(), file.getFileName()));
        }
        return PropertiesFileUtil.load(file.getFileName());
    }

    /**
     * 读取jar内默认配置文件
     */
    private static Map<String, String> loadClasspathFile(ConfigFile file) {
        if (file.isYaml()) {
            return YamlFileUtil.loadFromClasspath(file.getFileName());
        }
        ClassPathResource resource = new ClassPathResource("config/" + file.getFileName());
        if (!resource.exists()) {
            return Collections.emptyMap();
        }
        Properties properties = new Properties();
        try (InputStream in = resource.getInputStream()) {
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("加载内置配置文件异常 {}", file.getFileName(), e);
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return map;
    }

    /**
     * 把 ./config 下的全部配置（properties + yml）注入 Spring Environment
     * <p>
     * 由 {@link ConfigsEnvironmentInitializer} 在容器 refresh 之前调用，
     * 这样 {@code @ConditionalOnProperty}、{@code @Value} 与 {@link Configs} 读到的是同一份配置，
     * 不依赖 {@code spring.config.import} 的路径解析。
     */
    public static Map<String, Object> loadIntoEnvironment() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (ConfigFile file : ConfigFile.values()) {
            result.putAll(loadClasspathFile(file));
        }
        for (ConfigFile file : ConfigFile.values()) {
            result.putAll(loadExternal(file));
        }
        // 保证关键项即便文件缺失也有值（例如端口）
        result.putIfAbsent(ConfigKey.SERVER_PORT.getKey(), ConfigKey.SERVER_PORT.getDefaultValue());
        return result;
    }

    // ==================================================================
    // 读取
    // ==================================================================

    /**
     * 取字符串。未配置时返回 defaultValue（不会返回null）
     */
    public static String getStr(ConfigKey key, String defaultValue) {
        String value = lookup(key);
        return StringUtils.isNotBlank(value) ? value.trim() : defaultValue;
    }

    /**
     * 取字符串，未配置时返回声明中的默认值
     */
    public static String getStr(ConfigKey key) {
        return getStr(key, key.getDefaultValue());
    }

    /**
     * 严格取字符串：配置项存在时原样返回（包括空串），只有真正缺失时才用默认值
     * <p>
     * 用于"显式留空"是有意义的配置项，例如把密码/密钥/token清空
     */
    public static String getStrStrict(ConfigKey key, String defaultValue) {
        String value = lookup(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 取原始值，未配置或未声明时返回null。用于需要区分"空串"与"未配置"的场景
     */
    public static String getRaw(ConfigKey key) {
        return lookup(key);
    }

    public static int getInt(ConfigKey key, int defaultValue) {
        String value = getStr(key, null);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置项[{}]的值不是整数，使用默认值{}，当前值：{}", key.getKey(), defaultValue, value);
            return defaultValue;
        }
    }

    public static int getInt(ConfigKey key) {
        return getInt(key, parseInt(key.getDefaultValue(), 0));
    }

    public static long getLong(ConfigKey key, long defaultValue) {
        String value = getStr(key, null);
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("配置项[{}]的值不是整数，使用默认值{}，当前值：{}", key.getKey(), defaultValue, value);
            return defaultValue;
        }
    }

    public static boolean getBool(ConfigKey key, boolean defaultValue) {
        String value = getStr(key, null);
        return StringUtils.isBlank(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public static boolean getBool(ConfigKey key) {
        return getBool(key, Boolean.parseBoolean(key.getDefaultValue()));
    }

    /**
     * 取列表，按逗号（中英文）、换行、空格分割
     */
    public static List<String> getStrList(ConfigKey key) {
        return getList(key, String.class, Collections.emptyList());
    }

    public static <T> List<T> getList(ConfigKey key, Class<T> tClass, List<T> defaultList) {
        String value = getStr(key, null);
        if (StringUtils.isBlank(value)) {
            return defaultList;
        }
        String[] split = value.split("[,，\\s]+");
        List<T> list = new ArrayList<>(split.length);
        for (String s : split) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                T convert = convert(s.trim(), tClass);
                if (convert != null) {
                    list.add(convert);
                }
            } catch (Exception e) {
                log.warn("配置项[{}]的值[{}]无法转换为{}", key.getKey(), s, tClass.getSimpleName());
            }
        }
        return list;
    }

    public static <T> T getJson(ConfigKey key, Class<T> tClass) {
        String value = getStr(key, null);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return JSON.parseObject(value, tClass);
        } catch (Exception e) {
            log.warn("配置项[{}]的值不是合法JSON", key.getKey(), e);
            return null;
        }
    }

    public static <T> List<T> getJsonList(ConfigKey key, Class<T> tClass) {
        String value = getStr(key, null);
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        try {
            List<T> list = JSON.parseObject(value, new TypeReference<List<T>>() {
            });
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.warn("配置项[{}]的值不是合法JSON数组", key.getKey(), e);
            return Collections.emptyList();
        }
    }

    // ==================================================================
    // 写入（仅更新内存快照，落盘由 ConfigHub 负责）
    // ==================================================================

    static synchronized void put(ConfigKey key, String value) {
        update(key, value, ConfigSource.FILE);
    }

    /**
     * 只覆盖内存快照，不写文件
     * <p>
     * 用于程序运行期自动生成的值、以及单元测试；正式配置修改请走
     * {@link com.haruhi.botServer.config.service.ConfigHub#save}
     */
    public static void override(ConfigKey key, String value) {
        update(key, value, ConfigSource.FILE);
    }

    static synchronized void remove(ConfigKey key) {
        update(key, key.getDefaultValue(), ConfigSource.DEFAULT);
    }

    /**
     * 由程序自动写入某个配置项（写文件 + 刷新快照）
     * <p>
     * 适用于"运行期自动获取到、需要持久化"的值，例如 b站接口返回的 ticket；
     * yml 类配置不支持写入。
     *
     * @return 是否写入成功
     */
    public static boolean save(ConfigKey key, String value) {
        if (key.getFile().isYaml()) {
            log.warn("配置项[{}]所属文件为yml，不支持程序写入", key.getKey());
            return false;
        }
        String v = value == null ? "" : value;
        try {
            PropertiesFileUtil.save(key.getFile().getFileName(), key.getKey(), v);
        } catch (Exception e) {
            log.error("程序写入配置失败 key:{}", key.getKey(), e);
            return false;
        }
        reloadFile(key.getFile());
        return true;
    }

    private static void update(ConfigKey key, String value, ConfigSource source) {
        Map<String, String> next = new HashMap<>(snapshot);
        Map<String, ConfigSource> nextSource = new HashMap<>(sources);
        next.put(key.getKey(), value);
        nextSource.put(key.getKey(), source);
        snapshot = Collections.unmodifiableMap(next);
        sources = Collections.unmodifiableMap(nextSource);
    }

    // ==================================================================
    // 辅助
    // ==================================================================

    /**
     * 值是否来自外置配置文件（false表示正在使用声明中的默认值）
     */
    public static boolean isConfigured(ConfigKey key) {
        return source(key) == ConfigSource.FILE;
    }

    public static ConfigSource source(ConfigKey key) {
        ConfigSource configSource = sources.get(key.getKey());
        return configSource == null ? ConfigSource.DEFAULT : configSource;
    }

    public static Map<String, String> snapshot() {
        return snapshot;
    }

    /**
     * 按 原始key -> {应用名}.key 的顺序查找
     */
    private static String lookup(ConfigKey key) {
        Map<String, String> current = snapshot;
        String value = current.get(key.getKey());
        if (value != null) {
            return value;
        }
        return current.get(APP_NAME + "." + key.getKey());
    }

    private static void apply(Map<String, String> target, Map<String, ConfigSource> targetSource,
                              Map<String, String> loaded, ConfigSource source) {
        for (ConfigKey key : ConfigKey.values()) {
            String value = loaded.get(key.getKey());
            if (value != null) {
                target.put(key.getKey(), value);
                targetSource.put(key.getKey(), source);
                continue;
            }
            // 支持 haruhibot.xxx=yyy 这种带应用名前缀的写法
            value = loaded.get(APP_NAME + "." + key.getKey());
            if (value != null) {
                target.put(key.getKey(), value);
                targetSource.put(key.getKey(), source);
            }
        }
    }

    private static void append(Map<String, String> target, Map<String, String> loaded) {
        for (Map.Entry<String, String> entry : loaded.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(APP_NAME + ".")) {
                target.putIfAbsent(key.substring(APP_NAME.length() + 1), entry.getValue());
            }
        }
    }

    private static void applySystemProperties(Map<String, String> target, Map<String, ConfigSource> targetSource) {
        for (ConfigKey key : ConfigKey.values()) {
            if (targetSource.get(key.getKey()) == ConfigSource.FILE) {
                continue;
            }
            String value = System.getProperty(key.getKey());
            if (value == null) {
                value = System.getProperty(APP_NAME + "." + key.getKey());
            }
            if (value != null) {
                target.put(key.getKey(), value);
                targetSource.put(key.getKey(), ConfigSource.FILE);
            }
        }
    }

    /**
     * 加载jar内 config/ 下的默认配置文件
     */
    private static Map<String, String> loadClasspathConfigs() {
        Map<String, String> map = new LinkedHashMap<>();
        for (ConfigFile file : ConfigFile.values()) {
            loadClasspathFile(file).forEach(map::putIfAbsent);
        }
        return map;
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(String value, Class<T> tClass) throws Exception {
        if (tClass == String.class) {
            return (T) value;
        }
        if (tClass == Integer.class || tClass == int.class) {
            return (T) Integer.valueOf(value);
        }
        if (tClass == Long.class || tClass == long.class) {
            return (T) Long.valueOf(value);
        }
        if (tClass == Double.class || tClass == double.class) {
            return (T) Double.valueOf(value);
        }
        if (tClass == Float.class || tClass == float.class) {
            return (T) Float.valueOf(value);
        }
        if (tClass == Boolean.class || tClass == boolean.class) {
            return (T) Boolean.valueOf(value);
        }
        if (tClass == Short.class || tClass == short.class) {
            return (T) Short.valueOf(value);
        }
        if (tClass == Byte.class || tClass == byte.class) {
            return (T) Byte.valueOf(value);
        }
        try {
            return (T) tClass.getMethod("valueOf", String.class).invoke(null, value);
        } catch (NoSuchMethodException e) {
            return tClass.getConstructor(String.class).newInstance(value);
        }
    }

    /**
     * 外置配置文件目录中实际存在的文件（用于前端展示与变更监听）
     */
    public static List<File> existingFiles() {
        List<File> files = new ArrayList<>();
        for (ConfigFile file : ConfigFile.values()) {
            File f = fileOf(file);
            if (f.isFile()) {
                files.add(f);
            }
        }
        return files;
    }

    /**
     * 某个配置项在磁盘上的文件
     */
    public static File fileOf(ConfigKey key) {
        return fileOf(key.getFile());
    }

    public static File fileOf(ConfigFile file) {
        return new File(configDir(), file.getFileName());
    }
}
