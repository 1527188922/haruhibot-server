package com.haruhi.botserver.features.ai.model;

import com.haruhi.botserver.bot.session.Bot;
import lombok.Data;

/**
 * 会话队列中的一轮输入。
 */
@Data
public class AiTurn {

    private Bot bot;
    private AiIncoming incoming;
    /**
     * 最早可开始处理的时间（提及等待/合并窗口）。
     */
    private long notBefore;
    /**
     * 合并窗口的绝对上限，超过后必须处理，不能无限等待。
     */
    private long deadline;
    /**
     * 是否由 @/引用/触发词/前缀等直接触发；false 表示随机接话或普通私聊。
     */
    private boolean direct;

    public long userId() {
        return incoming == null ? 0L : incoming.getUserId();
    }

    public boolean expired(long now) {
        return now >= notBefore || now >= deadline;
    }
}
