package com.haruhi.botserver.infrastructure.web.websocket;

/**
 * webui WebSocket 命令扩展点
 * <p>
 * 功能模块实现该接口并注册为 Spring Bean 即可处理前端发来的消息，基础设施层不反向依赖功能包。
 * 通道级命令(ping/subscribe/unsubscribe)由 {@link WebuiWsHandler} 直接处理，不走这里
 */
public interface WebuiWsCommandHandler {

    /**
     * 是否处理该消息类型
     */
    boolean supports(String type);

    void handle(WebuiWsCommandContext context);
}
