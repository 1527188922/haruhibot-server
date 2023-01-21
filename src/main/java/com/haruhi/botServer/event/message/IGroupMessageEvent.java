package com.haruhi.botServer.event.message;

import com.haruhi.botServer.dto.gocq.response.Message;
import org.springframework.web.socket.WebSocketSession;

/**
 * 实现这接口的类
 * 都能收到群消息
 */
public interface IGroupMessageEvent extends IMessageEvent {
    /**
     * 群聊触发
     * @param message
     * @param command
     */
    boolean onGroup(WebSocketSession session,Message message, String command);
}
