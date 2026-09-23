package com.haruhi.botserver.administration.model;

import lombok.Data;

@Data
public class ExportDatabaseReq {
    private String sql;
    private String tableName;
    private SqlExecuteResult data;
}
