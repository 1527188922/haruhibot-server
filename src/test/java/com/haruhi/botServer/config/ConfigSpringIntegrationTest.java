package com.haruhi.botServer.config;

import com.haruhi.botServer.config.config.ConfigFile;
import com.haruhi.botServer.config.config.ConfigKey;
import com.haruhi.botServer.config.config.Configs;
import com.haruhi.botServer.config.service.ConfigApplier;
import com.haruhi.botServer.config.service.ConfigHub;
import com.haruhi.botServer.config.util.PropertiesFileUtil;
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
import java.util.List;
import java.util.Map;

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
 *     <li>{@code ./config/*.properties} 与 {@code ./config/server.yml} 里的配置，既能被 {@link Configs}
 *         读到，也能被 Spring Environment 读到</li>
 *     <li>{@link ConfigApplier} 订阅者已注册，且 {@link ConfigHub} 能按key通知到它们</li>
 *     <li>配置管理接口返回的数据结构完整（含 yml 类配置只读标记）</li>
 * </ul>
 */
@ActiveProfiles("test")
@NoMockitoSpringTest
@SpringBootTest(classes = com.haruhi.botServer.HaruhiBotServer.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.main.banner-mode=off")
class ConfigSpringIntegrationTest {

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
     * 把 ./config 下的配置文件内容作为 Spring 属性注册进 Environment
     * <p>
     * 生产环境由 {@code ConfigsEnvironmentInitializer}（spring.factories）自动完成同样的事，
     * 这里在测试中显式注册，避免依赖测试的工作目录
     */
    @DynamicPropertySource
    static void registerConfigProperties(DynamicPropertyRegistry registry) {
        Path dir = prepare();
        for (ConfigFile file : ConfigFile.values()) {
            if (file.isYaml()) {
                continue;
            }
            Map<String, String> props = PropertiesFileUtil.load(dir.resolve(file.getFileName()).toFile());
            props.forEach((key, value) -> registry.add(key, () -> value));
        }
        // yml 类的项单独注册（等价于生产环境由 initializer 拍平后注入）
        registry.add("server.port", () -> "8090");
        registry.add("job.downloadPixiv.enable", () -> "true");
        registry.add("job.bilibiliLive.enable", () -> "false");
    }

    private static synchronized Path prepare() {
        if (configDir != null) {
            return configDir;
        }
        try {
            Path dir = Files.createTempDirectory("haruhibot-config-it");
            writeBaseline(dir);
            configDir = dir;
            Configs.useConfigDirForTest(dir.toString());
            return dir;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeBaseline(Path dir) throws IOException {
        Files.writeString(dir.resolve(ConfigFile.JOB.getFileName()), """
                # 定时任务
                job.downloadPixiv.enable=true
                job.downloadPixiv.cron=0 0 3 * * ?
                job.bilibiliLive.enable=false
                job.bilibiliLive.cron=0 0/5 * * * ?
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(ConfigFile.WEBSOCKET.getFileName()), """
                bot.ws.access_token=token-123456
                bot.ws.max_connections=7
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(ConfigFile.BOT.getFileName()), """
                bot.superusers=10001,10002
                bot.switch.disable_group=false
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(ConfigFile.AI.getFileName()), """
                ds.api.key=sk-test-key
                ds.api.base_url=https://api.deepseek.com
                ds.api.timeout=45
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(ConfigFile.SEARCH_IMG.getFileName()), """
                searchimg.saucenao.baseurl=https://saucenao.com/search.php
                searchimg.saucenao.apikey=sk-saucenao
                searchimg.agefans.url=https://www.agemys.vip
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve(ConfigFile.SERVER.getFileName()), """
                server:
                  port: 8090
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
        writeBaseline(configDir);
        Configs.reloadAll();
    }

    @Test
    void 配置文件同时被Configs与SpringEnvironment读到() {
        // Configs 侧
        assertTrue(Configs.getBool(ConfigKey.JOB_DOWNLOAD_PIXIV_ENABLE));
        assertEquals("0 0 3 * * ?", Configs.getStr(ConfigKey.JOB_DOWNLOAD_PIXIV_CRON));
        assertFalse(Configs.getBool(ConfigKey.JOB_BILIBILI_LIVE_ENABLE));
        assertEquals(List.of(10001L, 10002L), Configs.getList(ConfigKey.BOT_SUPERUSERS, Long.class, List.of()));
        assertEquals("https://saucenao.com/search.php", Configs.getStr(ConfigKey.SEARCH_IMG_SAUCENAO_BASEURL));
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

        // yml 类配置只读
        ConfigFileNode server = nodes.stream()
                .filter(e -> ConfigFile.SERVER.getFileName().equals(e.getFileName()))
                .findFirst().orElseThrow();
        assertTrue(server.getItems().stream().noneMatch(e -> e.isWritable()));
    }

    @Test
    void 保存后落盘并能刷新回来() throws IOException {
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
    void 非热更新项保存后提示需要重启() {
        // server.port 属于 yml，页面不允许写入
        HttpResp<ConfigController.SaveResult> ymlResp = configController.save(saveReq("server.port", "8099"));
        assertEquals(500, ymlResp.getCode());

        // druid 开关是 properties 项，但需要重启
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
    }

    private ConfigController.SaveReq saveReq(String key, String value) {
        ConfigController.SaveReq req = new ConfigController.SaveReq();
        req.setKey(key);
        req.setValue(value);
        return req;
    }

    private ConfigController.ConfigReq fileReq(String fileName) {
        ConfigController.ConfigReq req = new ConfigController.ConfigReq();
        req.setFileName(fileName);
        return req;
    }
}
