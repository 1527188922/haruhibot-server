package com.haruhi.botserver.shared.model;

import lombok.Data;

@Data
public class CodeNameReq extends PageReq {

    private String codeOrName;
    private Boolean eqCode = false;
    private Boolean eqName = false;

    private Integer limit = 10;

    private Long groupId;
    private Boolean needPage = true;

    private String prop;
    private String order;

}
