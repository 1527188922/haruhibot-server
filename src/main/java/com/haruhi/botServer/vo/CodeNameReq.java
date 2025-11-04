package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class CodeNameReq {

    private String codeOrName;
    private Boolean eqCode = false;
    private Boolean eqName = false;

    private Integer limit = 10;

}
