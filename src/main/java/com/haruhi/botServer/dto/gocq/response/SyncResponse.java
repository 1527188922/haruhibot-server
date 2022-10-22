package com.haruhi.botServer.dto.gocq.response;

import lombok.Data;

@Data
public class SyncResponse<T> {

    public static transient final String STATUS_OK = "ok";

    private int retcode;
    private String status;
    private String echo;
    private T data;
}
