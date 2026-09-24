package com.haruhi.botserver.configuration;

import com.haruhi.botserver.configuration.metadata.ConfigFile;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.metadata.ConfigType;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.service.ConfigApplier;
import com.haruhi.botserver.configuration.service.ConfigChange;
import com.haruhi.botserver.configuration.service.ConfigHub;
import com.haruhi.botserver.configuration.store.PropertiesFileUtil;
import com.haruhi.botserver.configuration.store.YamlFileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置读写与热更新机制测试
 * <p>
 * 不启动 Spring 容器，直接验证 Configs 快照、PropertiesFileUtil / YamlFileUtil 文件读写
 * 与 ConfigHub 的"写文件 + 刷快照 + 按key通知"链路。
 */
class ConfigsTest {

    /** 程序目录（临时），application*.yml 放这里 */
    @TempDir
    Path baseDir;

    /** properties 目录，即 {baseDir}/config */
    private Path configDir;

    private ConfigHub configHub;

    @BeforeEach
    void setUp() {
        configDir = baseDir.resolve("config");
        Configs.useConfigDirForTest(baseDir.toString());
        configHub = new ConfigHub();
        // ConfigHub 只依赖 ApplicationContext 查找 ConfigApplier，这里用没有订阅者的上下文
        ReflectionTestUtils.setField(configHub, "applicationContext", emptyApplicationContext());
    }

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    /**
     * 只需要 getBeansOfType 返回空集合的最小 ApplicationContext
     */
    private static ApplicationContext emptyApplicationContext() {
        return (ApplicationContext) Proxy.newProxyInstance(
                ConfigsTest.class.getClassLoader(),
                new Class<?>[]{ApplicationContext.class},
                (proxy, method, args) -> {
                    if ("getBeansOfType".equals(method.getName())) {
                        return Collections.emptyMap();
                    }
                    if ("toString".equals(method.getName())) {
                        return "EmptyApplicationContext";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private Path propertyFile(ConfigFile file) {
        return configDir.resolve(file.getFileName());
    }

    private Path applicationFile(ConfigFile file) {
        return baseDir.resolve(file.getFileName());
    }

    // ==================================================================
    // 声明默认值
    // ==================================================================

    @Test
    void 未配置时使用声明默认值且不会返回null() {
        assertEquals("5", Configs.getStr(ConfigKey.WS_MAX_CONNECTIONS));
        assertEquals(5, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertFalse(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS));
        assertEquals(Configs.ConfigSource.DEFAULT, Configs.source(ConfigKey.WS_MAX_CONNECTIONS));
        assertTrue(Configs.getBool(ConfigKey.SAME_MACHINE_QQCLIENT));
        // 未配置且没有默认值的项返回调用方给的默认值，而不是抛异常
        assertNull(Configs.getStr(ConfigKey.WS_ACCESS_TOKEN, null));
    }

    @Test
    void 数据源配置从databaseProperties读取() throws IOException {
        Files.writeString(propertyFile(ConfigFile.DATABASE), """
                spring.datasource.dynamic.datasource.master.url=jdbc:sqlite:D:\\my\\bot\\db\\x.db
                spring.datasource.dynamic.datasource.master.driver-class-name=org.sqlite.JDBC
                spring.datasource.dynamic.datasource.master.druid.validation-query=SELECT 1
                spring.datasource.dynamic.datasource.master.druid.filters=stat
                spring.datasource.dynamic.datasource.master.druid.test-on-borrow=false
                spring.datasource.dynamic.datasource.master.druid.test-on-return=false
                spring.datasource.dynamic.datasource.master.druid.pool-prepared-statements=false
                spring.datasource.dynamic.datasource.master.druid.max-active=5
                """, StandardCharsets.UTF_8);

        Configs.reloadFile(ConfigFile.DATABASE);

        assertEquals("jdbc:sqlite:D:\\my\\bot\\db\\x.db", Configs.getStr(ConfigKey.DATABASE_URL));
        assertEquals("org.sqlite.JDBC", Configs.getStr(ConfigKey.DATABASE_DRIVER_CLASS_NAME));
        assertEquals("SELECT 1", Configs.getStr(ConfigKey.DATABASE_DRUID_VALIDATION_QUERY));
        assertEquals("stat", Configs.getStr(ConfigKey.DATABASE_DRUID_FILTERS));
        assertFalse(Configs.getBool(ConfigKey.DATABASE_DRUID_TEST_ON_BORROW));
        assertFalse(Configs.getBool(ConfigKey.DATABASE_DRUID_TEST_ON_RETURN));
        assertFalse(Configs.getBool(ConfigKey.DATABASE_DRUID_POOL_PREPARED_STATEMENTS));
        assertEquals(5, Configs.getInt(ConfigKey.DATABASE_DRUID_MAX_ACTIVE));
    }

    @Test
    void 聊天记录压缩配置独立成文件() {
        assertEquals(ConfigFile.CHAT_RECORD, ConfigKey.CHAT_RECORD_RAW_COMPRESS.getFile());
        assertEquals("db.chat_extend.raw_compress", ConfigKey.CHAT_RECORD_RAW_COMPRESS.getKey());
        assertTrue(Configs.getBool(ConfigKey.CHAT_RECORD_RAW_COMPRESS));
        // 数据库文件里只放数据源相关配置
        assertFalse(ConfigKey.of(ConfigFile.DATABASE).isEmpty());
        assertTrue(ConfigKey.of(ConfigFile.DATABASE).stream()
                .allMatch(e -> e.getKey().startsWith("spring.datasource.")));
    }

    @Test
    void agefans地址归属站点地址文件() {
        assertEquals(ConfigFile.URL, ConfigKey.URL_CONF_AGEFANS.getFile());
        assertEquals("url_conf.agefans", ConfigKey.URL_CONF_AGEFANS.getKey());
        // 识图文件里只有识图引擎相关配置
        assertTrue(ConfigKey.of(ConfigFile.SEARCH_IMG).stream()
                .allMatch(e -> e.getKey().startsWith("searchimg.saucenao.")));
    }

    @Test
    void 敏感值脱敏() {
        String masked = ConfigHub.mask("QBgu5c4xfA4_6Ky8A_QnO");
        assertTrue(masked.startsWith("****"));
        assertTrue(masked.endsWith("QnO"));
        assertEquals("****", ConfigHub.mask("ab"));
        assertNull(ConfigHub.mask(null));
        assertTrue(ConfigApplier.class.isInterface());
    }

    @Test
    void 配置项声明完整且key唯一() {
        for (ConfigFile file : ConfigFile.values()) {
            assertFalse(ConfigKey.of(file).isEmpty(), file.getFileName() + " 没有声明任何配置项");
        }
        for (ConfigKey key : ConfigKey.values()) {
            // 同一属性名可以在多个 profile 文件里各声明一次，此时按"文件+key"定位
            assertTrue(ConfigKey.of(key.getKey()) != null, "key无法反查: " + key.getKey());
            assertEquals(key, ConfigKey.of(key.getFile(), key.getKey()), "key无法按文件反查: " + key.getKey());
        }
    }

    @Test
    void 未声明的key无法反查() {
        assertNull(ConfigKey.of("bot.access_token"));
        assertNull(ConfigKey.of("db.sql_cache"));
        assertNull(ConfigKey.of("not.exists"));
        assertFalse(ConfigKey.isAmbiguous("not.exists"));
    }

    // ==================================================================
    // properties 读写
    // ==================================================================

    @Test
    void 保存配置后立即生效并写入对应文件() throws IOException {
        configHub.save(ConfigKey.WS_MAX_CONNECTIONS, "12");
        configHub.save(ConfigKey.BOT_SUPERUSERS, "10001,10002");

        assertEquals(12, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertTrue(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS));
        assertEquals(List.of(10001L, 10002L), Configs.getList(ConfigKey.BOT_SUPERUSERS, Long.class, List.of()));

        Map<String, String> ws = PropertiesFileUtil.load(ConfigFile.WEBSOCKET.getFileName());
        assertEquals("12", ws.get("bot.ws.max_connections"));
        Map<String, String> bot = PropertiesFileUtil.load(ConfigFile.BOT.getFileName());
        assertEquals("10001,10002", bot.get("bot.superusers"));
    }

    @Test
    void 写properties保留注释与已有顺序() throws IOException {
        Files.writeString(propertyFile(ConfigFile.WEBSOCKET), """
                ### Websocket配置

                # 认证token
                bot.ws.access_token=
                # 最大连接数
                bot.ws.max_connections=5
                """, StandardCharsets.UTF_8);

        configHub.save(ConfigKey.WS_MAX_CONNECTIONS, "9");

        String after = Files.readString(propertyFile(ConfigFile.WEBSOCKET), StandardCharsets.UTF_8);
        assertTrue(after.contains("### Websocket配置"), "文件头注释应保留");
        assertTrue(after.contains("# 最大连接数"), "key上方的注释应保留");
        assertTrue(after.contains("bot.ws.max_connections=9"), "值应被替换");
        assertTrue(after.contains("# 认证token"), "其他项的注释应保留");
        assertTrue(after.indexOf("bot.ws.access_token") < after.indexOf("bot.ws.max_connections"));
    }

    @Test
    void 新增key追加到properties末尾() throws IOException {
        Files.writeString(propertyFile(ConfigFile.BOT), "bot.switch.disable_group=false\n", StandardCharsets.UTF_8);

        configHub.save(ConfigKey.BOT_SWITCH_GROUP_INCREASE, "false");

        String after = Files.readString(propertyFile(ConfigFile.BOT), StandardCharsets.UTF_8);
        assertTrue(after.contains("bot.switch.disable_group=false"));
        assertTrue(after.contains("bot.switch.group_increase=false"));
    }

    @Test
    void 多行值与特殊字符可正确往返() throws IOException {
        configHub.save(ConfigKey.URL_CONF_BT_SEARCH, "http://a.com\nhttp://b.com");
        String text = Files.readString(propertyFile(ConfigFile.URL), StandardCharsets.UTF_8);
        assertTrue(text.contains("url_conf.bt_search=http://a.com\\nhttp://b.com"), "换行应转义: " + text);

        Configs.reloadFile(ConfigFile.URL);
        assertEquals("http://a.com\nhttp://b.com", Configs.getStr(ConfigKey.URL_CONF_BT_SEARCH));
    }
    @Test
    void 注释掉的key与未声明的key都不会进入快照() throws IOException {
        Files.writeString(propertyFile(ConfigFile.AI), """
                # ds.api.key=sk-should-be-ignored
                ds.api.base_url=https://example.com
                unknown.key=whatever
                """, StandardCharsets.UTF_8);

        Configs.reloadFile(ConfigFile.AI);

        assertEquals("https://example.com", Configs.getStr(ConfigKey.DEEP_SEEK_API_BASE_URL));
        assertEquals("", Configs.getStr(ConfigKey.DEEP_SEEK_API_KEY, ""));
        assertFalse(Configs.isConfigured(ConfigKey.DEEP_SEEK_API_KEY));
        assertNull(Configs.snapshot().get("unknown.key"));
    }

    @Test
    void 类型校验拒绝非法值() {
        assertEquals("必须是整数", ConfigType.INT.validate("abc"));
        assertEquals("只能是 true 或 false", ConfigType.BOOL.validate("yes"));
        assertNull(ConfigType.INT.validate("42"));
        assertNull(ConfigType.BOOL.validate("TRUE"));
        assertNull(ConfigType.STRING.validate("任意文本"));
    }

    // ==================================================================
    // yml 读写（application*.yml）
    // ==================================================================

    @Test
    void applicationYml会被拍平后进入快照() throws IOException {
        Files.writeString(applicationFile(ConfigFile.APPLICATION), """
                # 应用主配置
                server:
                  # http端口
                  port: 18080

                logging:
                  level:
                    com.haruhi.botserver: debug
                """, StandardCharsets.UTF_8);

        Configs.reloadFile(ConfigFile.APPLICATION);

        assertEquals(18080, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Configs.isConfigured(ConfigKey.SERVER_PORT));
        assertEquals(ConfigFile.APPLICATION, ConfigKey.SERVER_PORT.getFile());
        assertEquals("debug", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    @Test
    void 写applicationYml原地改值且保留注释() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, """
                # 应用主配置
                server:
                  # http端口
                  port: 8090

                logging:
                  level:
                    com.haruhi.botserver: debug
                """, StandardCharsets.UTF_8);

        configHub.save(ConfigKey.SERVER_PORT, "18081");

        // 快照立即生效；Spring 侧需要重启才会重新绑定端口
        assertEquals(18081, Configs.getInt(ConfigKey.SERVER_PORT));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("# 应用主配置"), "文件头注释应保留");
        assertTrue(after.contains("# http端口"), "key上方的注释应保留");
        assertTrue(after.contains("port: 18081"), "值应被替换: " + after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), "其它配置应保留");
        assertTrue(after.contains("  port: 18081"), "缩进应保持不变");
    }

    @Test
    void 写applicationYml支持追加不存在的key() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        // 已有 server 块，但没有 server.port：应插入到 server 块内，而不是文件末尾
        Files.writeString(file, """
                server:
                  servlet:
                    context-path: /api

                logging:
                  level:
                    com.haruhi.botserver: debug
                """, StandardCharsets.UTF_8);

        configHub.save(ConfigKey.SERVER_PORT, "8095");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("  port: 8095"), "应插入到 server 块内且缩进正确: " + after.replace("\n", "|"));
        // 关键：不能把后面的 logging 块吞进 server 下
        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(8095, Configs.getInt(ConfigKey.SERVER_PORT), "重读后应为8095，文件: " + after.replace("\n", "|"));
        assertEquals("debug", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    @Test
    void 日志级别声明在唯一的applicationYml里() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, """
                logging:
                  level:
                    com.haruhi.botserver: debug
                """, StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        // 不再有 dev/prod 两个文件：同一个 key 只有一处声明，也就没有"歧义"
        assertEquals(ConfigFile.APPLICATION, ConfigKey.LOGGING_LEVEL.getFile());
        assertFalse(ConfigKey.isAmbiguous("logging.level.com.haruhi.botserver"));
        assertEquals("debug", Configs.getStr(ConfigKey.LOGGING_LEVEL));

        configHub.save(ConfigKey.LOGGING_LEVEL, "warn");
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("com.haruhi.botserver: warn"));
    }

