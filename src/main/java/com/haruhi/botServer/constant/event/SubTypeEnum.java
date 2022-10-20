package com.haruhi.botServer.constant.event;

public enum SubTypeEnum {
    // 戳一戳
    poke,
    // 被批准进入(管理直接邀请也是这个)
    approve,
    // 自行离开
    leave,
    // 被踢出
    kick,

    connect
}
