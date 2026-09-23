package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.Message;
import com.haruhi.botserver.bot.session.Bot;

/**
 * 实现这个接口的类,都能收到戳一戳消息
 */
public interface IPokeHandler extends INoticeHandler {

    void onPoke(Bot bot, Message message);
}
