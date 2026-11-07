package com.haruhi.botserver.integration.onebot.model;

/**
 * 通知类型枚举
 * 文档：https://napneko.github.io/onebot/event#通知事件
 */
public enum NoticeTypeEnum {
    // 戳一戳/群名片变更/群名变更等
    notify,
    // 群文件上传
    group_upload,
    // 群管理员变动
    group_admin,
    // 群成员减少
    group_decrease,
    // 群成员增加
    group_increase,
    // 群禁言
    group_ban,
    // 好友添加
    friend_add,
    // 群消息撤回
    group_recall,
    // 好友消息撤回
    friend_recall,
    // 群名片变更
    group_card,
    // 群消息表情回应
    group_msg_emoji_like,
    // 机器人离线
    bot_offline,
    // 群荣誉变更
    honor,
    // 群幸运王
    lucky_king,
    // 群精华消息
    essence
}
