package com.haruhi.botServer.config.config;

import lombok.Getter;

/**
 * 配置所属文件（位于 {@code ./config/} 目录下）
 * <p>
 * 支持 {@code .properties} 与 {@code .yml/.yaml} 两种格式：
 * <ul>
 *     <li>业务配置一律用 properties（人可直接编辑，程序能按行保留注释）</li>
 *     <li>需要交给 Spring Boot 直接消费的配置（如 server.port）用 yml：
 *         Configs 会把 yml 里的叶子节点拍平成 {@code a.b.c=value} 一并放进快照，
 *         同时由 {@code spring.config.import} 交给 Spring Environment</li>
 * </ul>
 * 前端「配置管理」页按文件分大类展示
 */
@Getter
public enum ConfigFile {

    WEBUI("webui.properties", "WebUI", "WebUI登录、Token、监控台等，修改后需重启"),
    SERVER("server.yml", "服务端(yml)", "http端口等需要由Spring直接读取的配置，修改后需重启"),
    JOB("job.properties", "定时任务", "各定时任务的开关与cron表达式"),
    BOT("bot.properties", "机器人", "机器人自身行为：超级管理员、功能开关、访问控制"),
    WEBSOCKET("websocket.properties", "WebSocket", "机器人Websocket服务的认证token与连接数限制"),
    SEARCH_IMG("searchimg.properties", "识图", "识图引擎地址与key、图源站点"),
    BILIBILI("bilibili.properties", "B站", "b站cookie、上传下载限制"),
    AI("ai.properties", "AI", "AI模型（千问、DeepSeek）相关配置"),
    JM("jm.properties", "JM漫画", "JM漫画下载、搜索相关配置"),
    URL("url.properties", "站点地址", "第三方站点地址"),
    DB("db.properties", "数据库", "数据存储相关配置"),
    ;

    /** 文件名，位于 ./config/ 目录下 */
    private final String fileName;
    /** 前端展示名 */
    private final String displayName;
    /** 文件说明 */
    private final String remark;

    ConfigFile(String fileName, String displayName, String remark) {
        this.fileName = fileName;
        this.displayName = displayName;
        this.remark = remark;
    }

    public boolean isYaml() {
        String name = fileName.toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }
}
