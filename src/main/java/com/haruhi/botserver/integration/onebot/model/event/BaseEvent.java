package com.haruhi.botserver.integration.onebot.model.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 所有上报事件的公共字段
 * 文档：https://napneko.github.io/onebot/event
 */
@Data
@NoArgsConstructor
public class BaseEvent implements Serializable {

    /**
     * 事件发生的时间戳（秒）
     */
    private Long time;
    /**
     * 收到事件的机器人QQ号
     */
    private Long selfId;
    /**
     * 上报类型 message/message_sent/notice/request/meta_event
     */
    private String postType;
}
