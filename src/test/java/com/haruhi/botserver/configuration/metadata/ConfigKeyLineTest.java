package com.haruhi.botserver.configuration.metadata;

import com.haruhi.botserver.features.jmcomic.util.JmLineCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 两个 JM 域名配置项的「控件元数据」接线测试。
 *
 * <p>配置管理页只认配置项里下发的 {@code control}，所以"下拉 + 可自定义 + 支持测速"
 * 这三件事必须落在 {@link ConfigKey} 的声明里；写漏一项前端就会退化成输入框、没有测速按钮。
 */
class ConfigKeyLineTest {

    @Test
    void apiDomainIsSelectableWithProbe() {
        assertLineKey(ConfigKey.JM_API_DOMAIN, "JM_API", JmLineCatalog.DEFAULT_API_HOST,
                JmLineCatalog.apiLines().size());
    }

    @Test
    void imageDomainIsSelectableWithProbe() {
        assertLineKey(ConfigKey.JM_IMAGE_DOMAIN, "JM_IMAGE", JmLineCatalog.DEFAULT_IMAGE_HOST,
                JmLineCatalog.imageLines().size());
    }

    @Test
    void newImageDomainKeySitsInJmFileAndIsHotReloadable() {
        assertEquals(ConfigFile.JM, ConfigKey.JM_IMAGE_DOMAIN.getFile());
        assertTrue(ConfigKey.JM_IMAGE_DOMAIN.isHot(), "图片域名要能改完立即生效");
        assertEquals("jm.image_domain", ConfigKey.JM_IMAGE_DOMAIN.getKey());
        assertNotNull(ConfigKey.of("jm.image_domain"));
    }

    private static void assertLineKey(ConfigKey key, String probe, String defaultHost, int optionCount) {
        ControlMeta control = key.getControl();
        assertEquals(ControlType.SELECT, control.getType(), key.getKey() + " 必须是下拉框");
        assertFalse(control.isMultiple(), key.getKey() + " 是单选");
        assertTrue(control.isAllowCustom(), "清单外的历史域名也要能保留，下拉必须允许自定义值");
        assertEquals(probe, control.getProbe(), "测速类型要写进控件元数据，前端才能回传");
        assertEquals(optionCount, control.getOptions().size(), "候选项数量必须与线路清单一致");
        assertEquals(defaultHost, key.getDefaultValue());
        assertEquals(defaultHost, control.getOptions().get(0).getValue(), "默认线路排第一");
        // 显示名里带域名，用户只看下拉也能分辨线路
        assertTrue(control.getOptions().get(0).getLabel().contains(defaultHost));
    }
}
