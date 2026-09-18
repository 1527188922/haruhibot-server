package com.haruhi.botServer.config.service;

import com.haruhi.botServer.config.config.ConfigKey;

/**
 * 一次配置变更
 *
 * @param key   发生变更的配置项
 * @param oldValue 旧值（变更前快照中的值）
 * @param newValue 新值
 */
public record ConfigChange(ConfigKey key, String oldValue, String newValue) {

    public String keyName() {
        return key.getKey();
    }

    public boolean changed() {
        return !java.util.Objects.equals(oldValue, newValue);
    }
}
