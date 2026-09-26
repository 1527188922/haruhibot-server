package com.haruhi.botserver.infrastructure.web.websocket;

/**
 * webui WebSocket 主题扩展点
 * <p>
 * 前端订阅某个主题后，服务端先回 subscribed，再补发一条首帧
 * (例如 jm.task 主题补发当前任务快照)，之后由功能模块按变化主动推送
 */
public interface WebuiWsTopicProvider {

    /**
     * 主题名，如 jm.task
     */
    String topic();

    /**
     * 订阅后立即补发的首帧，返回 null 表示不需要
     */
    WebuiWsMessage initialMessage();
}
