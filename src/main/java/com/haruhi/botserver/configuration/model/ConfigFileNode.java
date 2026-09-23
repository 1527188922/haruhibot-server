package com.haruhi.botserver.configuration.model;

import lombok.Data;

import java.util.List;

/**
 * 前端「配置管理」页的一个配置大类（对应 ./config 下的一个 properties 文件）
 */
@Data
public class ConfigFileNode {

    /** 文件名 */
    private String fileName;
    /** 中文名 */
    private String displayName;
    /** 文件说明 */
    private String remark;
    /** 文件绝对路径 */
    private String path;
    /** 文件是否存在 */
    private boolean exists;
    /** 最后修改时间 */
    private long lastModified;
    /** 配置项数量 */
    private int count;
    /** 可热更新的配置项数量 */
    private int hotCount;
    /** 最近一次刷新/校验的错误信息，正常为null */
    private String error;
    /** 配置项 */
    private List<ConfigItem> items;
}
