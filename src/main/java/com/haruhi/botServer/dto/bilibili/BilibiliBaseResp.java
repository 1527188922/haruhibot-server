package com.haruhi.botServer.dto.bilibili;

import lombok.Data;

@Data
public class BilibiliBaseResp<T> {

    public static final int SUCCESS_CODE = 0;

    private Integer code;
    private String message;
    private Long ttl;
    private T data;


    private String raw;
}
