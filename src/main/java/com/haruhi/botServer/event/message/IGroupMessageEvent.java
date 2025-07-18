package com.haruhi.botServer.event.message;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.ws.Bot;

/**
 * 实现这接口的类
 * 都能收到群消息
 */
public interface IGroupMessageEvent extends IMessageEvent {
    /**
     * 群聊触发
     * @param message
     */
    boolean onGroup(Bot bot, Message message);
}
