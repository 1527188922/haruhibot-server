package com.haruhi.botserver.configuration.metadata;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置项的前端<b>控件元数据</b>：用什么控件渲染、候选项是什么、能不能多选/自定义值
 * <p>
 * 在 {@link ConfigKey} 里按需声明，没声明时由 {@link #of(ConfigType)} 按值类型取默认控件。
 * 前端「配置管理」页只认这份元数据，因此同一套后端代码可以给不同配置项配不同控件。
 * <p>
 * 常用写法（声明顺序：{@code ..., sort, ControlMeta.xxx(...)}）：
 * <pre>
 * // 输入框（默认，不用写）
 * X(..., ConfigType.STRING, "v", true, "说明", 10)
 * // 开关（BOOL 默认，不用写）
 * X(..., ConfigType.BOOL, "true", true, "说明", 10)
 * // 下拉单选：值:显示名，逗号分隔，不写显示名时显示名=值
 * X(..., ConfigType.STRING, "0", true, "说明", 10, ControlMeta.select("0:关闭,1:开启"))
 * // 下拉多选（候选固定，不允许自定义）
 * X(..., ConfigType.LIST, "a", true, "说明", 10, ControlMeta.selectMultiple("a,b,c"))
 * // 下拉多选 + 可自定义值（LIST 的默认控件就是它，候选可以为空）
 * X(..., ConfigType.LIST, "", true, "说明", 10, ControlMeta.selectCustom(true, null))
 * // 复选组（多值）/ 单个复选框（布尔）
 * X(..., ConfigType.STRING, "stat", true, "说明", 10, ControlMeta.checkboxGroup("stat,wall,log4j"))
 * X(..., ConfigType.BOOL, "false", true, "说明", 10, ControlMeta.checkbox())
 * // 单选框组（从候选项里选一个）
 * X(..., ConfigType.STRING, "0", true, "说明", 10, ControlMeta.radio("0:自动,1:强制"))
 * </pre>
 */
@Getter
public final class ControlMeta {

    /** 控件类型 */
    private final ControlType type;
    /** 是否多选（SELECT / CHECKBOX 有意义） */
    private final boolean multiple;
    /** 是否允许自定义值：下拉可以输入候选项之外的值 */
    private final boolean allowCustom;
    /** 候选项，不会为null */
    private final List<Option> options;

    private ControlMeta(ControlType type, boolean multiple, boolean allowCustom, List<Option> options) {
        this.type = type;
        this.multiple = multiple;
        this.allowCustom = allowCustom;
        this.options = options == null ? Collections.emptyList() : Collections.unmodifiableList(options);
    }

    // ==================== 默认控件 ====================

    /**
     * 值类型的默认控件：BOOL → 开关；LIST → 多选下拉 + 可自定义值（候选为空，相当于标签输入框）；其余 → 输入框
     */
    public static ControlMeta of(ConfigType valueType) {
        ControlType type = ControlType.defaultOf(valueType);
        if (type == ControlType.SELECT) {
            return selectCustom(true, null);
        }
        return new ControlMeta(type, false, false, null);
    }

    // ==================== 静态工厂 ====================

    /** 文本输入框 */
    public static ControlMeta input() {
        return new ControlMeta(ControlType.INPUT, false, false, null);
    }

    /** 开关 */
    public static ControlMeta switcher() {
        return new ControlMeta(ControlType.SWITCH, false, false, null);
    }

    /** 下拉单选 */
    public static ControlMeta select(String options) {
        return select(false, false, options);
    }

    /** 下拉多选 */
    public static ControlMeta selectMultiple(String options) {
        return select(true, false, options);
    }

    /**
     * 下拉 + 可自定义值（候选项只是常用值，用户可以输入别的）
     *
     * @param multiple 是否多选
     */
    public static ControlMeta selectCustom(boolean multiple, String options) {
        return select(multiple, true, options);
    }

    private static ControlMeta select(boolean multiple, boolean allowCustom, String options) {
        return new ControlMeta(ControlType.SELECT, multiple, allowCustom, parse(options));
    }

    /** 复选组（多值：勾选的项用逗号拼成一个值） */
    public static ControlMeta checkboxGroup(String options) {
        return new ControlMeta(ControlType.CHECKBOX, true, false, parse(options));
    }

    /** 单个复选框（布尔值 true/false） */
    public static ControlMeta checkbox() {
        return new ControlMeta(ControlType.CHECKBOX, false, false, null);
    }

    /** 单选框组 */
    public static ControlMeta radio(String options) {
        return new ControlMeta(ControlType.RADIO, false, false, parse(options));
    }

    // ==================== 候选项 ====================

    /**
     * 解析候选项：{@code "值:显示名,值:显示名"}，冒号可省略（显示名=值）
     *
     * @return 候选项列表，{@code raw} 为空时返回空列表
     */
    private static List<Option> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        List<Option> options = new ArrayList<>();
        for (String item : raw.split(",")) {
            String text = item.trim();
            if (text.isEmpty()) {
                continue;
            }
            int sep = text.indexOf(':');
            if (sep < 0) {
                options.add(new Option(text, text));
            } else {
                String value = text.substring(0, sep).trim();
                String label = text.substring(sep + 1).trim();
                options.add(new Option(value, label.isEmpty() ? value : label));
            }
        }
        return options;
    }

    /**
     * 候选项
     */
    @Getter
    public static final class Option {
        /** 保存到配置文件里的值 */
        private final String value;
        /** 前端展示名 */
        private final String label;

        public Option(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
}
