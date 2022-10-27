package com.haruhi.botServer.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class CacheMap<K,V> implements Serializable {

    private transient Cache<K,V> cache;

    /**
     * 有参构造
     * @param expireTime 过期时间
     * @param timeUnit 过期时间单位
     * @param maximumSize 缓存最大容量
     */
    public CacheMap(long expireTime, TimeUnit timeUnit, long maximumSize){
        cache = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expireTime, timeUnit)
                .build();
    }

    public V get(K key) {
        return cache.getIfPresent(key);
    }

    public V get(K key, Function<K, V> function) {
        return cache.get(key, function);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public void remove(K key) {
        cache.invalidate(key);
    }

    public void removeAll() {
        cache.invalidateAll();
    }

    public CacheStats stats() {
        return cache.stats();
    }

    public long size() {
        return cache.estimatedSize();
    }
}
