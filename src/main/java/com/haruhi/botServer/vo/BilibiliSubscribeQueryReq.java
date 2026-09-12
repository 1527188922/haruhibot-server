package com.haruhi.botServer.vo;

import lombok.Data;

/**
 * bilibili订阅查询条件
 * 列表不分页，因此不需要分页参数
 */
@Data
public class BilibiliSubscribeQueryReq {

    /**
     * 订阅的目标id(b站主播uid)
     */
    private Long uid;
    /**
     * 推送消息的机器人qq号
     */
    private Long selfId;
    /**
     * b站主播昵称，模糊查询
     */
    private String uname;
    /**
     * 订阅类型
     */
    private String subType;
    /**
     * 是否启用
     */
    private Integer enableStatus;
    /**
     * 下播是否推送
     */
    private Integer offNotify;
}
