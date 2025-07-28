package com.haruhi.botServer.entity;

import lombok.Data;

@Data
public class TableInfoSqlite {
    private Integer cid;
    private String name;// 字段名称
    private String type;
    private Integer notnull;//是否非空 1是 0否
    private String dfltValue;
    private Integer pk;//是否主键 1是 0否

}
