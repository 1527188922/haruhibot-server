package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.event.RequestEvent;
import com.haruhi.botserver.bot.session.Bot;

/**
 * 请求事件（加好友/加群）处理类
 */
public interface IRequestHandler {

    /**
     * 是否处理该请求事件
     */
    boolean support(RequestEvent event);

    void onRequest(Bot bot, RequestEvent event);
}
