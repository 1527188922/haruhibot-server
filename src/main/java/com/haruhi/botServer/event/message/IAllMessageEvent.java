package com.haruhi.botServer.event.message;

import com.haruhi.botServer.dto.qqclient.Message;
import com.haruhi.botServer.ws.Bot;

/**
 * 实现这个接口的类
 * 群聊 私聊消息都能收到
 */
public interface IAllMessageEvent extends IMessageEvent {
    /**
     * 群聊私聊都触发
     * @param message 由go-cqhttp发来的json串 转换过来的java bean
     */
    boolean onMessage(Bot bot, Message message);
}
