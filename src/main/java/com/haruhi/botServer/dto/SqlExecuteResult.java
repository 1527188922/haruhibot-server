package com.haruhi.botServer.dto;

import lombok.Data;

@Data
public class SqlExecuteResult {

    private String type;
    private String sql;
    private Object data;
    private Long cost;
    private String errorMessage;
}
