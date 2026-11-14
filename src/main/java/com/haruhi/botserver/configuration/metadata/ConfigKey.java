package com.haruhi.botserver.configuration.metadata;

import com.haruhi.botserver.configuration.service.Configs;
import com.haruhi.botserver.features.jmcomic.util.JmLineCatalog;

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
 * 实际取值通过 {@link Configs} 读取，读写文件通过 {@link com.haruhi.botserver.configuration.service.ConfigHub}。
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

    // ============================ 应用主配置（application.yml） ============================
    SERVER_PORT(ConfigFile.APPLICATION, "server.port", ConfigType.INT, "8090", false,
            "http服务端口，可在配置管理页修改，修改后需重启", 10),
    LOGGING_LEVEL(ConfigFile.APPLICATION, "logging.level.com.haruhi.botserver", ConfigType.STRING, "info", false,
            "本项目自身的日志级别（TRACE/DEBUG/INFO/WARN/ERROR），修改后需重启", 20,
            ControlMeta.select("TRACE,DEBUG,INFO,WARN,ERROR")),
    INTERNET_HOST(ConfigFile.APPLICATION, "internet-host", ConfigType.STRING, "", true,
            "本服务对外访问的ip或域名，留空自动探测；用于拼接图片url", 30),
    SAME_MACHINE_QQCLIENT(ConfigFile.APPLICATION, "same-machine-qqclient", ConfigType.BOOL, "true", true,
            "qq客户端是否与本服务在同一台机器，true时图片/语音使用file://本地路径发送", 40),
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD_MODE(ConfigFile.APPLICATION, "playwright.skip-browser-download-mode", ConfigType.INT, "0", true,
            "浏览器下载模式 0自动判断（正常优先，失败降级为跳过下载）1强制跳过下载直接使用系统浏览器 2强制使用playwright下载的浏览器；改完下次截图生效", 50,
            ControlMeta.radio("0:自动判断,1:强制跳过下载（用系统浏览器）,2:使用playwright下载的浏览器")),


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
    BOT_SWITCH_SEARCH_BT_ALLOW_GROUP(ConfigFile.BOT, "bot.switch.search_bt_allow_group", ConfigType.BOOL, "false", true,
            "是否允许群聊中使用bt搜索功能", 90),
    BOT_SWITCH_GROUP_INCREASE(ConfigFile.BOT, "bot.switch.group_increase", ConfigType.BOOL, "true", true,
            "是否开启加群提示", 100),
    BOT_SWITCH_GROUP_DECREASE(ConfigFile.BOT, "bot.switch.group_decrease", ConfigType.BOOL, "true", true,
            "是否开启群成员离群提示", 110),

    // ============================ WebSocket ============================
    WS_ACCESS_TOKEN(ConfigFile.WEBSOCKET, "bot.ws.access_token", ConfigType.SECRET, "", true,
            "机器人Websocket服务建立连接时的认证token，留空表示无需认证", 10),
    WS_MAX_CONNECTIONS(ConfigFile.WEBSOCKET, "bot.ws.max_connections", ConfigType.INT, "5", true,
            "机器人Websocket服务最大连接数，小于0无限制，0表示禁止连接（改成0不会断开已有连接）", 20),

    // ============================ 识图 ============================
    SEARCH_IMG_SAUCENAO_BASEURL(ConfigFile.SEARCH_IMG, "searchimg.saucenao.baseurl", ConfigType.STRING,
            "https://saucenao.com/search.php", true,
            "saucenao识图接口地址", 10),
    SEARCH_IMG_SAUCENAO_APIKEY(ConfigFile.SEARCH_IMG, "searchimg.saucenao.apikey", ConfigType.SECRET, "", true,
            "saucenao识图接口认证key，从 https://saucenao.com 获取", 20),

    // ============================ B站 ============================
    BILIBILI_COOKIES_RAW(ConfigFile.BILIBILI, "bilibili.cookies.raw", ConfigType.SECRET, "", true,
            "整条b站cookie（浏览器F12 -> Network -> 任意b站接口 -> Request Headers 里的 Cookie 整段复制）。" +
                    "配置后覆盖下面的sessdata登录态来源，风控概率最低；留空则用下面几项自动拼装。" +
                    "其中的bili_ticket会被忽略，统一使用程序自动生成并定期刷新的那一份", 5),
    BILIBILI_COOKIES_SESSDATA(ConfigFile.BILIBILI, "bilibili.cookies.sessdata", ConfigType.SECRET, "", true,
            "b站cookie中的SESSDATA，用于解析b站视频等需要调用b站api的功能（已配置bilibili.cookies.raw时无需配置）", 10),
    // 保存接口响应 raw
    BILIBILI_COOKIES_TICKET(ConfigFile.BILIBILI, "bilibili.cookies.ticket", ConfigType.STRING, "", true,
            "请求b站接口自动获取，非必要，可减小风控概率", 30),
    BILIBILI_COOKIES_BUVID3(ConfigFile.BILIBILI, "bilibili.cookies.buvid3", ConfigType.STRING, "", true,
            "b站设备标识buvid3，程序首次请求b站接口时自动获取并写入，不要手工修改", 31),
    BILIBILI_COOKIES_BUVID4(ConfigFile.BILIBILI, "bilibili.cookies.buvid4", ConfigType.STRING, "", true,
            "b站设备标识buvid4，程序首次请求b站接口时自动获取并写入，不要手工修改", 32),
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

    // ============================ DeepSeek 群聊/私聊 ============================
    DS_CHAT_ENABLE(ConfigFile.AI, "ds.chat.enable", ConfigType.BOOL, "false", true,
            "DeepSeek 聊天总开关；关闭时不会接管任何聊天，青云客保持原逻辑", 100),
    DS_CHAT_MODEL(ConfigFile.AI, "ds.chat.model", ConfigType.STRING, "deepseek-flash", true,
            "DeepSeek 聊天模型", 110, ControlMeta.select("deepseek-flash:deepseek-flash（默认，支持视觉）,deepseek-v4-pro:deepseek-v4-pro（更贵，不支持视觉）")),
    DS_CHAT_THINKING(ConfigFile.AI, "ds.chat.thinking", ConfigType.STRING, "disabled", true,
            "是否开启思考模式；聊天默认关闭以降低延迟", 120, ControlMeta.select("disabled:关闭（推荐）,enabled:开启")),
    DS_CHAT_REASONING_EFFORT(ConfigFile.AI, "ds.chat.reasoning_effort", ConfigType.STRING, "high", true,
            "思考强度，仅思考模式开启时生效", 130, ControlMeta.select("low:low,high:high,max:max")),
    DS_CHAT_TEMPERATURE(ConfigFile.AI, "ds.chat.temperature", ConfigType.STRING, "1.1", true,
            "采样温度，0~2；思考模式下不生效", 140),
    DS_CHAT_MAX_TOKENS(ConfigFile.AI, "ds.chat.max_tokens", ConfigType.INT, "1024", true,
            "单次回复最大 token；JSON 被截断时会内部重试更高值", 150),
    DS_CHAT_RETRY(ConfigFile.AI, "ds.chat.retry", ConfigType.INT, "2", true,
            "429/5xx/超时的最大重试次数", 160),
    DS_CHAT_MAX_CONCURRENT(ConfigFile.AI, "ds.chat.max_concurrent", ConfigType.INT, "4", true,
            "全局并发请求上限", 170),
    DS_CHAT_DAILY_TOKEN_LIMIT(ConfigFile.AI, "ds.chat.daily_token_limit", ConfigType.INT, "0", true,
            "每日 token 上限，0=不限；达到上限后停止 AI", 180),
    DS_CHAT_SESSION_EXPIRE_MINUTES(ConfigFile.AI, "ds.chat.session.expire_minutes", ConfigType.INT, "120", true,
            "内存会话空闲过期时间，单位分钟", 190),
    DS_CHAT_HISTORY_MAX_TURNS(ConfigFile.AI, "ds.chat.history.max_turns", ConfigType.INT, "20", true,
            "注入模型的历史轮数上限", 200),
    DS_CHAT_HISTORY_MAX_INPUT_TOKENS(ConfigFile.AI, "ds.chat.history.max_input_tokens", ConfigType.INT, "12000", true,
            "输入 token 预算，超出后裁剪最旧历史", 210),
    DS_CHAT_ROLE(ConfigFile.AI, "ds.chat.role", ConfigType.STRING, "春日酱", true,
            "全局默认角色卡文件名（不含 .md）；留空=无角色", 220),
    DS_CHAT_ROLE_MAX_CHARS(ConfigFile.AI, "ds.chat.role.max_chars", ConfigType.INT, "6000", true,
            "角色卡注入字符上限", 230),

    DS_CHAT_MEMORY_ENABLE(ConfigFile.AI, "ds.chat.memory.enable", ConfigType.BOOL, "true", true,
            "是否启用会话长期记忆", 270),
    DS_CHAT_MEMORY_AUTO_SAVE(ConfigFile.AI, "ds.chat.memory.auto_save", ConfigType.BOOL, "true", true,
            "是否自动保存模型输出 memory 字段中的记忆", 271),
    DS_CHAT_MEMORY_MAX_ENTRIES(ConfigFile.AI, "ds.chat.memory.max_entries", ConfigType.INT, "20", true,
            "每个会话最多保留的记忆条数", 272),
    DS_CHAT_MEMORY_MAX_VALUE_CHARS(ConfigFile.AI, "ds.chat.memory.max_value_chars", ConfigType.INT, "200", true,
            "单条记忆值的最大字符数", 273),
    DS_CHAT_MEMORY_INJECT_MAX(ConfigFile.AI, "ds.chat.memory.inject_max", ConfigType.INT, "10", true,
            "每轮最多注入多少条长期记忆", 274),
    DS_CHAT_DEDUPE_TTL_SECONDS(ConfigFile.AI, "ds.chat.dedupe_ttl_seconds", ConfigType.INT, "300", true,
            "同一 messageId 重复事件的去重窗口，单位秒", 240),
    DS_CHAT_IGNORE_USER_IDS(ConfigFile.AI, "ds.chat.ignore_user_ids", ConfigType.LIST, "", true,
            "不触发 AI 的 QQ 号黑名单，逗号分隔", 250),
    DS_CHAT_VISION_ENABLE(ConfigFile.AI, "ds.chat.vision.enable", ConfigType.BOOL, "false", true,
            "是否把图片传给 deepseek-flash 做视觉理解；关闭时只发 [图片] 占位", 260),

    DS_CHAT_GROUP_WHITELIST(ConfigFile.AI, "ds.chat.group.whitelist", ConfigType.LIST, "", true,
            "启用 DeepSeek 聊天的群号白名单，逗号分隔；留空=所有群都不启用", 300),
    DS_CHAT_GROUP_TRIGGER_AT(ConfigFile.AI, "ds.chat.group.trigger.at", ConfigType.BOOL, "true", true,
            "群里被 @ 机器人时触发", 310),
    DS_CHAT_GROUP_TRIGGER_REPLY(ConfigFile.AI, "ds.chat.group.trigger.reply", ConfigType.BOOL, "true", true,
            "群里引用/回复机器人消息时触发", 320),
    DS_CHAT_GROUP_TRIGGER_ROLE(ConfigFile.AI, "ds.chat.group.trigger.role", ConfigType.BOOL, "true", true,
            "角色卡触发词命中且语境指向机器人时触发", 330),
    DS_CHAT_GROUP_TRIGGER_CONTEXT_JUDGE(ConfigFile.AI, "ds.chat.group.trigger.context_judge", ConfigType.BOOL, "true", true,
            "触发词命中后是否做语境判定；关闭=命中即算提及", 340),
    DS_CHAT_GROUP_TRIGGER_ADDRESS_WORDS(ConfigFile.AI, "ds.chat.group.trigger.address_words", ConfigType.LIST,
            "你,您,在吗,出来,帮我,你觉得,你说,看看,怎么办,咋办", true,
            "语境判定用的呼叫词，与触发词共现时加强“在叫机器人”", 350),
    DS_CHAT_GROUP_TRIGGER_PREFIX(ConfigFile.AI, "ds.chat.group.trigger.prefix", ConfigType.STRING, "", true,
            "文本前缀触发，留空=关闭；例如 ds ", 360),
    DS_CHAT_GROUP_TRIGGER_KEYWORDS(ConfigFile.AI, "ds.chat.group.trigger.keywords", ConfigType.LIST, "", true,
            "全局补充触发词，与角色卡触发词叠加", 370),
    DS_CHAT_GROUP_ROLES(ConfigFile.AI, "ds.chat.group.roles", ConfigType.LIST, "", true,
            "群角色映射，格式 群号=角色名，多个逗号分隔；优先于全局角色", 380),
    DS_CHAT_GROUP_TRIGGER_RANDOM_PERCENT(ConfigFile.AI, "ds.chat.group.trigger.random_percent", ConfigType.INT, "0", true,
            "每条群消息随机接话概率 0~100；0=关闭", 390),
    DS_CHAT_GROUP_AMBIENT_SIZE(ConfigFile.AI, "ds.chat.group.ambient_size", ConfigType.INT, "20", true,
            "注入模型的环境上下文条数", 400),
    DS_CHAT_GROUP_MIN_REPLY_INTERVAL_MS(ConfigFile.AI, "ds.chat.group.min_reply_interval_ms", ConfigType.INT, "3000", true,
            "同一群两次回复的最小间隔，单位毫秒", 410),
    DS_CHAT_GROUP_MAX_REPLIES_PER_MINUTE(ConfigFile.AI, "ds.chat.group.max_replies_per_minute", ConfigType.INT, "6", true,
            "同一群每分钟最多回复次数", 420),
    DS_CHAT_GROUP_USER_COOLDOWN_MS(ConfigFile.AI, "ds.chat.group.user_cooldown_ms", ConfigType.INT, "10000", true,
            "同一用户连续触发的冷却时间，单位毫秒", 430),
    DS_CHAT_GROUP_MENTION_DELAY_MIN_MS(ConfigFile.AI, "ds.chat.group.mention_delay_min_ms", ConfigType.INT, "5000", true,
            "被直接叫到后的最小等待，单位毫秒", 440),
    DS_CHAT_GROUP_MENTION_DELAY_MAX_MS(ConfigFile.AI, "ds.chat.group.mention_delay_max_ms", ConfigType.INT, "15000", true,
            "被直接叫到后的最大等待，单位毫秒", 450),
    DS_CHAT_GROUP_MENTION_DELAY_CAP_MS(ConfigFile.AI, "ds.chat.group.mention_delay_cap_ms", ConfigType.INT, "30000", true,
            "提及等待窗口的总上限，单位毫秒", 460),
    DS_CHAT_GROUP_AT_ONLY_DELAY_MIN_MS(ConfigFile.AI, "ds.chat.group.at_only_delay_min_ms", ConfigType.INT, "15000", true,
            "纯 @ 无文本的最小等待，单位毫秒", 470),
    DS_CHAT_GROUP_AT_ONLY_DELAY_MAX_MS(ConfigFile.AI, "ds.chat.group.at_only_delay_max_ms", ConfigType.INT, "25000", true,
            "纯 @ 无文本的最大等待，单位毫秒", 480),
    DS_CHAT_GROUP_REPLY_AT_ONLY(ConfigFile.AI, "ds.chat.group.reply_at_only", ConfigType.BOOL, "true", true,
            "纯 @ 无文本等待结束后是否回一个“？”", 490),

    DS_CHAT_PRIVATE_ENABLE(ConfigFile.AI, "ds.chat.private.enable", ConfigType.BOOL, "false", true,
            "私聊 DeepSeek 聊天总开关", 500),
    DS_CHAT_PRIVATE_ROLES(ConfigFile.AI, "ds.chat.private.roles", ConfigType.LIST, "", true,
            "私聊角色映射，格式 QQ号=角色名，多个逗号分隔；优先于全局角色", 510),
    DS_CHAT_PRIVATE_MIN_REPLY_INTERVAL_MS(ConfigFile.AI, "ds.chat.private.min_reply_interval_ms", ConfigType.INT, "1000", true,
            "同一私聊两次回复的最小间隔，单位毫秒", 520),
    DS_CHAT_DEBOUNCE_MIN_MS(ConfigFile.AI, "ds.chat.debounce.min_ms", ConfigType.INT, "2000", true,
            "连续消息合并窗口下限，单位毫秒", 530),
    DS_CHAT_DEBOUNCE_MAX_MS(ConfigFile.AI, "ds.chat.debounce.max_ms", ConfigType.INT, "6000", true,
            "连续消息合并窗口上限，单位毫秒；0=关闭合并", 540),
    DS_CHAT_DEBOUNCE_MAX_WAIT_MS(ConfigFile.AI, "ds.chat.debounce.max_wait_ms", ConfigType.INT, "15000", true,
            "单轮合并的总等待上限，单位毫秒", 550),

    DS_CHAT_MESSAGE_MAX_COUNT(ConfigFile.AI, "ds.chat.message.max_count", ConfigType.INT, "4", true,
            "单轮最多发送的消息条数", 600),
    DS_CHAT_MESSAGE_MAX_CHARS(ConfigFile.AI, "ds.chat.message.max_chars", ConfigType.INT, "300", true,
            "单条消息最大字符数", 610),
    DS_CHAT_MESSAGE_MAX_DELAY_MS(ConfigFile.AI, "ds.chat.message.max_delay_ms", ConfigType.INT, "5000", true,
            "单条消息最大延迟，单位毫秒", 620),
    DS_CHAT_MESSAGE_FIRST_DELAY_MS(ConfigFile.AI, "ds.chat.message.first_delay_ms", ConfigType.INT, "300", true,
            "第一条消息的默认延迟，单位毫秒", 630),
    DS_CHAT_MESSAGE_TOTAL_TIMEOUT_MS(ConfigFile.AI, "ds.chat.message.total_timeout_ms", ConfigType.INT, "30000", true,
            "一轮发送的总时长上限，单位毫秒", 640),

    DS_CHAT_AGENT_ENABLE(ConfigFile.AI, "ds.chat.agent.enable", ConfigType.BOOL, "true", true,
            "是否允许模型通过 JSON call 调用白名单工具", 700),
    DS_CHAT_AGENT_MAX_STEPS(ConfigFile.AI, "ds.chat.agent.max_steps", ConfigType.INT, "2", true,
            "一轮内最多工具调用轮数", 710),
    DS_CHAT_AGENT_TOOLS(ConfigFile.AI, "ds.chat.agent.tools", ConfigType.LIST, "group_history,member_info,memory_append,memory_query,memory_forget", true,
            "可调用工具白名单，逗号分隔", 720),
    DS_CHAT_AGENT_TOOL_TIMEOUT_MS(ConfigFile.AI, "ds.chat.agent.tool_timeout_ms", ConfigType.INT, "5000", true,
            "单个工具执行超时，单位毫秒", 730),
    DS_CHAT_AGENT_MAX_RESULT_CHARS(ConfigFile.AI, "ds.chat.agent.max_result_chars", ConfigType.INT, "2000", true,
            "工具结果注入模型前的截断字符数", 740),
    DS_CHAT_AGENT_CALLS_PER_TURN(ConfigFile.AI, "ds.chat.agent.calls_per_turn", ConfigType.INT, "2", true,
            "单轮最多工具调用次数", 750),
    DS_CHAT_AGENT_CALLS_PER_MINUTE(ConfigFile.AI, "ds.chat.agent.calls_per_minute", ConfigType.INT, "6", true,
            "单会话每分钟最多工具调用次数", 760),

    DS_CHAT_PERSISTENCE_ENABLE(ConfigFile.AI, "ds.chat.persistence.enable", ConfigType.BOOL, "true", true,
            "是否把会话消息落库", 800),
    DS_CHAT_PERSISTENCE_MAX_MESSAGES_PER_SESSION(ConfigFile.AI, "ds.chat.persistence.max_messages_per_session", ConfigType.INT, "200", true,
            "每个会话保留的消息条数", 810),
    DS_CHAT_CLEANUP_RETENTION_DAYS(ConfigFile.AI, "ds.chat.cleanup.retention_days", ConfigType.INT, "30", true,
            "消息/用量数据保留天数", 820),
    DS_CHAT_BALANCE_REFRESH_MINUTES(ConfigFile.AI, "ds.chat.balance.refresh_minutes", ConfigType.INT, "10", true,
            "官方余额刷新间隔，0=不自动刷新", 830),

    // ============================ JM漫画 ============================
    // 下面三项默认值与 JmcomicService.JM_DEFAULT_PASSWORD / DEFAULT_API_DOMAIN 保持一致
    JM_PASSWORD_ZIP(ConfigFile.JM, "jm.password.zip", ConfigType.STRING, "1234", true,
            "jm本子zip包解压密码。注意：修改密码不会改变已存在的zip包密码，仅影响后续生成的zip包", 10),
    JM_PASSWORD_PDF(ConfigFile.JM, "jm.password.pdf", ConfigType.STRING, "1234", true,
            "jm本子pdf保护密码。注意：修改密码不会改变已存在的pdf文件密码，仅影响后续生成的pdf", 20),
    JM_DOWNLOAD_THREADS(ConfigFile.JM, "jm.download.threads", ConfigType.INT, "0", true,
            "下载本子时线程数量，未配置、0或小于0则等于CPU逻辑核心数量", 30),
    JM_OPERATION_PARALLEL_ENABLED(ConfigFile.JM, "jm.operation.parallel.enabled", ConfigType.BOOL, "false", true,
            "JM漫画操作是否允许不同JM ID并发执行，true表示开启并发执行", 40),
    JM_ALBUM_NAME_MAX_LENGTH(ConfigFile.JM, "jm.album.name_max_length", ConfigType.INT, "150", true,
            "jm本子文件夹名称的最大长度（bytes.length而非字符个数，不含_JM{id}后缀），本子名会作为文件夹名使用，需小于255", 50),
    JM_API_DOMAIN(ConfigFile.JM, "jm.api_domain", ConfigType.STRING, JmLineCatalog.DEFAULT_API_HOST, true,
            "JM API域名（本子详情/搜索/章节清单走的线路）。候选项来自 JM 线路清单，可点「测速」看各线路延迟后选最快的",
            60, ControlMeta.selectWithProbe(JmLineCatalog.Kind.API.code(), JmLineCatalog.apiControlOptions())),
    JM_IMAGE_DOMAIN(ConfigFile.JM, "jm.image_domain", ConfigType.STRING, JmLineCatalog.DEFAULT_IMAGE_HOST, true,
            "JM 图片域名（章节图片与封面走的图片CDN）。候选项来自 JM 线路清单，可点「测速」看各图源延迟后选最快的",
            65, ControlMeta.selectWithProbe(JmLineCatalog.Kind.IMAGE.code(), JmLineCatalog.imageControlOptions())),
    JM_SEARCH_RESULT_IMAGE_MODE(ConfigFile.JM, "jm.search_result.image_mode", ConfigType.BOOL, "false", true,
            "JM搜索结果是否以图片方式发送，true表示以图片发送搜索结果，false表示合并转发消息发送", 70),
    JM_SEARCH_RESULT_LIMIT(ConfigFile.JM, "jm.search_result.limit", ConfigType.INT, "12", true,
            "JM搜索结果返回条数上限；无效或小于等于0时：仅发送12条结果", 80),
    JM_SEARCH_HISTORY_LIMIT(ConfigFile.JM, "jm.search.history.limit", ConfigType.INT, "20", true,
            "JM在线搜索历史保存条数，超出后自动删除最旧的记录；无效或小于等于0时不保存历史", 90),
    JM_REQUEST_ALBUM_RETRY(ConfigFile.JM, "jm.request.album.retry", ConfigType.INT, "0", true,
            "请求本子详情(/album)接口遇到网络超时最大重试次数，0=不重试", 100),
    JM_REQUEST_SEARCH_RETRY(ConfigFile.JM, "jm.request.search.retry", ConfigType.INT, "0", true,
            "JM搜索(/search)接口遇到网络超时最大重试次数，0=不重试", 110),
    JM_REQUEST_CHAPTER_RETRY(ConfigFile.JM, "jm.request.chapter.retry", ConfigType.INT, "0", true,
            "请求章节详情(/chapter)接口遇到网络超时最大重试次数，0=不重试", 120),
    JM_IMAGE_ENCRYPT_ENABLED(ConfigFile.JM, "jm.image.encrypt.enabled", ConfigType.BOOL, "true", true,
            "JM漫画图片和封面是否加密保存到本地服务器，true=加密；仅影响新下载的文件，历史明文图片仍可正常读取", 130),
    JM_IMAGE_ENCRYPT_PREFIX_KB(ConfigFile.JM, "jm.image.encrypt.prefix.kb", ConfigType.INT, "32", true,
            "JM图片加密字节数（KB）：0=整图加密，大于0=仅加密文件前N KB；仅影响新下载的文件", 140),
    JM_IMAGE_COMPRESS_ENABLED(ConfigFile.JM, "jm.image.compress.enabled", ConfigType.BOOL, "true", true,
            "JM漫画图片解扰后是否重新压缩：true=有损WebP q85 / PDF内嵌JPEG q90，false=无损WebP q100 / PDF无损内嵌。只影响需要解扰的webp；未加扰章节与jpg/gif原图按站点原始字节保存（不重编码）",
            150),

    // ============================ 站点地址 ============================
    // ThirdPartyURL 中的地址常量已迁移到这里，除 identimg（识图，见 searchimg.properties）外
    URL_CONF_AGEFANS(ConfigFile.URL, "url_conf.agefans", ConfigType.STRING, "https://www.agemys.vip", true,
            "agefans网站地址，用于今日新番功能，末尾不需斜杠。备用：https://www.age.tv https://www.agemys.net", 10),
    URL_CONF_BT_SEARCH(ConfigFile.URL, "url_conf.bt_search", ConfigType.STRING, "http://www.eclzz.bio", true,
            "磁力搜索网站地址，用于bt搜索功能，末尾不需斜杠", 20),
    URL_CONF_BTBTLA_SEARCH(ConfigFile.URL, "url_conf.btbtla_search", ConfigType.STRING, "https://www.btbtla.com", true,
            "bt影视搜索站点地址", 30),
    URL_CONF_QINGYUNKE_AI_CHAT(ConfigFile.URL, "url_conf.qingyunke_ai_chat", ConfigType.STRING,
            "http://api.qingyunke.com/api.php", true, "青云客ai聊天接口", 40),
    URL_CONF_BILIBILI_BULLET_CHAR(ConfigFile.URL, "url_conf.bilibili_bullet_char", ConfigType.STRING,
            "https://api.bilibili.com/x/v1/dm/list.so", true, "b站获取弹幕接口", 50),
    URL_CONF_BILIBILI_PLAYER_CID(ConfigFile.URL, "url_conf.bilibili_player_cid", ConfigType.STRING,
            "https://api.bilibili.com/x/player/pagelist", true, "b站获取cid接口", 60),
    URL_CONF_BILIBILI_LIVE_STATUS(ConfigFile.URL, "url_conf.bilibili_live_status", ConfigType.STRING,
            "https://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids", true,
            "b站根据uid批量获取直播间状态接口", 70),
    URL_CONF_LOLICON(ConfigFile.URL, "url_conf.lolicon", ConfigType.STRING, "https://api.lolicon.app/setu/v2", true,
            "lolicon图片接口，用于pixiv下载任务", 80),
    URL_CONF_WHATS_LINK(ConfigFile.URL, "url_conf.whats_link", ConfigType.STRING,
            "https://whatslink.info/api/v1/link", true, "磁力链接预览接口", 90),
    URL_CONF_NEWS_163(ConfigFile.URL, "url_conf.news_163", ConfigType.STRING,
            "http://c.m.163.com/nc/article/headline/T1348647853363/0-40.html", true, "网易新闻头条接口", 100),
    URL_CONF_NETEASE_SEARCH_MUSIC(ConfigFile.URL, "url_conf.netease_search_music", ConfigType.STRING,
            "http://music.163.com/weapi/cloudsearch/get/web", true, "网易云搜索歌曲接口", 110),

    // ============================ 数据库 ============================
    // 数据源配置的真源。application*.yml 里的 spring.datasource 会被
    // SqliteDataSourceInitAspect 用这里的值覆盖（见该类注释）
    DATABASE_URL(ConfigFile.DATABASE, "spring.datasource.dynamic.datasource.master.url", ConfigType.STRING,
            "jdbc:sqlite::resource:data/haruhibot_server.db?journal_mode=WAL&synchronous=NORMAL", false,
            "sqlite数据库jdbc url，支持绝对路径，修改后需重启", 10),
    DATABASE_DRIVER_CLASS_NAME(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.driver-class-name",
            ConfigType.STRING, "org.sqlite.JDBC", false, "数据库驱动类名，修改后需重启", 20),
    DATABASE_DRUID_VALIDATION_QUERY(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.druid.validation-query", ConfigType.STRING, "SELECT 1", false,
            "druid连接校验语句，修改后需重启", 30),
    DATABASE_DRUID_FILTERS(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.druid.filters", ConfigType.STRING, "stat", false,
            "druid过滤器（可多选，保存时逗号拼接），修改后需重启", 40,
            ControlMeta.checkboxGroup("stat,wall,log4j,slf4j")),
    DATABASE_DRUID_TEST_ON_BORROW(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.druid.test-on-borrow", ConfigType.BOOL, "false", false,
            "druid取连接时是否校验，修改后需重启", 50),
    DATABASE_DRUID_TEST_ON_RETURN(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.druid.test-on-return", ConfigType.BOOL, "false", false,
            "druid归还连接时是否校验，修改后需重启", 60),
    DATABASE_DRUID_POOL_PREPARED_STATEMENTS(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.druid.pool-prepared-statements", ConfigType.BOOL, "false",
            false, "druid是否缓存PreparedStatement，修改后需重启", 70),
    DATABASE_DRUID_MAX_ACTIVE(ConfigFile.DATABASE,
            "spring.datasource.dynamic.datasource.master.druid.max-active", ConfigType.INT, "5", false,
            "druid最大连接数，修改后需重启", 80),

    // ============================ 聊天记录 ============================
    CHAT_RECORD_RAW_COMPRESS(ConfigFile.CHAT_RECORD, "db.chat_extend.raw_compress", ConfigType.BOOL, "true", true,
            "是否对聊天记录扩展表raw消息压缩存储", 10),
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
    /** 前端控件元数据（不声明时按值类型取默认控件） */
    private final ControlMeta control;

    ConfigKey(ConfigFile file, String key, ConfigType type, String defaultValue, boolean hot, String remark, int sort) {
        this(file, key, type, defaultValue, hot, remark, sort, null);
    }

    /**
     * @param control 前端控件；传 null 表示按值类型取默认控件（BOOL→开关、LIST→多选下拉、其余→输入框）
     */
    ConfigKey(ConfigFile file, String key, ConfigType type, String defaultValue, boolean hot, String remark,
              int sort, ControlMeta control) {
        this.file = file;
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        this.hot = hot;
        this.remark = remark;
        this.sort = sort;
        this.control = control == null ? ControlMeta.of(type) : control;
    }

    /** key -> 配置项；同一属性名可以出现在不同文件（如 dev/prod 的日志级别），此时取第一个 */
    private static final Map<String, ConfigKey> KEY_MAP;

    /** (文件, key) -> 配置项 */
    private static final Map<String, ConfigKey> FILE_KEY_MAP;

    static {
        Map<String, ConfigKey> map = new LinkedHashMap<>();
        Map<String, ConfigKey> fileKeyMap = new LinkedHashMap<>();
        for (ConfigKey value : values()) {
            map.putIfAbsent(value.key, value);
            String fileKey = value.file.getFileName() + "#" + value.key;
            ConfigKey exist = fileKeyMap.put(fileKey, value);
            if (exist != null) {
                throw new IllegalStateException("同一文件内配置key重复：" + fileKey
                        + " -> " + exist.name() + " / " + value.name());
            }
        }
        KEY_MAP = Collections.unmodifiableMap(map);
        FILE_KEY_MAP = Collections.unmodifiableMap(fileKeyMap);
    }

    /**
     * 根据key查找配置项，找不到返回null
     */
    public static ConfigKey of(String key) {
        return key == null ? null : KEY_MAP.get(key.trim());
    }

    /**
     * 按"文件 + key"查找配置项，用于同名属性出现在多个文件里的场景
     */
    public static ConfigKey of(ConfigFile file, String key) {
        if (file == null || key == null) {
            return null;
        }
        return FILE_KEY_MAP.get(file.getFileName() + "#" + key.trim());
    }

    /**
     * 该key是否被多个文件声明
     */
    public static boolean isAmbiguous(String key) {
        if (key == null) {
            return false;
        }
        String k = key.trim();
        long count = Arrays.stream(values()).filter(e -> e.key.equals(k)).count();
        return count > 1;
    }

    public static List<ConfigKey> of(ConfigFile file) {
        return Arrays.stream(values()).filter(e -> e.file == file).toList();
    }
}
