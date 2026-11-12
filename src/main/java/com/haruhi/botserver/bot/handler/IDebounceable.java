package com.haruhi.botserver.bot.handler;

/**
 * 可防抖处理器（与具体 postType 无关）
 * 实现该接口后，由分发器统一通过 DebounceSupport 做防抖判断
 *
 * @param <T> 处理器对应的事件类型
 */
public interface IDebounceable<T> {

    /**
     * 获取本次事件的防抖参数
     * 返回 null 表示本次事件不防抖
     */
    DebounceParam debounceParam(T event);
}
