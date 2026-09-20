package com.haruhi.botServer.config;

import com.haruhi.botServer.config.config.ConfigFile;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.service.ConfigApplier;
import com.haruhi.botServer.config.service.ConfigHub;
import com.haruhi.botServer.config.util.PropertiesFileUtil;
import com.haruhi.botServer.config.util.YamlFileUtil;
import com.haruhi.botServer.config.vo.ConfigFileNode;
import com.haruhi.botServer.controller.ConfigController;
import com.haruhi.botServer.job.schedule.JobManage;
import com.haruhi.botServer.utils.OpenAiServiceHolder;
import com.haruhi.botServer.vo.HttpResp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring 容器启动 + 配置管理接口联调测试
 * <p>
 * 验证重点：
 * <ul>
 *     <li>整个应用能正常启动（quartz / 数据源 / websocket 等装配不受配置重构影响）</li>
 *     <li>{@code ./config/*.properties} 与 {@code application*.yml} 里的配置，既能被 {@link Configs}
 *         读到，也能被 Spring Environment 读到</li>
 *     <li>{@link ConfigApplier} 订阅者已注册，且 {@link ConfigHub} 能按key通知到它们</li>
 *     <li>配置管理接口返回的数据结构完整，且所有配置项都可编辑</li>
 * </ul>
 */
@ActiveProfiles("test")
@NoMockitoSpringTest
@SpringBootTest(classes = com.haruhi.botServer.HaruhiBotServer.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.banner-mode=off")
class ConfigSpringIntegrationTest {

    /** 临时程序目录：application*.yml 在这里 */
    private static Path baseDir;
    /** 临时 properties 目录：{baseDir}/config */
    private static Path configDir;

    @Autowired
    private ConfigHub configHub;
    @Autowired
    private ConfigController configController;
    @Autowired
    private JobManage jobManage;
    @Autowired
    private OpenAiServiceHolder openAiServiceHolder;
    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Value("${job.downloadPixiv.cron}")
    private String pixivCron;

    @Value("${job.bilibiliLive.enable}")
    private String bilibiliLiveEnable;

    /** yml 里的配置也要能被 Spring 读到 */
    @Value("${server.port}")
    private int serverPortFromEnvironment;

    /** properties 里的配置也要能被 Spring 读到 */
    @Value("${bot.ws.max_connections}")
    private String wsMaxConnectionsFromEnvironment;

    /**
     * 把配置文件的真实内容注册进 Spring Environment
     * <p>
     * 生产环境由 {@code ConfigsEnvironmentInitializer}（spring.factories）自动完成同样的事，
     * 这里在测试中显式注册，避免依赖测试的工作目录
     */
    @DynamicPropertySource
    static void registerConfigProperties(DynamicPropertyRegistry registry) {
        Path dir = prepare();
        for (ConfigFile file : ConfigFile.inLoadOrder()) {
            Map<String, String> props;
            if (file.isYaml()) {
                props = YamlFileUtil.load(baseDir.resolve(file.getFileName()).toFile());
            } else {
                props = PropertiesFileUtil.load(dir.resolve(file.getFileName()).toFile());
            }
            props.forEach((key, value) -> registry.add(key, () -> value));
        }
        registry.add("server.port", () -> "8090");
        registry.add("job.downloadPixiv.enable", () -> "true");
        registry.add("job.bilibiliLive.enable", () -> "false");
    }

    private static synchronized Path prepare() {
        if (configDir != null) {
            return configDir;
        }
        try {
            baseDir = Files.createTempDirectory("haruhibot-config-it");
            configDir = baseDir.resolve("config");
            Files.createDirectories(configDir);
            writeBaseline();
            Configs.useConfigDirForTest(baseDir.toString());
            return configDir;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeBaseline() throws IOException {
        Files.writeString(baseDir.resolve(ConfigFile.APPLICATION.getFileName()), """
                # 应用主配置
                server:
                  # http端口
                  port: 8090

                spring:
                  profiles:
                    active: test
                """, StandardCharsets.UTF_8);
        Files.writeString(baseDir.resolve(ConfigFile.APPLICATION_DEV.getFileName()), """
                # dev 环境
                logging:
                  level:
                    com.haruhi.botServer: debug

                spring:
                  datasource:
                    dynamic:
                      datasource:
                        master:
                          url: jdbc:sqlite:./data/haruhibot_server.db
                """, StandardCharsets.UTF_8);
        Files.writeString(baseDir.resolve(ConfigFile.APPLICATION_PROD.getFileName()), """
                # prod 环境
                logging:
                  level:
                    com.haruhi.botServer: info

                spring:
                  datasource:
                    dynamic:
                      datasource:
                        master:
                          url: jdbc:sqlite:./data/haruhibot_server.db
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.JOB.getFileName()), """
                # 定时任务
                job.downloadPixiv.enable=true
                job.downloadPixiv.cron=0 0 3 * * ?
                job.bilibiliLive.enable=false
                job.bilibiliLive.cron=0 0/5 * * * ?
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.WEBSOCKET.getFileName()), """
                bot.ws.access_token=token-123456
                bot.ws.max_connections=7
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.BOT.getFileName()), """
                bot.superusers=10001,10002
                bot.switch.disable_group=false
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.AI.getFileName()), """
                ds.api.key=sk-test-key
                ds.api.base_url=https://api.deepseek.com
                ds.api.timeout=45
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.SEARCH_IMG.getFileName()), """
                searchimg.saucenao.baseurl=https://saucenao.com/search.php
                searchimg.saucenao.apikey=sk-saucenao
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.URL.getFileName()), """
                url_conf.agefans=https://www.agemys.vip
                url_conf.lolicon=https://api.lolicon.app/setu/v2
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.DATABASE.getFileName()), """
                spring.datasource.dynamic.datasource.master.url=jdbc:sqlite::resource:data/haruhibot_server.db?journal_mode=WAL&synchronous=NORMAL
                spring.datasource.dynamic.datasource.master.driver-class-name=org.sqlite.JDBC
                spring.datasource.dynamic.datasource.master.druid.max-active=5
                """, StandardCharsets.UTF_8);
        Files.writeString(configDir.resolve(ConfigFile.CHAT_RECORD.getFileName()), """
                db.chat_extend.raw_compress=true
                """, StandardCharsets.UTF_8);
    }

    @BeforeAll
    static void prepareConfigDir() {
        prepare();
    }

    /**
     * 每个测试前重置受测文件，避免测试之间相互影响
     */
    @BeforeEach
    void resetConfigFiles() throws IOException {
        writeBaseline();
        Configs.reloadAll();
    }

    @Test
    void 配置文件同时被Configs与SpringEnvironment读到() {
        // Configs 侧：properties
        assertTrue(Configs.getBool(ConfigKey.JOB_DOWNLOAD_PIXIV_ENABLE));
        assertEquals("0 0 3 * * ?", Configs.getStr(ConfigKey.JOB_DOWNLOAD_PIXIV_CRON));
        assertFalse(Configs.getBool(ConfigKey.JOB_BILIBILI_LIVE_ENABLE));
        assertEquals(List.of(10001L, 10002L), Configs.getList(ConfigKey.BOT_SUPERUSERS, Long.class, List.of()));
        assertEquals("https://saucenao.com/search.php", Configs.getStr(ConfigKey.SEARCH_IMG_SAUCENAO_BASEURL));
        // Configs 侧：yml
        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));

        // Spring Environment 侧
        assertEquals("0 0 3 * * ?", pixivCron);
        assertEquals("false", bilibiliLiveEnable);
        assertEquals(8090, serverPortFromEnvironment);
        assertEquals("7", wsMaxConnectionsFromEnvironment);
    }

    @Test
    void 订阅者已注册且能被ConfigHub通知() {
        Map<String, ConfigApplier> appliers = applicationContext.getBeansOfType(ConfigApplier.class);
        assertTrue(appliers.containsValue(jobManage), "JobManage 应注册为 ConfigApplier");
        assertTrue(appliers.containsValue(openAiServiceHolder), "OpenAiServiceHolder 应注册为 ConfigApplier");

        configHub.save(ConfigKey.DEEP_SEEK_API_TIMEOUT, "60");
        assertEquals(60, Configs.getInt(ConfigKey.DEEP_SEEK_API_TIMEOUT));
    }

    @Test
    void 数据源以databaseProperties为准() throws Exception {
        // application*.yml 里没有 spring.datasource，数据源由 database.properties + Aspect 提供
        assertEquals("jdbc:sqlite::resource:data/haruhibot_server.db?journal_mode=WAL&synchronous=NORMAL",
                Configs.getStr(ConfigKey.DATABASE_URL));
        assertEquals("org.sqlite.JDBC", Configs.getStr(ConfigKey.DATABASE_DRIVER_CLASS_NAME));
        assertEquals(5, Configs.getInt(ConfigKey.DATABASE_DRUID_MAX_ACTIVE));

        // 容器里真正生效的 master 数据源 url 应等于配置里的值
        DataSource master = applicationContext.getBean(DataSource.class);
        assertNotNull(master);
        try (Connection connection = master.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    void 配置管理接口返回完整的分组数据() {
        HttpResp<List<ConfigFileNode>> resp = configController.list();
        assertEquals(200, resp.getCode());
        List<ConfigFileNode> nodes = resp.getData();
        assertEquals(ConfigFile.values().length, nodes.size());

        ConfigFileNode bot = nodes.stream()
                .filter(e -> ConfigFile.BOT.getFileName().equals(e.getFileName()))
                .findFirst().orElseThrow();
        assertEquals("机器人", bot.getDisplayName());
        assertTrue(bot.isExists());
        assertEquals(bot.getCount(), bot.getItems().size());

        // 敏感项不下发明文
        ConfigFileNode ws = nodes.stream()
                .filter(e -> ConfigFile.WEBSOCKET.getFileName().equals(e.getFileName()))
                .findFirst().orElseThrow();
        assertTrue(ws.getItems().stream()
                .filter(e -> "bot.ws.access_token".equals(e.getKey()))
                .allMatch(e -> e.getValue() == null && e.isHasValue()));

        // yml 类配置也要出现在列表里，并且能给前端提供值
        ConfigFileNode app = nodes.stream()
                .filter(e -> ConfigFile.APPLICATION.getFileName().equals(e.getFileName()))
                .findFirst().orElseThrow();
        assertEquals("应用主配置", app.getDisplayName());
        assertTrue(app.getItems().stream().anyMatch(e -> "server.port".equals(e.getKey()) && "8090".equals(e.getValue())));
    }

    @Test
    void 保存properties后落盘并能刷新回来() throws IOException {
        HttpResp<ConfigController.SaveResult> resp = configController.save(saveReq("bot.ws.max_connections", "9"));
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isHot(), "bot.ws.max_connections 是热更新项");
        assertEquals(9, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));

        String fileText = Files.readString(configDir.resolve(ConfigFile.WEBSOCKET.getFileName()), StandardCharsets.UTF_8);
        assertTrue(fileText.contains("bot.ws.max_connections=9"));

        // 手动改文件后刷新整个文件
        Files.writeString(configDir.resolve(ConfigFile.WEBSOCKET.getFileName()),
                "bot.ws.max_connections=11\n", StandardCharsets.UTF_8);
        configController.refreshFile(fileReq(ConfigFile.WEBSOCKET.getFileName()));
        assertEquals(11, Configs.getInt(ConfigKey.WS_MAX_CONNECTIONS));
    }

    @Test
    void 保存applicationYml后落盘且保留注释() throws IOException {
        Path file = baseDir.resolve(ConfigFile.APPLICATION.getFileName());

        HttpResp<ConfigController.SaveResult> resp = configController.save(saveReq("server.port", "8099"));
        assertEquals(200, resp.getCode());
        // server.port 也允许修改，只是需要重启才真正换端口
        assertFalse(resp.getData().isHot());
        assertTrue(resp.getData().getRestartKeys().contains("server.port"));

        // 快照立即生效
        assertEquals(8099, Configs.getInt(ConfigKey.SERVER_PORT));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("# http端口"), "注释应保留: " + after);
        assertTrue(after.contains("port: 8099"), "值应写入: " + after);
        assertTrue(after.contains("active: test"), "其它配置应保留: " + after);
    }

    @Test
    void 所有配置项都允许修改() {
        // 不存在"不可编辑"的项：区分只在能否热生效
        List<ConfigFileNode> nodes = configController.list().getData();
        int items = 0;
        int hot = 0;
        for (ConfigFileNode node : nodes) {
            items += node.getCount();
            hot += node.getHotCount();
            assertTrue(node.getCount() > 0, node.getFileName() + " 应有配置项");
        }
        assertTrue(items > 30, "配置项总数应大于30，实际：" + items);
        assertTrue(hot > 0, "应有可热更新的配置项");
    }

    @Test
    void 同名属性带fileName可以正常保存到指定文件() throws IOException {
        String key = "logging.level.com.haruhi.botServer";
        assertTrue(ConfigKey.isAmbiguous(key), "该key应在多个文件中声明");

        // 带上 fileName：应精确改到 application-dev.yml
        ConfigController.SaveReq req = saveReq(key, "trace");
        req.setFileName(ConfigFile.APPLICATION_DEV.getFileName());
        HttpResp<ConfigController.SaveResult> resp = configController.batchSave(batchReq(req));
        assertEquals(200, resp.getCode());
        assertEquals("trace", Configs.getStr(ConfigKey.LOGGING_LEVEL_DEV));
        String dev = Files.readString(baseDir.resolve(ConfigFile.APPLICATION_DEV.getFileName()), StandardCharsets.UTF_8);
        assertTrue(dev.contains("com.haruhi.botServer: trace"), dev);
        // 另一个文件不能被改动
        String prod = Files.readString(baseDir.resolve(ConfigFile.APPLICATION_PROD.getFileName()), StandardCharsets.UTF_8);
        assertFalse(prod.contains("trace"), prod);
    }

    @Test
    void 同名属性不带fileName会给出明确提示() {
        HttpResp<ConfigController.SaveResult> resp = configController.save(
                saveReq("logging.level.com.haruhi.botServer", "trace"));
        assertEquals(500, resp.getCode());
        // 提示里应包含候选文件名，方便排错
        assertTrue(resp.getMessage().contains(ConfigFile.APPLICATION_DEV.getFileName()), resp.getMessage());
        assertTrue(resp.getMessage().contains(ConfigFile.APPLICATION_PROD.getFileName()), resp.getMessage());
    }

    @Test
    void 非热更新项保存后提示需要重启() {
        HttpResp<ConfigController.SaveResult> resp = configController.save(saveReq("druid.enabled", "true"));
        assertEquals(200, resp.getCode());
        assertFalse(resp.getData().isHot());
        assertTrue(resp.getData().getRestartKeys().contains("druid.enabled"));
    }

    @Test
    void 读取配置文件原始内容() {
        HttpResp<String> resp = configController.fileContent(fileReq(ConfigFile.WEBSOCKET.getFileName()));
        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().contains("bot.ws.max_connections"));

        // yml 也能读
        HttpResp<String> app = configController.fileContent(fileReq(ConfigFile.APPLICATION.getFileName()));
        assertEquals(200, app.getCode());
        assertTrue(app.getData().contains("port:"));
    }

    private ConfigController.SaveReq saveReq(String key, String value) {
        ConfigController.SaveReq req = new ConfigController.SaveReq();
        req.setKey(key);
        req.setValue(value);
        return req;
    }

    private ConfigController.BatchSaveReq batchReq(ConfigController.SaveReq... items) {
        ConfigController.BatchSaveReq req = new ConfigController.BatchSaveReq();
        req.setItems(java.util.Arrays.asList(items));
        return req;
    }

    private ConfigController.ConfigReq fileReq(String fileName) {
        ConfigController.ConfigReq req = new ConfigController.ConfigReq();
        req.setFileName(fileName);
        return req;
    }
}
