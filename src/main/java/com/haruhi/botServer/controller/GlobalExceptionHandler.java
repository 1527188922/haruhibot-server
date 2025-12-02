package com.haruhi.botServer.controller;

import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.vo.HttpResp;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public HttpResp handleAllUncaughtException(Exception e) {
        return HttpResp.fail(HttpResp.SERVER_ERROR, e.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public HttpResp handleBusinessException(BusinessException e) {
        return HttpResp.fail(e.getErrorCode(), e.getErrorMsg(), null);
    }

}