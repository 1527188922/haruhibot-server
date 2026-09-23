package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.session.Bot;

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
