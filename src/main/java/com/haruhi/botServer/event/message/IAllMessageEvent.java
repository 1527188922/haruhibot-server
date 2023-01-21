package com.haruhi.botServer.event.message;

import com.haruhi.botServer.dto.gocq.response.Message;
import org.springframework.web.socket.WebSocketSession;

/**
 * 实现这个接口的类
 * 群聊 私聊消息都能收到
 */
public interface IAllMessageEvent extends IMessageEvent {
    /**
     * 群聊私聊都触发
     * @param message 由go-cqhttp发来的json串 转换过来的java bean
     * @param command 命令 实际上就是对方发来的消息 message 对象中也可以获得
     */
    boolean onMessage(WebSocketSession session,Message message, String command);
}
