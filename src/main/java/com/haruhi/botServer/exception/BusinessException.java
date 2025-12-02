package com.haruhi.botServer.exception;

import com.haruhi.botServer.vo.HttpResp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class BusinessException extends RuntimeException{

    @Getter
    private Integer errorCode;
    @Getter
    private String errorMsg;

    public BusinessException(String errorMsg) {
        this.errorCode = HttpResp.BUSI_ERROR;
        this.errorMsg = errorMsg;
    }
}