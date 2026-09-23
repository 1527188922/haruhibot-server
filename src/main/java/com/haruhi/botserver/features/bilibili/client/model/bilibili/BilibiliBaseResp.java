package com.haruhi.botserver.features.bilibili.client.model.bilibili;

import lombok.Data;

@Data
public class BilibiliBaseResp<T> {

    public static final int SUCCESS_CODE = 0;

    private Integer code;
    private String message;
    private Long ttl;
    private T data;


    private String raw;

    public boolean isSuccess() {
        return code != null && SUCCESS_CODE == code;
    }
}
