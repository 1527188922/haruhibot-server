package com.haruhi.botServer.vo;

import lombok.Data;

@Data
public class DictQueryReq extends PageReq{
    private String key;
    private String content;
    private String remark;
}
