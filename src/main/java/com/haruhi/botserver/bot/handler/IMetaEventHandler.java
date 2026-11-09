package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.bot.session.Bot;
import com.haruhi.botserver.integration.onebot.model.event.MetaEvent;

/**
 * 元事件处理类（心跳/生命周期）
 * 内置的前置状态处理完成后，由 MetaEventDispatcher 统一分发给所有实现类
 */
public interface IMetaEventHandler {

    /**
     * 是否处理该元事件
     */
    boolean support(MetaEvent event);

    void onMetaEvent(Bot bot, MetaEvent event);
}
