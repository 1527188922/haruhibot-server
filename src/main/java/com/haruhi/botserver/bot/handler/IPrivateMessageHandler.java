package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.session.Bot;

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
