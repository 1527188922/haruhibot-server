package com.haruhi.botServer.dto.qqclient;

import lombok.Data;

import java.io.Serializable;

@Data
public class RequestBox<T> implements Serializable {
    private String action;
    private T params;
    private String echo;
}
