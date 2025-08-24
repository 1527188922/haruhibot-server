package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class DatabaseInfoNode {

    public static final String TYPE_INDEX = "index";
    public static final String TYPE_COLUMN = "column";
    public static final String TYPE_TABLE = "table";
    public static final String TYPE_FIXED = "fixed";
    public static final String TYPE_FIXED_COLUMN = "列";
    public static final String TYPE_FIXED_INDEX = "索引";

    private String name;// 表名称、字段名称、索引名称
    private String type;// 业务类型
    private String columnType;// 字段类型
    private Integer notnull;// 字段是否非空 1非空
    private String defaultValue;// 字段默认值
    private Integer pk;// 字段是否主键 1是


    private String tableName;// 所属表名称
    private String sql;// DDL
    private Integer unique;// 是否唯一索引

    private Boolean leaf;
}