    @Test
    void yml标量会按需加引号() {
        assertEquals("0/15 * 0-7 * * ? *", YamlFileUtil.encodeScalar("0/15 * 0-7 * * ? *"));
        assertEquals("\"true\"", YamlFileUtil.encodeScalar("true"));
        assertEquals("\"8090\"", YamlFileUtil.encodeScalar("8090"));
        assertEquals("\"\"", YamlFileUtil.encodeScalar(""));
        assertEquals("http://a.com", YamlFileUtil.encodeScalar("http://a.com"));
    }

    @Test
    void yml里的字符串值会被加引号而数字不会() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "server:\n  port: 8090\n", StandardCharsets.UTF_8);

        // INT 类型写成裸值
        configHub.save(ConfigKey.SERVER_PORT, "8091");
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("port: 8091"));
        assertEquals(8091, Configs.getInt(ConfigKey.SERVER_PORT));

        // STRING 类型原样写入（值本身像布尔/数字时才加引号）
        configHub.save(ConfigKey.LOGGING_LEVEL, "trace");
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("com.haruhi.botserver: trace"));
        assertEquals("trace", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    @Test
    void yml里的数字样式字符串可正确往返() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "server:\n  port: 8090\n", StandardCharsets.UTF_8);

        YamlFileUtil.save(file.toFile(), "server.port", "8090");
        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("port: \"8090\""));
    }

    @Test
    void 重置yml配置是把默认值写回该行而不是删掉key() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, """
                server:
                  port: 18080
                logging:
                  level:
                    com.haruhi.botserver: debug
                """, StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(18080, Configs.getInt(ConfigKey.SERVER_PORT));

        configHub.reset(ConfigKey.SERVER_PORT);

        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Configs.isConfigured(ConfigKey.SERVER_PORT), "key应保留在文件里");
        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("port: 8090"), "应写回默认值: " + after);
        assertFalse(after.contains("18080"), after);
        assertEquals(1, count(after, "port:"), "不应新增行: " + after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), "其它配置应保留: " + after);
    }

    @Test
    void 保存yml的新key写嵌套结构且重复保存不会新增行() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "logging:\n  level:\n    com.haruhi.botserver: debug\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        configHub.save(ConfigKey.SERVER_PORT, "18080");
        configHub.save(ConfigKey.SERVER_PORT, "18081");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(after.contains("server.port"), "不应写成 server.port 这种properties风格的行: " + after);
        assertEquals(1, count(after, "port: 18081"), "重复保存不应新增行: " + after);
        assertTrue(after.contains("server:\n  port: 18081"), "应是 yml 嵌套结构: " + after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), "其它配置应保留: " + after);

        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(18081, Configs.getInt(ConfigKey.SERVER_PORT));
    }

    @Test
    void 保存yml里带点的叶子key不会重复追加() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "logging:\n  level:\n    com.haruhi.botserver: debug\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        configHub.save(ConfigKey.LOGGING_LEVEL, "trace");
        configHub.save(ConfigKey.LOGGING_LEVEL, "warn");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(1, count(after, "com.haruhi.botserver"), "带点的key只应有一行: " + after);
        assertTrue(after.contains("    com.haruhi.botserver: warn"), "缩进与值应正确: " + after);

        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals("warn", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    @Test
    void 保存yml会清理历史遗留的点分重复行并改写为嵌套结构() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        // 模拟旧版本bug的产物：文件末尾不断追加的点分行
        Files.writeString(file, """
                logging:
                  level:
                    com.haruhi.botserver: debug

                server.port: 8091

                server.port: 8090
                """, StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        configHub.save(ConfigKey.SERVER_PORT, "18080");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(after.contains("server.port"), "点分行应被改写为嵌套结构: " + after);
        assertEquals(1, count(after, "port: 18080"), "重复行应被清理: " + after);
        assertTrue(after.contains("server:\n  port: 18080"), after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), after);

        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(18080, Configs.getInt(ConfigKey.SERVER_PORT));
    }

    @Test
    void 重置yml里不存在的key会补上默认值() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "logging:\n  level:\n    com.haruhi.botserver: debug\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);
        assertFalse(Configs.isConfigured(ConfigKey.SERVER_PORT));

        configHub.reset(ConfigKey.SERVER_PORT);

        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));
        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(1, count(after, "port: 8090"), "不应新增多行: " + after);
        assertTrue(after.contains("server:\n  port: 8090"), "应写成 yml 嵌套结构: " + after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), after);
    }

    @Test
    void 重置yml会清掉点分重复行并写回一个嵌套行() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, """
                logging:
                  level:
                    com.haruhi.botserver: debug

                server.port: 18080

                server.port: 18081
                """, StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        configHub.reset(ConfigKey.SERVER_PORT);

        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Configs.isConfigured(ConfigKey.SERVER_PORT), "key应保留在文件里");
        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(after.contains("18080") || after.contains("18081"), after);
        assertEquals(1, count(after, "port:"), "点分重复行应被清理成一行: " + after);
        assertTrue(after.contains("server:\n  port: 8090"), after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), after);
    }

    @Test
    void 批量保存同一个yml文件的多个key不会互相覆盖() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        configHub.saveAll(Map.of(
                ConfigKey.SERVER_PORT, "18080",
                ConfigKey.LOGGING_LEVEL, "warn"));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("port: 18080"), after);
        assertTrue(after.contains("com.haruhi.botserver: warn"), after);

        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(18080, Configs.getInt(ConfigKey.SERVER_PORT));
        assertEquals("warn", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    private static int count(String text, String part) {
        int total = 0;
        int index = text.indexOf(part);
        while (index >= 0) {
            total++;
            index = text.indexOf(part, index + part.length());
        }
        return total;
    }

    @Test
    void 自动修复被写坏的点分重复行() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, """
                logging:
                  level:
                    com.haruhi.botserver: debug

                server.port: 8091

                server.port: 8090

                server.port: 8091
                """, StandardCharsets.UTF_8);

        assertTrue(YamlFileUtil.repairDuplicateKeys(file.toFile()), "应识别出重复项并修复");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(1, count(after, "port:"), "重复行应只剩一行: " + after);
        assertEquals(1, after.lines().filter(line -> line.equals("server:")).count(), after);
        assertTrue(after.contains("server:\n  port: 8091"), "保留下来的点分行应改写成嵌套结构: " + after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), after);

        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(8091, Configs.getInt(ConfigKey.SERVER_PORT), "应保留最后一行的值");
        assertFalse(YamlFileUtil.repairDuplicateKeys(file.toFile()), "没有重复项时不应改动文件");
    }

    @Test
    void 自动修复嵌套与点分混用的重复行() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, """
                logging:
                  level:
                    com.haruhi.botserver: debug

                logging.level.com.haruhi.botserver: trace

                logging.level.com.haruhi.botserver: warn
                """, StandardCharsets.UTF_8);

        assertTrue(YamlFileUtil.repairDuplicateKeys(file.toFile()));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(1, count(after, "com.haruhi.botserver"), "重复行应只剩一行: " + after);
        assertTrue(after.contains("    com.haruhi.botserver: warn"), "应保留最后一行并写回嵌套结构: " + after);

        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals("warn", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    @Test
    void 配置文件类型判断与加载顺序() {
        assertTrue(ConfigFile.APPLICATION.isYaml());
        assertTrue(ConfigFile.APPLICATION.isSpringApplicationFile());
        assertFalse(ConfigFile.BOT.isYaml());
        assertFalse(ConfigFile.BOT.isSpringApplicationFile());
        // 只有一份 application.yml：不再有 application-dev / application-prod
        assertEquals(1, java.util.Arrays.stream(ConfigFile.values())
                .filter(ConfigFile::isSpringApplicationFile).count());

        // application.yml 先加载，./config/*.properties 覆盖它
        assertTrue(ConfigFile.APPLICATION.getOrder() < ConfigFile.BOT.getOrder());
        ConfigFile[] order = ConfigFile.inLoadOrder();
        assertTrue(indexOf(order, ConfigFile.APPLICATION) < indexOf(order, ConfigFile.BOT));
    }

    private static int indexOf(ConfigFile[] files, ConfigFile target) {
        for (int i = 0; i < files.length; i++) {
            if (files[i] == target) {
                return i;
            }
        }
        return -1;
    }

    // ==================================================================
    // 刷新与重置
    // ==================================================================

    @Test
    void 文件级刷新能发现外部改动() throws IOException {
        Files.writeString(propertyFile(ConfigFile.BOT), "bot.switch.disable_group=true\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.BOT);
        assertTrue(Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP));

        // 模拟用户在服务器上直接改了文件（true -> false）
        Files.writeString(propertyFile(ConfigFile.BOT), "bot.switch.disable_group=false\n", StandardCharsets.UTF_8);
        List<ConfigChange> changes = configHub.refreshFile(ConfigFile.BOT);

        assertFalse(Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP));
        assertEquals(1, changes.size());
        assertEquals(ConfigKey.BOT_SWITCH_DISABLE_GROUP, changes.get(0).key());
        assertEquals("true", changes.get(0).oldValue());
        assertEquals("false", changes.get(0).newValue());
    }

    @Test
    void 单key刷新重新读取该key所在文件() throws IOException {
        Files.writeString(propertyFile(ConfigFile.BOT),
                "# 加群提示\nbot.switch.group_increase=true\n", StandardCharsets.UTF_8);
        Files.writeString(propertyFile(ConfigFile.URL),
                "url_conf.btbtla_search=https://a.example\n", StandardCharsets.UTF_8);
        Configs.reloadAll();
        assertTrue(Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_INCREASE));

        Files.writeString(propertyFile(ConfigFile.BOT),
                "# 加群提示\nbot.switch.group_increase=false\n", StandardCharsets.UTF_8);

        List<ConfigChange> changes = configHub.refresh(ConfigKey.BOT_SWITCH_GROUP_INCREASE);
        assertEquals(1, changes.size());
        assertFalse(Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_INCREASE));
        // 其它文件的配置不受影响
        assertEquals("https://a.example", Configs.getStr(ConfigKey.URL_CONF_BTBTLA_SEARCH));
    }

    @Test
    void 重置properties配置是把默认值写回该行() throws IOException {
        configHub.save(ConfigKey.WS_MAX_CONNECTIONS, "99");
        assertEquals(99, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertTrue(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS));

        configHub.reset(ConfigKey.WS_MAX_CONNECTIONS);

        assertEquals(5, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertTrue(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS), "key应保留在文件里");
        Map<String, String> ws = PropertiesFileUtil.load(ConfigFile.WEBSOCKET.getFileName());
        assertEquals("5", ws.get("bot.ws.max_connections"));
    }

    @Test
    void 程序自动写入配置会落盘() throws IOException {
        assertTrue(Configs.save(ConfigKey.BILIBILI_COOKIES_TICKET, "{\"ticket\":\"abc\"}"));
        assertEquals("{\"ticket\":\"abc\"}", Configs.getStr(ConfigKey.BILIBILI_COOKIES_TICKET));
        Map<String, String> bilibili = PropertiesFileUtil.load(ConfigFile.BILIBILI.getFileName());
        assertEquals("{\"ticket\":\"abc\"}", bilibili.get("bilibili.cookies.ticket"));
    }

    @Test
    void 程序自动写入yml配置也会落盘() throws IOException {
        Path file = applicationFile(ConfigFile.APPLICATION);
        Files.writeString(file, "server:\n  port: 8090\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        assertTrue(Configs.save(ConfigKey.SERVER_PORT, "18090"));
        assertEquals(18090, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("port: 18090"));
    }
}
