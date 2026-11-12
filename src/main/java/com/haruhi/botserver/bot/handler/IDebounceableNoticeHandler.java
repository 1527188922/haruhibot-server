package com.haruhi.botserver.bot.handler;

import com.haruhi.botserver.integration.onebot.model.event.NoticeEvent;

/**
 * 可防抖的通知处理类
 * 防抖语义：窗口内同一 key 只处理第一条通知，后续重复通知由 NoticeDispatcher 直接丢弃
 */
public interface IDebounceableNoticeHandler extends INoticeHandler, IDebounceable<NoticeEvent> {
}
