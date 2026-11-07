package com.haruhi.botserver.integration.onebot.model;

/**
 * 上报类型枚举
 * 文档：https://napneko.github.io/onebot/basic_event
 */
public enum PostTypeEnum {
    // 普通消息
    message,
    // 机器人自身发送的消息
    message_sent,
    // bot通知
    notice,
    // 请求（加好友/加群）
    request,
    // 系统/程序事件
    meta_event
}
