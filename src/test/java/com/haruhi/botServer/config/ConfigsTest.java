package com.haruhi.botServer.config;

import com.haruhi.botServer.config.config.ConfigFile;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.ConfigType;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.service.ConfigApplier;
import com.haruhi.botServer.config.service.ConfigChange;
import com.haruhi.botServer.config.service.ConfigHub;
import com.haruhi.botServer.config.util.PropertiesFileUtil;
import com.haruhi.botServer.exception.BusinessException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置读写与热更新机制测试
 * <p>
 * 不启动 Spring 容器，直接验证 Configs 快照、PropertiesFileUtil / YamlFileUtil 文件读写
 * 与 ConfigHub 的"写文件 + 刷快照 + 按key通知"链路。
 */
class ConfigsTest {

    @TempDir
    Path tempDir;

    private ConfigHub configHub;

    @BeforeEach
    void setUp() {
        Configs.useConfigDirForTest(tempDir.toString());
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

    // ==================================================================
    // 声明默认值
    // ==================================================================

    @Test
    void 未配置时使用声明默认值且不会返回null() {
        assertEquals("5", Configs.getStr(ConfigKey.WS_MAX_CONNECTIONS));
        assertEquals(5, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertFalse(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS));
        assertEquals(Configs.ConfigSource.DEFAULT, Configs.source(ConfigKey.WS_MAX_CONNECTIONS));
        assertTrue(Configs.getBool(ConfigKey.BOT_SAME_MACHINE_QQCLIENT));
        // 未配置且没有默认值的项返回调用方给的默认值，而不是抛异常
        assertNull(Configs.getStr(ConfigKey.WS_ACCESS_TOKEN, null));
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
            assertEquals(key, ConfigKey.of(key.getKey()), "key无法反查: " + key.getKey());
        }
    }

    @Test
    void 老key可以反查到新配置项() {
        assertEquals(ConfigKey.WS_ACCESS_TOKEN, ConfigKey.ofLegacy("bot.access_token"));
        assertEquals(ConfigKey.WS_MAX_CONNECTIONS, ConfigKey.ofLegacy("bot.max_connections"));
        assertEquals(ConfigKey.SEARCH_IMG_SAUCENAO_APIKEY, ConfigKey.ofLegacy("saucenao.search_image_key"));
        assertEquals(ConfigKey.SEARCH_IMG_AGEFANS_URL, ConfigKey.ofLegacy("url_conf.agefans"));
        assertNull(ConfigKey.ofLegacy("db.sql_cache"));
        assertNull(ConfigKey.ofLegacy("not.exists"));
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
    void 写文件保留注释与已有顺序() throws IOException {
        Path file = tempDir.resolve(ConfigFile.WEBSOCKET.getFileName());
        Files.writeString(file, """
                ### Websocket配置

                # 认证token
                bot.ws.access_token=
                # 最大连接数
                bot.ws.max_connections=5
                """, StandardCharsets.UTF_8);

        configHub.save(ConfigKey.WS_MAX_CONNECTIONS, "9");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("### Websocket配置"), "文件头注释应保留");
        assertTrue(after.contains("# 最大连接数"), "key上方的注释应保留");
        assertTrue(after.contains("bot.ws.max_connections=9"), "值应被替换");
        assertTrue(after.contains("# 认证token"), "其他项的注释应保留");
        assertTrue(after.indexOf("bot.ws.access_token") < after.indexOf("bot.ws.max_connections"));
    }

