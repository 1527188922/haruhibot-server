package com.haruhi.botServer.config.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配置项值类型
 * <p>
 * 决定前端渲染的控件类型、后端保存时的校验规则，以及读取时的反序列化方式
 */
@Getter
@AllArgsConstructor
public enum ConfigType {

    /** 单行文本 */
    STRING(1),
    /** 整数 */
    INT(2),
    /** 布尔值 true/false */
    BOOL(3),
    /** 列表，按 regex 分割（默认逗号/中文逗号/换行） */
    LIST(4),
    /** JSON 文本 */
    JSON(5),
    /** 敏感信息，前端脱敏展示 */
    SECRET(6),
    ;

    private final int code;

    public boolean isNumeric() {
        return this == INT;
    }

    /**
     * 校验待写入的值是否符合当前类型，合法返回 null，否则返回错误说明
     */
    public String validate(String value) {
        if (value == null) {
            return null;
        }
        switch (this) {
            case INT:
                try {
                    Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    return "必须是整数";
                }
                return null;
            case BOOL:
                String v = value.trim().toLowerCase();
                if (!"true".equals(v) && !"false".equals(v)) {
                    return "只能是 true 或 false";
                }
                return null;
            case JSON:
                try {
                    com.alibaba.fastjson.JSON.parse(value);
                } catch (Exception e) {
                    return "不是合法的JSON";
                }
                return null;
            default:
                return null;
        }
    }
}
