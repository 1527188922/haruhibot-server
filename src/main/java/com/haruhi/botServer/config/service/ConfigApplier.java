package com.haruhi.botServer.config.service;

import com.haruhi.botServer.config.config.ConfigKey;

import java.util.Collection;

/**
 * 配置变更订阅者
 * <p>
 * 那些<b>必须持有配置值</b>的组件（连接池、HttpClient、定时任务触发器等）实现本接口，
 * 由 {@link ConfigHub} 在配置变更后按 key 精确通知。
 * <p>
 * 只读取 {@link com.haruhi.botServer.config.config.Configs} 的代码不需要实现本接口，
 * 因为它每次读到的都是最新快照。
 * <p>
 * 实现类必须是 Spring bean，异常会被 ConfigHub 捕获，不影响其他订阅者。
 */
public interface ConfigApplier {

    /**
     * 关心哪些配置项
     */
    Collection<ConfigKey> keys();

    /**
     * 配置变更回调
     *
     * @param change 本次变更（包含旧值与新值）
     */
    void onConfigChange(ConfigChange change);

    /**
     * 是否只在值真正发生变化时收到通知，默认true
     */
    default boolean onlyOnChanged() {
        return true;
    }
}