    @Test
    void 新增key追加到文件末尾() throws IOException {
        Path file = tempDir.resolve(ConfigFile.BOT.getFileName());
        Files.writeString(file, "bot.switch.disable_group=false\n", StandardCharsets.UTF_8);

        configHub.save(ConfigKey.BOT_SWITCH_GROUP_INCREASE, "false");

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("bot.switch.disable_group=false"));
        assertTrue(after.contains("bot.switch.group_increase=false"));
    }

    @Test
    void 多行值与特殊字符可正确往返() throws IOException {
        configHub.save(ConfigKey.URL_CONF_BT_SEARCH, "http://a.com\nhttp://b.com");
        String text = Files.readString(tempDir.resolve(ConfigFile.URL.getFileName()), StandardCharsets.UTF_8);
        assertTrue(text.contains("url_conf.bt_search=http://a.com\\nhttp://b.com"), "换行应转义: " + text);

        Configs.reloadFile(ConfigFile.URL);
        assertEquals("http://a.com\nhttp://b.com", Configs.getStr(ConfigKey.URL_CONF_BT_SEARCH));
    }

    @Test
    void 注释掉的key与未声明的key都不会进入快照() throws IOException {
        Path file = tempDir.resolve(ConfigFile.AI.getFileName());
        Files.writeString(file, """
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
    // yml 支持
    // ==================================================================

    @Test
    void yml配置会被拍平后进入快照() throws IOException {
        Files.writeString(tempDir.resolve(ConfigFile.SERVER.getFileName()), """
                # 服务端
                server:
                  port: 18080
                """, StandardCharsets.UTF_8);

        Configs.reloadFile(ConfigFile.SERVER);

        assertEquals(18080, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Configs.isConfigured(ConfigKey.SERVER_PORT));
        assertEquals(ConfigFile.SERVER, ConfigKey.SERVER_PORT.getFile());
    }

    @Test
    void yml配置不支持页面写入() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> configHub.save(ConfigKey.SERVER_PORT, "18081"));
        assertTrue(e.getErrorMsg().contains("server.yml"), e.getErrorMsg());
        assertThrows(BusinessException.class, () -> configHub.reset(ConfigKey.SERVER_PORT));
        // 拒绝写入后快照不应被改动
        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));
    }

    @Test
    void 配置文件类型判断() {
        assertTrue(ConfigFile.SERVER.isYaml());
        assertFalse(ConfigFile.BOT.isYaml());
        assertFalse(ConfigFile.WEBSOCKET.isYaml());
    }

    // ==================================================================
    // 刷新与重置
    // ==================================================================

    @Test
    void 文件级刷新能发现外部改动() throws IOException {
        Path file = tempDir.resolve(ConfigFile.BOT.getFileName());
        Files.writeString(file, "bot.switch.disable_group=true\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.BOT);
        assertTrue(Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP));

        // 模拟用户在服务器上直接改了文件（true -> false）
        Files.writeString(file, "bot.switch.disable_group=false\n", StandardCharsets.UTF_8);
        List<ConfigChange> changes = configHub.refreshFile(ConfigFile.BOT);

        assertFalse(Configs.getBool(ConfigKey.BOT_SWITCH_DISABLE_GROUP));
        assertEquals(1, changes.size());
        assertEquals(ConfigKey.BOT_SWITCH_DISABLE_GROUP, changes.get(0).key());
        assertEquals("true", changes.get(0).oldValue());
        assertEquals("false", changes.get(0).newValue());
    }

    @Test
    void 单key刷新重新读取该key所在文件() throws IOException {
        Files.writeString(tempDir.resolve(ConfigFile.BOT.getFileName()),
                "# 加群提示\nbot.switch.group_increase=true\n", StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(ConfigFile.URL.getFileName()),
                "url_conf.btbtla_search=https://a.example\n", StandardCharsets.UTF_8);
        Configs.reloadAll();
        assertTrue(Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_INCREASE));

        Files.writeString(tempDir.resolve(ConfigFile.BOT.getFileName()),
                "# 加群提示\nbot.switch.group_increase=false\n", StandardCharsets.UTF_8);

        List<ConfigChange> changes = configHub.refresh(ConfigKey.BOT_SWITCH_GROUP_INCREASE);
        assertEquals(1, changes.size());
        assertFalse(Configs.getBool(ConfigKey.BOT_SWITCH_GROUP_INCREASE));
        // 其它文件的配置不受影响
        assertEquals("https://a.example", Configs.getStr(ConfigKey.URL_CONF_BTBTLA_SEARCH));
    }

    @Test
    void 重置回到声明默认值并从文件中删除该key() throws IOException {
        configHub.save(ConfigKey.WS_MAX_CONNECTIONS, "99");
        assertEquals(99, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertTrue(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS));

        configHub.reset(ConfigKey.WS_MAX_CONNECTIONS);

        assertEquals(5, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
        assertFalse(Configs.isConfigured(ConfigKey.WS_MAX_CONNECTIONS));
        Map<String, String> ws = PropertiesFileUtil.load(ConfigFile.WEBSOCKET.getFileName());
        assertNull(ws.get("bot.ws.max_connections"));
    }

    @Test
    void 程序自动写入配置会落盘() throws IOException {
        assertTrue(Configs.save(ConfigKey.BILIBILI_COOKIES_TICKET, "{\"ticket\":\"abc\"}"));
        assertEquals("{\"ticket\":\"abc\"}", Configs.getStr(ConfigKey.BILIBILI_COOKIES_TICKET));
        Map<String, String> bilibili = PropertiesFileUtil.load(ConfigFile.BILIBILI.getFileName());
        assertEquals("{\"ticket\":\"abc\"}", bilibili.get("bilibili.cookies.ticket"));
        // yml 不支持程序写入
        assertFalse(Configs.save(ConfigKey.SERVER_PORT, "1"));
    }
}
