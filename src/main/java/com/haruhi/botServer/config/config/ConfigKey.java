package com.haruhi.botServer.config.config;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 全部配置项声明
 * <p>
 * 这里是配置的<b>唯一声明处</b>：key、所属文件、类型、默认值、是否可热更新、前端分组等。
 * 实际取值通过 {@link Configs} 读取，读写文件通过 {@link com.haruhi.botServer.config.service.ConfigHub}。
 * <p>
 * 约定：
 * <ul>
 *     <li>key 使用 properties 风格的点分命名，同时作为 Spring 的属性名，因此 {@code application.yml} 中同名配置会被 {@code ./config/*.properties} 覆盖</li>
 *     <li>{@code hot=true} 表示修改后通过 {@link Configs} 立刻生效，无需重启；直接读 Configs 的代码天然支持</li>
 *     <li>{@code hot=false} 表示该值在应用启动阶段就已确定（端口、数据源、条件装配等），修改后必须重启</li>
 * </ul>
 */
@Getter
public enum ConfigKey {

    // ============================ WebUI ============================
    WEBUI_LOGIN_USERNAME(ConfigFile.WEBUI, "login.username", ConfigType.STRING, "haruhi", false,
            "WebUI登录账号", 10),
    WEBUI_LOGIN_PASSWORD(ConfigFile.WEBUI, "login.password", ConfigType.SECRET, "123123", false,
            "WebUI登录密码，同时也是druid监控台账号密码", 20),
    WEBUI_SESSION_MAX(ConfigFile.WEBUI, "login.session.max", ConfigType.INT, "6", false,
            "内存中token最大数量", 30),
    WEBUI_LOGIN_EXPIRE(ConfigFile.WEBUI, "login.expire", ConfigType.INT, "30", false,
            "内存token过期时长，单位分钟", 40),
    WEBUI_JWT_EXPIRE(ConfigFile.WEBUI, "login.jwt.expire", ConfigType.INT, "24", false,
            "JWT有效时长，单位小时", 50),
    WEBUI_JWT_SECRET(ConfigFile.WEBUI, "login.jwt.secret", ConfigType.SECRET,
            "QBgu5c4xfA4_6Ky8A_QnO_XW238-YD7Y6EH7HQKAyM8OiCNbn6xCcN-5BaZGysxA", false,
            "JWT签名密钥，留空会导致启动失败", 60),
    WEBUI_JWT_GRACE(ConfigFile.WEBUI, "login.jwt.grace", ConfigType.INT, "30", false,
            "JWT宽限时长，目前无用", 70),
    WEBUI_DRUID_ENABLED(ConfigFile.WEBUI, "druid.enabled", ConfigType.BOOL, "false", false,
            "是否开启druid监控台，需重启", 80),
    WEBUI_DRUID_MONITOR_URL_ENABLED(ConfigFile.WEBUI, "druid.monitor.url.enabled", ConfigType.BOOL, "false", false,
            "是否开启druid的URL监控，需重启", 90),
    WEBUI_DRUID_MONITOR_URL_SESSION_ENABLED(ConfigFile.WEBUI, "druid.monitor.url.session.enabled", ConfigType.BOOL, "false", false,
            "druid的URL监控是否统计session，需重启", 100),
    WEBUI_DRUID_MONITOR_SPRING_ENABLED(ConfigFile.WEBUI, "druid.monitor.spring.enabled", ConfigType.BOOL, "false", false,
            "是否开启druid的Spring监控，需重启", 110),

    // ============================ 服务端 ============================
    // 只有 server.port 保留在 yml 中（需要由 Spring 直接消费）
    SERVER_PORT(ConfigFile.SERVER, "server.port", ConfigType.INT, "8090", false,
            "http服务端口，修改后需重启", 10),

    // ============================ 定时任务 ============================
    JOB_DOWNLOAD_PIXIV_ENABLE(ConfigFile.JOB, "job.downloadPixiv.enable", ConfigType.BOOL, "false", true,
            "是否开启pixiv下载任务", 10),
    JOB_DOWNLOAD_PIXIV_CRON(ConfigFile.JOB, "job.downloadPixiv.cron", ConfigType.STRING, "0/15 * 0-7 * * ? *", true,
            "pixiv下载任务cron表达式", 20),
    JOB_BILIBILI_LIVE_ENABLE(ConfigFile.JOB, "job.bilibiliLive.enable", ConfigType.BOOL, "false", true,
            "是否开启b站直播推送任务", 30),
    JOB_BILIBILI_LIVE_CRON(ConfigFile.JOB, "job.bilibiliLive.cron", ConfigType.STRING, "0/30 * * * * ?", true,
            "b站直播状态检测cron表达式", 40),

    // ============================ 机器人 ============================
    BOT_SAME_MACHINE_QQCLIENT(ConfigFile.BOT, "bot.same-machine-qqclient", ConfigType.BOOL, "true", true,
            "qq客户端是否与本服务在同一台机器，true时图片/语音使用file://本地路径发送", 10),
    BOT_INTERNET_HOST(ConfigFile.BOT, "bot.internet-host", ConfigType.STRING, "", true,
            "本服务对外访问的ip或域名，留空自动探测；用于拼接图片url", 20),
    BOT_SUPERUSERS(ConfigFile.BOT, "bot.superusers", ConfigType.LIST, "1527188922", true,
            "机器人超级管理员qq号，多个用逗号分割", 30),
    BOT_ACCESS_GROUP(ConfigFile.BOT, "bot.access_groups", ConfigType.LIST, "", true,
            "可使用机器人的群号，多个用逗号分开，留空表示所有群都可使用", 40),
    BOT_UPLOAD_FILE_PARALLEL(ConfigFile.BOT, "bot.upload_file.parallel", ConfigType.BOOL, "true", true,
            "上传私聊/群文件时是否并发发送请求，false为排队发送", 50),
    BOT_SWITCH_DISABLE_GROUP(ConfigFile.BOT, "bot.switch.disable_group", ConfigType.BOOL, "false", true,
            "是否禁用所有群功能（仍保留聊天记录），true为禁用", 60),
    BOT_SWITCH_QINGYUNKE_CHAT(ConfigFile.BOT, "bot.switch.qingyunke_chat", ConfigType.BOOL, "true", true,
            "是否启用青云可聊天api，任何命令都未触发时会调用", 70),
    BOT_SWITCH_SEARCH_IMAGE_ALLOW_GROUP(ConfigFile.BOT, "bot.switch.search_image_allow_group", ConfigType.BOOL, "true", true,
            "是否允许群聊中使用识图功能", 80),
    BOT_SWITCH_SEARCH_BT_ALLOW_GROUP(ConfigFile.BOT, "bot.switch.search_bt_allow_group", ConfigType.BOOL, "true", true,
            "是否允许群聊中使用bt搜索功能", 90),
    BOT_SWITCH_GROUP_INCREASE(ConfigFile.BOT, "bot.switch.group_increase", ConfigType.BOOL, "true", true,
            "是否开启加群提示", 100),
    BOT_SWITCH_GROUP_DECREASE(ConfigFile.BOT, "bot.switch.group_decrease", ConfigType.BOOL, "true", true,
            "是否开启群成员离群提示", 110),

    // ============================ WebSocket ============================
    WS_ACCESS_TOKEN(ConfigFile.WEBSOCKET, "bot.ws.access_token", ConfigType.SECRET, "", true,
            "机器人Websocket服务建立连接时的认证token，留空表示无需认证", 10, "bot.access_token"),
    WS_MAX_CONNECTIONS(ConfigFile.WEBSOCKET, "bot.ws.max_connections", ConfigType.INT, "5", true,
            "机器人Websocket服务最大连接数，小于0无限制，0表示禁止连接（改成0不会断开已有连接）", 20, "bot.max_connections"),

    // ============================ 识图 ============================
    SEARCH_IMG_SAUCENAO_BASEURL(ConfigFile.SEARCH_IMG, "searchimg.saucenao.baseurl", ConfigType.STRING,
            "https://saucenao.com/search.php", true,
            "saucenao识图接口地址", 10),
    SEARCH_IMG_SAUCENAO_APIKEY(ConfigFile.SEARCH_IMG, "searchimg.saucenao.apikey", ConfigType.SECRET, "", true,
            "saucenao识图接口认证key，从 https://saucenao.com 获取", 20, "saucenao.search_image_key"),
    SEARCH_IMG_AGEFANS_URL(ConfigFile.SEARCH_IMG, "searchimg.agefans.url", ConfigType.STRING, "https://www.agemys.vip", true,
            "agefans网站地址，用于今日新番功能，末尾不需斜杠。备用：https://www.age.tv https://www.agemys.net", 30,
            "url_conf.agefans"),

    // ============================ B站 ============================
    BILIBILI_COOKIES_SESSDATA(ConfigFile.BILIBILI, "bilibili.cookies.sessdata", ConfigType.SECRET, "", true,
            "b站cookie中的SESSDATA，用于解析b站视频等需要调用b站api的功能", 10),
    BILIBILI_COOKIES_BILI_JCT(ConfigFile.BILIBILI, "bilibili.cookies.bili_jct", ConfigType.SECRET, "", true,
            "b站cookie中的bili_jct，用于解析b站视频等需要调用b站api的功能", 20),
    BILIBILI_COOKIES_TICKET(ConfigFile.BILIBILI, "bilibili.cookies.ticket", ConfigType.STRING, "", true,
            "请求b站接口自动获取，非必要，可减小风控概率", 30),
    BILIBILI_UPLOAD_VIDEO_DURATION_LIMIT(ConfigFile.BILIBILI, "bilibili.upload_video.duration_limit", ConfigType.INT, "600", true,
            "上传b站视频时长限制，单位秒", 40),
    BILIBILI_DOWNLOAD_VIDEO_DURATION_LIMIT(ConfigFile.BILIBILI, "bilibili.download_video.duration_limit", ConfigType.INT, "600", true,
            "下载b站视频时长限制，单位秒", 50),

    // ============================ AI ============================
    QIANWEN_API_KEY(ConfigFile.AI, "qianwen.api_key", ConfigType.SECRET, "", true,
            "阿里巴巴千问模型api key", 10),
    DEEP_SEEK_API_KEY(ConfigFile.AI, "ds.api.key", ConfigType.SECRET, "", true,
            "DeepSeek api key", 20),
    DEEP_SEEK_API_BASE_URL(ConfigFile.AI, "ds.api.base_url", ConfigType.STRING, "https://api.deepseek.com", true,
            "DeepSeek api baseurl", 30),
    DEEP_SEEK_API_TIMEOUT(ConfigFile.AI, "ds.api.timeout", ConfigType.INT, "30", true,
            "DeepSeek 请求超时时长，单位秒", 40),

    // ============================ JM漫画 ============================
    // 下面三项默认值与 JmcomicService.JM_DEFAULT_PASSWORD / DEFAULT_API_DOMAIN 保持一致
    JM_PASSWORD_ZIP(ConfigFile.JM, "jm.password.zip", ConfigType.STRING, "1234", true,
            "jm本子zip包解压密码。注意：修改密码不会改变已存在的zip包密码，重复下载可重新生成", 10),
    JM_PASSWORD_PDF(ConfigFile.JM, "jm.password.pdf", ConfigType.STRING, "1234", true,
            "jm本子pdf保护密码。注意：修改密码不会改变已存在的pdf文件密码，重复下载可重新生成", 20),
    JM_DOWNLOAD_THREADS(ConfigFile.JM, "jm.download.threads", ConfigType.INT, "0", true,
            "下载本子时线程数量，未配置、0或小于0则等于CPU逻辑核心数量", 30),
    JM_OPERATION_PARALLEL_ENABLED(ConfigFile.JM, "jm.operation.parallel.enabled", ConfigType.BOOL, "false", true,
            "JM漫画操作是否允许不同JM ID并发执行，false为所有JM操作串行", 40),
    JM_ALBUM_NAME_MAX_LENGTH(ConfigFile.JM, "jm.album.name_max_length", ConfigType.INT, "215", true,
            "jm本子名称最大长度（bytes.length而非字符个数），本子名会作为文件名使用，需小于255", 50),
    JM_API_DOMAIN(ConfigFile.JM, "jm.api_domain", ConfigType.STRING, "www.cdnbea.net", true,
            "JM API域名", 60),
    JM_SEARCH_RESULT_IMAGE_MODE(ConfigFile.JM, "jm.search_result.image_mode", ConfigType.BOOL, "false", true,
            "JM搜索结果是否以HTML转图片方式发送，false为合并消息发送", 70),
    JM_SEARCH_RESULT_LIMIT(ConfigFile.JM, "jm.search_result.limit", ConfigType.INT, "12", true,
            "JM搜索结果返回条数上限，图片和合并消息共用；无效或小于等于0时使用12条", 80),

    // ============================ 站点地址 ============================
    URL_CONF_BT_SEARCH(ConfigFile.URL, "url_conf.bt_search", ConfigType.STRING, "http://www.eclzz.bio", true,
            "磁力搜索网站地址，用于bt搜索功能，末尾不需斜杠", 20),
    URL_CONF_BTBTLA_SEARCH(ConfigFile.URL, "url_conf.btbtla_search", ConfigType.STRING, "https://www.btbtla.com", true,
            "bt影视搜索站点地址", 30),

    // ============================ 数据库 ============================
    DATABASE_DB_CHAT_EXTEND_RAW_COMPRESS(ConfigFile.DB, "db.chat_extend.raw_compress", ConfigType.BOOL, "true", true,
            "是否对聊天记录扩展表raw消息压缩存储", 20),
    ;
    /** 所属文件 */
    private final ConfigFile file;
    /** 配置key */
    private final String key;
    /** 值类型 */
    private final ConfigType type;
    /** 默认值 */
    private final String defaultValue;
    /** 是否可热更新（修改后无需重启即可生效） */
    private final boolean hot;
    /** 说明 */
    private final String remark;
    /** 前端展示排序 */
    private final int sort;
    /**
     * 老版本使用过的key（数据库字典表里的key），迁移时用于把旧值带过来
     */
    private final List<String> legacyKeys;

    ConfigKey(ConfigFile file, String key, ConfigType type, String defaultValue, boolean hot, String remark, int sort,
              String... legacyKeys) {
        this.file = file;
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.hot = hot;
        this.remark = remark;
        this.sort = sort;
        this.legacyKeys = legacyKeys == null ? List.of() : List.of(legacyKeys);
    }

    private static final Map<String, ConfigKey> KEY_MAP;

    static {
        Map<String, ConfigKey> map = new LinkedHashMap<>();
        for (ConfigKey value : values()) {
            ConfigKey exist = map.put(value.key, value);
            if (exist != null) {
                throw new IllegalStateException("配置key重复：" + value.key + " -> " + exist.name() + " / " + value.name());
            }
        }
        KEY_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * 根据key查找配置项，找不到返回null
     */
    public static ConfigKey of(String key) {
        return key == null ? null : KEY_MAP.get(key.trim());
    }

    public static List<ConfigKey> of(ConfigFile file) {
        return Arrays.stream(values()).filter(e -> e.file == file).toList();
    }

    /**
     * 根据老key（数据库字典表里的key）反查配置项，找不到返回null
     * <p>
     * 用于把老版本存量的配置值迁移到新的key上
     */
    public static ConfigKey ofLegacy(String legacyKey) {
        if (legacyKey == null || legacyKey.isBlank()) {
            return null;
        }
        String k = legacyKey.trim();
        for (ConfigKey value : values()) {
            if (value.legacyKeys.contains(k)) {
                return value;
            }
        }
        return null;
    }
}
