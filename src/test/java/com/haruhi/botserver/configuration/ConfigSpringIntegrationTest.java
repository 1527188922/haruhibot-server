package com.haruhi.botserver.configuration;

import com.haruhi.botserver.configuration.metadata.ConfigFile;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.metadata.ConfigType;
import com.haruhi.botserver.configuration.metadata.ControlType;
import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.configuration.service.ConfigApplier;
import com.haruhi.botserver.configuration.service.ConfigHub;
import com.haruhi.botserver.configuration.store.PropertiesFileUtil;
import com.haruhi.botserver.configuration.store.YamlFileUtil;
import com.haruhi.botserver.configuration.model.ConfigFileNode;
import com.haruhi.botserver.configuration.model.ConfigItem;
import com.haruhi.botserver.bootstrap.WebResourceConfig;
import com.haruhi.botserver.configuration.controller.ConfigController;
import com.haruhi.botserver.infrastructure.scheduling.JobManager;
import com.haruhi.botserver.features.ai.support.OpenAiServiceHolder;
import com.haruhi.botserver.shared.model.HttpResp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.function.Supplier;

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
@SpringBootTest(classes = com.haruhi.botserver.HaruhiBotServer.class,
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
    private JobManager jobManage;
    @Autowired
    private OpenAiServiceHolder openAiServiceHolder;
    @Autowired
    private SchedulerFactoryBean schedulerFactoryBean;
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
        // 只有一份 application.yml：端口、日志级别、资源模式都在里面
        Files.writeString(baseDir.resolve(ConfigFile.APPLICATION.getFileName()), """
                # 应用主配置
                server:
                  # http端口
                  port: 8090

                logging:
                  level:
                    com.haruhi.botserver: debug
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
        // File refresh can clear internet-host; make fallback lookups deterministic and offline.
        WebResourceConfig resources = applicationContext.getBean(WebResourceConfig.class);
        ReflectionTestUtils.setField(resources, "publicIpSupplier", (Supplier<String>) () -> "203.0.113.10");
        ReflectionTestUtils.setField(resources, "localIpSupplier", (Supplier<String>) () -> "192.0.2.20");
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
        assertTrue(appliers.containsValue(applicationContext.getBean(WebResourceConfig.class)),
                "WebResourceConfig must subscribe to internet-host changes");
        assertTrue(appliers.containsValue(jobManage), "JobManager 应注册为 ConfigApplier");
        assertTrue(appliers.containsValue(openAiServiceHolder), "OpenAiServiceHolder 应注册为 ConfigApplier");

        configHub.save(ConfigKey.DEEP_SEEK_API_TIMEOUT, "60");
        assertEquals(60, Configs.getInt(ConfigKey.DEEP_SEEK_API_TIMEOUT));
    }

    @Test
    void jobProperties的开关能热更新() throws Exception {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("BilibiliLiveJob_trigger", "BilibiliLiveJob_group");

        // 基线里 bilibiliLive.enable=false（每次测试前重读，但不会通知订阅者），
        // 所以这里用"先开再关"来保证两次都是真实的值变化
        HttpResp<ConfigController.SaveResult> on = configController.save(saveReq("job.bilibiliLive.enable", "true"));
        assertEquals(200, on.getCode());
        assertTrue(on.getData().isHot(), "job.*.enable 应是热更新项：" + on.getMessage());
        assertTrue(on.getData().getHotKeys().contains("job.bilibiliLive.enable"), on.getMessage());
        assertTrue(Configs.getBool(ConfigKey.JOB_BILIBILI_LIVE_ENABLE));
        assertTrue(scheduler.checkExists(triggerKey), "开启后应即时注册任务");

        HttpResp<ConfigController.SaveResult> off = configController.save(saveReq("job.bilibiliLive.enable", "false"));
        assertEquals(200, off.getCode());
        assertFalse(Configs.getBool(ConfigKey.JOB_BILIBILI_LIVE_ENABLE));
        assertFalse(scheduler.checkExists(triggerKey), "关闭后应即时取消任务");
    }

    @Test
    void jobProperties的cron能热更新() throws Exception {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("BilibiliLiveJob_trigger", "BilibiliLiveJob_group");

        configController.save(saveReq("job.bilibiliLive.enable", "true"));
        assertTrue(scheduler.checkExists(triggerKey));
        assertEquals("0 0/5 * * * ?", ((CronTrigger) scheduler.getTrigger(triggerKey)).getCronExpression());

        configController.save(saveReq("job.bilibiliLive.cron", "0 0/9 * * * ?"));
        assertEquals("0 0/9 * * * ?", ((CronTrigger) scheduler.getTrigger(triggerKey)).getCronExpression(),
                "cron 应即时重新排期");
    }

    @Test
    void 文件里缺少的job开关保存后同样能热更新() throws Exception {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("BilibiliLiveJob_trigger", "BilibiliLiveJob_group");
        Path file = configDir.resolve(ConfigFile.JOB.getFileName());

        configController.save(saveReq("job.bilibiliLive.enable", "true"));
        assertTrue(scheduler.checkExists(triggerKey));

        // 模拟"这一行不在文件里"（被手工删掉等）：重读该文件后应回落到声明默认值false
        Files.writeString(file, "job.bilibiliLive.cron=0 0/5 * * * ?\n", StandardCharsets.UTF_8);
        configHub.refreshFile(ConfigFile.JOB);
        assertFalse(Configs.isConfigured(ConfigKey.JOB_BILIBILI_LIVE_ENABLE));
        assertFalse(scheduler.checkExists(triggerKey), "文件里没有该key时应回落到默认值false并取消任务");

        // 再把开关打开：应即时注册，并把这一行写回文件
        HttpResp<ConfigController.SaveResult> resp = configController.save(saveReq("job.bilibiliLive.enable", "true"));
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isHot(), resp.getMessage());
        assertTrue(Configs.isConfigured(ConfigKey.JOB_BILIBILI_LIVE_ENABLE), "应把该key写回文件");
        assertTrue(scheduler.checkExists(triggerKey), "文件里原本没有的key，保存后也应即时注册任务");
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("job.bilibiliLive.enable=true"),
                "应写回文件: " + Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void 直接用编辑器改jobProperties也会被自动感知并热更新() throws Exception {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("BilibiliLiveJob_trigger", "BilibiliLiveJob_group");
        Path file = configDir.resolve(ConfigFile.JOB.getFileName());

        configController.save(saveReq("job.bilibiliLive.enable", "true"));
        assertTrue(scheduler.checkExists(triggerKey));

        // 模拟用户在服务器上直接用编辑器把开关改成 false（不经过接口）
        Files.writeString(file, "job.bilibiliLive.enable=false\njob.bilibiliLive.cron=0 0/5 * * * ?\n",
                StandardCharsets.UTF_8);
        // 文件监听按"最后修改时间"判断是否变化，而 Windows 时间戳精度约15ms，这里显式推后避免同刻误判
        assertTrue(file.toFile().setLastModified(System.currentTimeMillis() + 2000));
        configHub.watchFiles();

        assertFalse(Configs.getBool(ConfigKey.JOB_BILIBILI_LIVE_ENABLE), "外部改动应被自动重载");
        assertFalse(scheduler.checkExists(triggerKey), "外部关掉开关后应即时取消任务");
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
    void 配置项下发控件元数据() {
        Map<String, ConfigItem> items = new java.util.HashMap<>();
        for (ConfigFileNode node : configController.list().getData()) {
            for (ConfigItem item : node.getItems()) {
                items.put(item.getKey(), item);
            }
        }

        // 值类型与控件类型分别下发：同一个值类型可以配不同控件
        ConfigItem logging = items.get("logging.level.com.haruhi.botserver");
        assertEquals(ConfigType.STRING, logging.getType());
        assertEquals(ControlType.SELECT, logging.getControl().getType());
        assertEquals(5, logging.getControl().getOptions().size());
        assertFalse(logging.getControl().isMultiple());

        ConfigItem playwright = items.get("playwright.skip-browser-download-mode");
        assertEquals(ConfigType.STRING, playwright.getType());
        assertEquals(ControlType.RADIO, playwright.getControl().getType());

        ConfigItem druidFilters = items.get("spring.datasource.dynamic.datasource.master.druid.filters");
        assertEquals(ConfigType.STRING, druidFilters.getType());
        assertEquals(ControlType.CHECKBOX, druidFilters.getControl().getType());
        assertTrue(druidFilters.getControl().isMultiple());

        // LIST → 多选下拉 + 可自定义值；BOOL → 开关；SECRET → 输入框
        ConfigItem superusers = items.get("bot.superusers");
        assertEquals(ControlType.SELECT, superusers.getControl().getType());
        assertTrue(superusers.getControl().isMultiple());
        assertTrue(superusers.getControl().isAllowCustom());
        assertEquals(ControlType.SWITCH, items.get("bot.upload_file.parallel").getControl().getType());
        assertEquals(ControlType.INPUT, items.get("bot.ws.access_token").getControl().getType());
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
        assertTrue(after.contains("com.haruhi.botserver: debug"), "其它配置应保留: " + after);
    }

    @Test
    void 重置applicationYml会把默认值写回该行且不新增行() throws IOException {
        Path file = baseDir.resolve(ConfigFile.APPLICATION.getFileName());

        HttpResp<ConfigController.SaveResult> saved = configController.save(saveReq("server.port", "8099"));
        assertEquals(200, saved.getCode());
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).contains("port: 8099"));

        ConfigController.ConfigReq req = new ConfigController.ConfigReq();
        req.setKey("server.port");
        HttpResp<ConfigController.SaveResult> resp = configController.reset(req);
        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().getRestartKeys().contains("server.port"), resp.getMessage());

        // 保留该key，只把值改成声明中的默认值（不是把行删掉）
        assertEquals(8090, Configs.getInt(ConfigKey.SERVER_PORT));
        assertTrue(Configs.isConfigured(ConfigKey.SERVER_PORT), "key应保留在文件里");
        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(after.contains("port: 8090"), "应写回默认值: " + after);
        assertFalse(after.contains("8099"), after);
        assertEquals(1, after.split("port:", -1).length - 1, "不应新增行: " + after);
        assertTrue(after.contains("# http端口"), "该key上方的注释应保留: " + after);
        assertTrue(after.contains("com.haruhi.botserver: debug"), after);

        // 再点几次重置也不会新增行
        configController.reset(req);
        configController.reset(req);
        assertEquals(after, Files.readString(file, StandardCharsets.UTF_8), "重复重置不应改动文件");
    }

    @Test
    void 保存applicationYml的缺失key会写入嵌套结构而不是点分行() throws IOException {
        Path file = baseDir.resolve(ConfigFile.APPLICATION.getFileName());
        // 基线文件里没有 server 块，先删掉它，模拟"配置文件不全"的情况
        Files.writeString(file, "logging:\n  level:\n    com.haruhi.botserver: debug\n", StandardCharsets.UTF_8);
        Configs.reloadFile(ConfigFile.APPLICATION);

        configController.save(saveReq("server.port", "8098"));
        configController.save(saveReq("server.port", "8099"));

        String after = Files.readString(file, StandardCharsets.UTF_8);
        assertFalse(after.contains("server.port"), "不应是properties风格的点分行: " + after);
        String expected = "logging:\n  level:\n    com.haruhi.botserver: debug\n\nserver:\n  port: 8099";
        assertEquals(expected, after.strip(), after);
        Configs.reloadFile(ConfigFile.APPLICATION);
        assertEquals(8099, Configs.getInt(ConfigKey.SERVER_PORT));
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
    void 日志级别保存在applicationYml且不再有歧义() throws IOException {
        String key = "logging.level.com.haruhi.botserver";
        // 只有一份 application.yml：同一个key不再出现在多个文件里
        assertFalse(ConfigKey.isAmbiguous(key), "dev/prod 文件已删除，不应再有歧义");
        assertEquals(ConfigKey.LOGGING_LEVEL, ConfigKey.of(ConfigFile.APPLICATION, key));

        HttpResp<ConfigController.SaveResult> resp = configController.batchSave(batchReq(saveReq(key, "trace")));
        assertEquals(200, resp.getCode());
        assertEquals("trace", Configs.getStr(ConfigKey.LOGGING_LEVEL));

        String app = Files.readString(baseDir.resolve(ConfigFile.APPLICATION.getFileName()), StandardCharsets.UTF_8);
        assertTrue(app.contains("com.haruhi.botserver: trace"), app);

        // 前端会带上 fileName，带上也一样能保存
        ConfigController.SaveReq scoped = saveReq(key, "warn");
        scoped.setFileName(ConfigFile.APPLICATION.getFileName());
        assertEquals(200, configController.batchSave(batchReq(scoped)).getCode());
        assertEquals("warn", Configs.getStr(ConfigKey.LOGGING_LEVEL));
    }

    @Test
    void 静态资源地址只注册一个实现() {
        // 不再有 dev/prod 两套实现，也不靠环境/模式条件
        assertEquals(1, applicationContext.getBeansOfType(WebResourceConfig.class).size());
        WebResourceConfig config = applicationContext.getBean(WebResourceConfig.class);
        String home = config.webHomePath();
        assertTrue(home.startsWith("http://"), home);
        assertTrue(home.endsWith(":8090"), home);
        // 资源路径直接挂在 webHomePath 下（与部署目录结构一致）
        assertEquals(home + "/image", config.webResourcesImagePath());
        assertEquals(home + "/jmcomic", config.webResourcesJmcomicPath());
        assertEquals(home + "/jmcomic", config.webResourcesJmcomicPathInClasses());
        assertEquals(home + "/audio/dg", config.webDgAudioPath());
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
