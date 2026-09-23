package com.haruhi.botserver.configuration.metadata;

import com.haruhi.botserver.configuration.service.Configs;

import lombok.Getter;

/**
 * 配置所属文件
 * <p>
 * 两类位置：
 * <ul>
 *     <li><b>程序目录下</b> {@code application.yml}：需要由 Spring Boot 直接消费的配置
 *         （http端口、日志级别、资源模式等），同样可编辑</li>
 *     <li><b>程序目录下</b> {@code ./config/*.properties}：业务配置，按文件分组，可编辑</li>
 * </ul>
 * 两种格式都被 {@link Configs} 读进同一份快照（yml 的叶子节点会被拍平成 {@code a.b.c=value}），
 * 也都支持写回（yml 按行原地改值，保留注释与缩进）。
 * <p>
 * 只有这一份 yml：以前按 {@code application-dev.yml} / {@code application-prod.yml} + Spring profile 区分环境，
 * 现在改为配置项本身区分（如 {@code webui.dev-mode}），部署包也只带一个 {@code application.yml}。
 * <p>
 * {@link #order} 决定加载优先级（<b>大的覆盖小的</b>）：{@code application.yml} &lt; {@code ./config/*.properties}
 */
@Getter
public enum ConfigFile {

    APPLICATION("application.yml", "应用主配置", "http端口、日志级别、资源模式等，修改后需重启", 10),

    WEBUI("webui.properties", "WebUI", "WebUI登录、Token、监控台等，修改后需重启", 30),
    JOB("job.properties", "定时任务", "各定时任务的开关与cron表达式", 30),
    BOT("bot.properties", "机器人", "机器人自身行为：超级管理员、功能开关、访问控制", 30),
    WEBSOCKET("websocket.properties", "WebSocket", "机器人Websocket服务的认证token与连接数限制", 30),
    SEARCH_IMG("searchimg.properties", "识图", "识图引擎地址与key", 30),
    BILIBILI("bilibili.properties", "B站", "b站cookie、上传下载限制", 30),
    AI("ai.properties", "AI", "AI模型（千问、DeepSeek）相关配置", 30),
    JM("jm.properties", "JM漫画", "JM漫画下载、搜索相关配置", 30),
    URL("url.properties", "站点地址", "第三方站点与接口地址", 30),
    DATABASE("database.properties", "数据库", "数据源配置（jdbc url、驱动、druid参数），修改后需重启", 30),
    CHAT_RECORD("chat_record.properties", "聊天记录", "聊天记录存储相关配置", 30),
    ;

    /** 文件名，位于程序目录（yml）或程序目录下的 config 目录（properties） */
    private final String fileName;
    /** 前端展示名 */
    private final String displayName;
    /** 文件说明 */
    private final String remark;
    /** 加载优先级，大的覆盖小的 */
    private final int order;

    ConfigFile(String fileName, String displayName, String remark, int order) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.remark = remark;
        this.order = order;
    }

    public boolean isYaml() {
        String name = fileName.toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    /**
     * 是否是 {@code application*.yml}（位于程序目录，而不是 ./config/ 下）
     */
    public boolean isSpringApplicationFile() {
        return isYaml() && fileName.startsWith("application");
    }

    /**
     * 按加载优先级升序返回（后者覆盖前者）
     */
    public static ConfigFile[] inLoadOrder() {
        ConfigFile[] files = values().clone();
        java.util.Arrays.sort(files, java.util.Comparator.comparingInt(ConfigFile::getOrder));
        return files;
    }
}
