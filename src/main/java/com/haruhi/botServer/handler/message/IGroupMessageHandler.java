package com.haruhi.botServer.handler.message;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.ws.Bot;

/**
 * 实现这接口的类
 * 都能收到群消息
 */
public interface IGroupMessageHandler extends IMessageHandler {
    /**
     * 群聊触发
     * @param message
     */
    boolean onGroup(Bot bot, Message message);
}
