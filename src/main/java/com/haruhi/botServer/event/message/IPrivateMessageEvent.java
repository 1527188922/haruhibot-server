package com.haruhi.botServer.event.message;

import com.haruhi.botServer.dto.gocq.response.Message;
import org.springframework.web.socket.WebSocketSession;

/**
 * 实现这个接口的类
 * 都能收到私聊消息
 */
public interface IPrivateMessageEvent extends IMessageEvent {
    /**
     * 私聊触发
     * @param message
     * @param command
     */
    boolean onPrivate(WebSocketSession session,Message message, String command);
}
