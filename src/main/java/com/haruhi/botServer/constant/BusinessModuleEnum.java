package com.haruhi.botServer.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BusinessModuleEnum {
    SYSTEM("系统"),
    DATABASE("数据库管理"),
    LOG_MONITOR("日志监控"),
    BOT_WS("机器人WebSocket"),
    CHAT_RECORD("聊天记录"),
    WORD_STRIP("词条"),
    CUSTOM_REPLY("自定义回复"),
    PIXIV("Pixiv"),
    IMAGE_SEARCH("识图"),
    BILIBILI("B站解析"),
    MUSIC("点歌"),
    NEWS("新闻"),
    JMCOMIC("jmcomic"),
    JOB("定时任务"),
    AUTH("认证");

    private final String name;
}
