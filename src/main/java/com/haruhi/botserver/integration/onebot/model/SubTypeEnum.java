package com.haruhi.botserver.integration.onebot.model;

/**
 * 事件子类型枚举
 * 文档：https://napneko.github.io/onebot/event
 */
public enum SubTypeEnum {
    // 戳一戳
    poke,
    // 群名片变更
    group_name,
    // 群名变更
    title,
    // 设置/取消群管理员
    set,
    unset,
    // 被批准进入/同意添加好友(管理直接邀请也是这个)
    approve,
    // 自行离开
    leave,
    // 被踢出
    kick,
    // 管理员主动入群/被邀请入群
    invite,
    // 机器人被踢
    kick_me,
    // 群解散
    disband,
    // 禁言/解除禁言
    ban,
    lift_ban,
    // 添加/删除好友
    add,
    delete,
    // 启用/禁用
    enable,
    disable,
    // 普通成员
    normal,
    // 匿名
    anonymous,
    // 精华消息通知
    notice,
    // 好友请求/群请求
    friend,
    group,
    // 荣誉
    honor,
    // 幸运王
    lucky_king,
    // 灰条
    gray_tip,
    // 输入状态
    input_status,
    // 点赞
    profile_like,

    connect
}
