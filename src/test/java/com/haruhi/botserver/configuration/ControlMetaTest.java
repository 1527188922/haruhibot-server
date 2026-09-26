package com.haruhi.botserver.configuration;

import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.metadata.ConfigType;
import com.haruhi.botserver.configuration.metadata.ControlMeta;
import com.haruhi.botserver.configuration.metadata.ControlType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 值类型（{@link ConfigType}）与控件类型（{@link ControlType}）解耦的元数据测试
 */
class ControlMetaTest {

    @Test
    void 没声明控件时按值类型取默认控件() {
        assertEquals(ControlType.SWITCH, ControlMeta.of(ConfigType.BOOL).getType());
        assertEquals(ControlType.INPUT, ControlMeta.of(ConfigType.STRING).getType());
        assertEquals(ControlType.INPUT, ControlMeta.of(ConfigType.INT).getType());
        assertEquals(ControlType.INPUT, ControlMeta.of(ConfigType.SECRET).getType());
        assertEquals(ControlType.INPUT, ControlMeta.of(ConfigType.JSON).getType());

        // LIST 默认：多选下拉 + 允许自定义值（候选为空，相当于标签输入框）
        ControlMeta list = ControlMeta.of(ConfigType.LIST);
        assertEquals(ControlType.SELECT, list.getType());
        assertTrue(list.isMultiple());
        assertTrue(list.isAllowCustom());
        assertTrue(list.getOptions().isEmpty());
    }

    @Test
    void 同一个值类型可以配不同控件() {
        // STRING：输入框 / 单选下拉 / 单选组 / 复选组
        assertEquals(ControlType.SELECT, ControlMeta.select("a:A,b:B").getType());
        assertEquals(ControlType.RADIO, ControlMeta.radio("a:A,b:B").getType());
        assertEquals(ControlType.CHECKBOX, ControlMeta.checkboxGroup("a:A,b:B").getType());
        assertEquals(ControlType.INPUT, ControlMeta.input().getType());

        // BOOL：开关 / 单个复选框
        assertEquals(ControlType.SWITCH, ControlMeta.switcher().getType());
        ControlMeta checkbox = ControlMeta.checkbox();
        assertEquals(ControlType.CHECKBOX, checkbox.getType());
        assertFalse(checkbox.isMultiple(), "单个复选框不是多选");
    }

    @Test
    void 下拉支持单选多选与自定义值三种模式() {
        ControlMeta single = ControlMeta.select("a:A,b:B");
        assertFalse(single.isMultiple());
        assertFalse(single.isAllowCustom());

        ControlMeta multiple = ControlMeta.selectMultiple("a:A,b:B");
        assertTrue(multiple.isMultiple());
        assertFalse(multiple.isAllowCustom());

        ControlMeta customSingle = ControlMeta.selectCustom(false, "a:A");
        assertFalse(customSingle.isMultiple());
        assertTrue(customSingle.isAllowCustom(), "单选也能允许自定义值");

        ControlMeta customMultiple = ControlMeta.selectCustom(true, null);
        assertTrue(customMultiple.isMultiple());
        assertTrue(customMultiple.isAllowCustom());
        assertTrue(customMultiple.getOptions().isEmpty(), "候选可以为空，只靠自定义值");
    }

    @Test
    void 候选项按值冒号显示名解析() {
        List<ControlMeta.Option> options = ControlMeta.radio("0:自动判断,1:强制跳过下载,B").getOptions();
        assertEquals(3, options.size());
        assertEquals("0", options.get(0).getValue());
        assertEquals("自动判断", options.get(0).getLabel());
        assertEquals("B", options.get(2).getValue());
        assertEquals("B", options.get(2).getLabel(), "不写显示名时显示名=值");

        assertTrue(ControlMeta.select(null).getOptions().isEmpty());
        assertTrue(ControlMeta.select("  ").getOptions().isEmpty());
        // 显示名为空时回落到值
        assertEquals("x", ControlMeta.select("x:").getOptions().get(0).getLabel());
    }

    @Test
    void 现有配置项声明了预期控件() {
        // 日志级别：单选下拉
        assertEquals(ControlType.SELECT, ConfigKey.LOGGING_LEVEL.getControl().getType());
        assertEquals(5, ConfigKey.LOGGING_LEVEL.getControl().getOptions().size());
        assertFalse(ConfigKey.LOGGING_LEVEL.getControl().isMultiple());

        // 浏览器下载模式：单选框组
        assertEquals(ControlType.RADIO, ConfigKey.PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD_MODE.getControl().getType());
        assertEquals(3, ConfigKey.PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD_MODE.getControl().getOptions().size());

        // druid 过滤器：复选组（多值，逗号拼接）
        assertEquals(ControlType.CHECKBOX, ConfigKey.DATABASE_DRUID_FILTERS.getControl().getType());
        assertTrue(ConfigKey.DATABASE_DRUID_FILTERS.getControl().isMultiple());
        assertEquals(ConfigType.STRING, ConfigKey.DATABASE_DRUID_FILTERS.getType(), "值类型仍是STRING");

        // LIST：多选下拉 + 自定义值
        assertEquals(ControlType.SELECT, ConfigKey.BOT_SUPERUSERS.getControl().getType());
        assertTrue(ConfigKey.BOT_SUPERUSERS.getControl().isMultiple());
        assertTrue(ConfigKey.BOT_SUPERUSERS.getControl().isAllowCustom());

        // BOOL：开关（默认）；SECRET：输入框（默认）
        assertEquals(ControlType.SWITCH, ConfigKey.BOT_UPLOAD_FILE_PARALLEL.getControl().getType());
        assertEquals(ControlType.INPUT, ConfigKey.WEBUI_LOGIN_PASSWORD.getControl().getType());
    }
}
