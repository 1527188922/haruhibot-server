package com.haruhi.botServer.controller;

import com.haruhi.botServer.exception.BusinessException;
import com.haruhi.botServer.vo.HttpResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public HttpResp handleAllUncaughtException(Exception e) {
        log.error("全局异常："+e.getMessage(), e);
        return HttpResp.fail(HttpResp.SERVER_ERROR, e.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public HttpResp handleBusinessException(BusinessException e) {
        log.error("全局业务异常："+e.getMessage(), e);
        return HttpResp.fail(e.getErrorCode(), e.getErrorMsg(), null);
    }

}