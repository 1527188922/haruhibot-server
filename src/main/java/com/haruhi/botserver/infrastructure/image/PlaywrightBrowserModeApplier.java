package com.haruhi.botserver.infrastructure.image;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.ConfigApplier;
import com.haruhi.botserver.configuration.service.ConfigChange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * {@code playwright.skip-browser-download-mode} 的热更新
 * <p>
 * {@link HtmlToImageUtils} 会把"要不要跳过浏览器下载"缓存在静态字段里（模式一旦探测出来就固定，
 * 否则每次截图都可能重跑一遍长达 10 分钟的浏览器安装流程），所以配置变更时要把这份缓存清掉，
 * 下一次截图才会按新配置重新决策：
 * <ul>
 *     <li>{@code 0} 自动：正常模式优先，失败降级为跳过下载（改用系统浏览器）</li>
 *     <li>{@code 1} 强制跳过下载：直接用系统浏览器</li>
 *     <li>{@code 2} 强制正常模式：使用 Playwright 自带的浏览器</li>
 * </ul>
 * 注意"下一次截图才生效"：已经打开的 Playwright 实例不受影响，也不需要重启应用。
 */
@Slf4j
@Component
public class PlaywrightBrowserModeApplier implements ConfigApplier {

    @Override
    public Collection<ConfigKey> keys() {
        return Set.of(ConfigKey.PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD_MODE);
    }

    @Override
    public void onConfigChange(ConfigChange change) {
        log.info("浏览器下载模式配置变更：{} -> {}，清除模式缓存，下一次截图按新配置重新决策",
                change.oldValue(), change.newValue());
        HtmlToImageUtils.resetBrowserModeCache();
    }
}
