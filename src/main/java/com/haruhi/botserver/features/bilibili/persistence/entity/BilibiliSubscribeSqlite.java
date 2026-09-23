package com.haruhi.botserver.features.bilibili.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botserver.features.bilibili.model.BilibiliSubscribeTypeEnum;
import com.haruhi.botserver.infrastructure.persistence.DataBaseConst;
import com.haruhi.botserver.features.contacts.model.AvatarInfo;
import lombok.Data;

/**
 * 订阅推送配置
 * 目前用于b站主播开播推送，sub_type为live
 */
@Data
@TableName(value = DataBaseConst.T_BILIBILI_SUBSCRIBE)
public class BilibiliSubscribeSqlite extends AvatarInfo {

    /**
     * 启用
     */
    public static final int ENABLE_STATUS_ENABLE = 1;
    /**
     * 禁用
     */
    public static final int ENABLE_STATUS_DISABLE = 0;
    /**
     * 推送下播消息
     */
    public static final int OFF_NOTIFY_ENABLE = 1;
    /**
     * 不推送下播消息
     */
    public static final int OFF_NOTIFY_DISABLE = 0;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订阅的目标id，如b站主播uid
     */
    private Long uid;

    /**
     * b站直播间id，由定时任务每次请求到直播状态数据时更新
     */
    private Long roomId;

    /**
     * 订阅类型 {@link BilibiliSubscribeTypeEnum}
     */
    private String subType;

    /**
     * b站主播昵称，由定时任务每次请求到直播状态数据时更新
     */
    private String uname;

    /**
     * b站主播头像，由定时任务每次请求到直播状态数据时更新
     */
    private String face;

    /**
     * 推送消息的机器人qq号
     */
    private Long selfId;

    /**
     * 推送到哪些群，多个群号用逗号分割
     */
    private String groupIds;

    /**
     * 开播消息需要@全体成员的群，多个群号用逗号分割，始终是group_ids的子集
     * 为空表示所有群都不@全体成员(默认关闭)
     */
    private String atAllGroupIds;

    /**
     * 私聊推送给哪些人，多个qq号用逗号分割
     */
    private String friendIds;

    /**
     * 是否启用 {@link #ENABLE_STATUS_ENABLE}
     */
    private Integer enableStatus;

    /**
     * 下播是否推送 {@link #OFF_NOTIFY_ENABLE}
     */
    private Integer offNotify;

    private String createTime;

    private String updateTime;
}
