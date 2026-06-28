package com.haruhi.botServer.handler.message;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.ws.Bot;

/**
 * 实现这个接口的类
 * 都能收到私聊消息
 */
public interface IPrivateMessageHandler extends IMessageHandler {
    /**
     * 私聊触发
     * @param message
     */
    boolean onPrivate(Bot bot, Message message);
}
