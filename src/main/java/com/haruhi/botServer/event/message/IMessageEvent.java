package com.haruhi.botServer.event.message;

/**
 * 这是一个事件类型的顶级接口
 * 普通消息处理类都实现这个接口
 */
public interface IMessageEvent {
    /**
     * 权重
     * 值越大 优先匹配
     * @return
     */
    int weight();

    String funName();
}
