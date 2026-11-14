package com.haruhi.botserver.bot.dispatch;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.haruhi.botserver.bot.handler.DebounceParam;
import com.haruhi.botserver.bot.handler.IDebounceable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 事件防抖支持
 * 与 postType 无关，任意分发器均可复用；窗口内同一处理器、同一 key 的事件只放行第一条
 */
@Component
public class DebounceSupport {

    /**
     * 防抖缓存：key -> 过期时间戳
     * 事件由虚拟线程并发分发，必须使用原子操作判断，避免重复事件同时通过
     */
    private final Cache<String, Long> cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    /**
     * 判断本次事件是否应被防抖丢弃
     * 处理器未实现 IDebounceable，或 debounceParam 返回 null 时，一律放行
     *
     * @param handler 处理器实例
     * @param event   本次事件
     * @return true 表示处于防抖窗口内，应丢弃；false 表示放行
     */
    @SuppressWarnings("unchecked")
    public <T> boolean isDebounced(Object handler, T event) {
        if (!(handler instanceof IDebounceable<?> debounceable)) {
            return false;
        }
        DebounceParam param = ((IDebounceable<T>) debounceable).debounceParam(event);
        if (param == null) {
            return false;
        }
        String cacheKey = handler.getClass().getName() + ":" + param.getKey();
        long now = System.currentTimeMillis();
        long expireAt = now + param.getWindowMillis();
        AtomicBoolean debounced = new AtomicBoolean(true);
        cache.asMap().compute(cacheKey, (key, oldExpireAt) -> {
            if (oldExpireAt != null && oldExpireAt > now) {
                return oldExpireAt;
            }
            debounced.set(false);
            return expireAt;
        });
        return debounced.get();
    }
}
