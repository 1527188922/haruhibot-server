package com.haruhi.botserver.configuration.model;

import com.haruhi.botserver.configuration.metadata.ConfigType;
import com.haruhi.botserver.configuration.metadata.ControlMeta;
import lombok.Data;

/**
 * 前端「配置管理」页的单个配置项
 */
@Data
public class ConfigItem {

    /** 配置key，同时作为增删改的唯一标识 */
    private String key;
    /** 枚举名，reset/reset 时使用 */
    private String name;
    /** 中文名 */
    private String displayName;
    /** 值类型：决定校验与读写，不决定控件 */
    private ConfigType type;
    /** 前端控件元数据：控件类型 + 候选项 + 是否多选/可自定义值 */
    private ControlMeta control;
    /** 当前值。SECRET类型不下发明文，为null */
    private String value;
    /** 是否已有值（SECRET类型无法下发明文，前端据此判断是否显示"已设置"） */
    private boolean hasValue;
    /** SECRET类型的掩码值，如 ****abcd */
    private String maskedValue;
    /** 声明中的默认值，SECRET类型为掩码 */
    private String defaultValue;
    /** 是否可热更新 */
    private boolean hot;
    /** 是否已配置（false表示正在使用默认值） */
    private boolean configured;
    /** 值来源：FILE / DEFAULT */
    private String source;
    /** 说明 */
    private String remark;
    /** 所属文件 */
    private String fileName;
    /** 排序 */
    private int sort;
}
