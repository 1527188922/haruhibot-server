package com.haruhi.botserver.configuration.metadata;

/**
 * 配置项<b>控件类型</b>（前端渲染成什么）
 * <p>
 * 与 {@link ConfigType}（值类型）是两件事：
 * <ul>
 *     <li>{@link ConfigType} 决定值的校验、yml 标量编码、读取时的反序列化；</li>
 *     <li>{@code ControlType} 只决定前端用哪个控件渲染。</li>
 * </ul>
 * 同一个值类型可以用不同控件，例如 {@code STRING} 既可以是 {@link #INPUT}，也可以是
 * {@link #SELECT} / {@link #RADIO}；反向也一样，{@link #SELECT} 既能承载 {@code STRING}
 * 也能承载逗号分隔的 {@code LIST}。具体每个配置项用哪个控件见 {@link ControlMeta}。
 */
public enum ControlType {

    /** 文本输入框（值类型 INT/SECRET/JSON/LIST 会自动细分数字框、密码框、多行文本） */
    INPUT,
    /** 开关：两个值（一般是 BOOL 的 true/false） */
    SWITCH,
    /** 下拉选择：单选 / 多选 / 可自定义值（见 {@link ControlMeta#isMultiple()}、{@link ControlMeta#isAllowCustom()}） */
    SELECT,
    /** 复选框：单个（布尔）或复选组（多个值） */
    CHECKBOX,
    /** 单选框组：从候选项里选一个 */
    RADIO,
    ;

    /**
     * 值类型对应的默认控件（没有显式声明控件时使用）
     * <p>
     * BOOL → 开关；LIST → 多选下拉（允许自定义值，等价于"标签输入框"）；其余 → 输入框
     */
    public static ControlType defaultOf(ConfigType valueType) {
        if (valueType == ConfigType.BOOL) {
            return SWITCH;
        }
        if (valueType == ConfigType.LIST) {
            return SELECT;
        }
        return INPUT;
    }
}
