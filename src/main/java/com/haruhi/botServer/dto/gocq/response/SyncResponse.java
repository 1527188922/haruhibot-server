package com.haruhi.botServer.dto.gocq.response;

import lombok.Data;

@Data
public class SyncResponse<T> {

    public static transient final String STATUS_OK = "ok";

    private Integer retcode;
    private String status;
    private String echo;
    private String wording;
    private String msg;
    private T data;
}
