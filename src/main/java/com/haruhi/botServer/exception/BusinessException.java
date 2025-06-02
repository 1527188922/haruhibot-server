package com.haruhi.botServer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class BusinessException extends RuntimeException{

    @Getter
    private Integer errorCode;
    @Getter
    private String errorMsg;

    public BusinessException(String errorMsg) {
        this.errorCode = 500;
        this.errorMsg = errorMsg;
    }
}