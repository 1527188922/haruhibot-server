package com.haruhi.botserver.shared.error;

import com.haruhi.botserver.shared.model.HttpResp;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class BusinessException extends RuntimeException{

    @Getter
    private Integer errorCode;
    @Getter
    private String errorMsg;

    public BusinessException(String errorMsg) {
        super(errorMsg);
        this.errorCode = HttpResp.BUSI_ERROR;
        this.errorMsg = errorMsg;
    }
}