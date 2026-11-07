package com.haruhi.botserver.integration.onebot.model.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 元事件（心跳/生命周期）
 * 文档：https://napneko.github.io/onebot/event#元事件
 */
@Data
@NoArgsConstructor
public class MetaEvent extends BaseEvent implements Serializable {

    /**
     * heartbeat/lifecycle
     */
    private String metaEventType;
    private String subType;
    /**
     * 心跳间隔（毫秒）
     */
    private Long interval;
    /**
     * 下次心跳的预期时间戳（毫秒）
     */
    private Long timestamp;
    private Status status;

    @Data
    @NoArgsConstructor
    public static class Status implements Serializable {
        private Boolean online;
        private Boolean good;
    }
}
