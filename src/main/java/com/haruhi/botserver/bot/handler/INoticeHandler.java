package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;

/**
 * 通知事件处理类
 * 按 noticeType/subType 判断是否处理（support），由 NoticeDispatcher 统一分发
 */
public interface INoticeHandler {

    /**
     * 是否处理该通知事件
     */
    boolean support(NoticeEvent event);

    void onNotice(Bot bot, NoticeEvent event);
}
