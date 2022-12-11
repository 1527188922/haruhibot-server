package com.haruhi.botServer.cache;

import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class CacheSet<E> implements Serializable {

    private final transient CacheMap<E,Object> cacheMap;
    private final static Object PRESENT = new Object();

    /**
     * 有参构造
     * @param expireTime 过期时间
     * @param timeUnit 过期时间单位
     * @param maximumSize 缓存最大容量
     */
    public CacheSet(long expireTime, TimeUnit timeUnit, long maximumSize){
        cacheMap = new CacheMap<>(expireTime,timeUnit,maximumSize);
    }

    public boolean contains(E e) {
        return cacheMap.get(e) != null;
    }

    public void add(E e) {
        cacheMap.put(e, PRESENT);
    }

    public void remove(E e) {
        cacheMap.remove(e);
    }

    public void removeAll() {
        cacheMap.removeAll();
    }

    public CacheStats stats() {
        return cacheMap.stats();
    }

    public long size() {
        return cacheMap.size();
    }
}
