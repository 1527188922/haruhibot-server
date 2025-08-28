package com.haruhi.botServer.vo;

import com.haruhi.botServer.dto.SqlExecuteResult;
import lombok.Data;

@Data
public class ExportDatabaseReq {
    private String sql;
    private SqlExecuteResult data;
}
