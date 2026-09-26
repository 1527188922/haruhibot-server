package com.haruhi.botserver.configuration.service;

import java.util.List;

/**
 * 一次"刷新"的结果
 *
 * @param fileChanged 文件内容自上次加载后是否变过（false 表示改动已被自动重载，或者文件本来就没动）
 * @param changes     声明的配置值差异（按文件 / 按key过滤后）
 */
public record ConfigRefreshResult(boolean fileChanged, List<ConfigChange> changes) {

    /** 是否有配置值发生变化 */
    public boolean changed() {
        return !changes.isEmpty();
    }
}
