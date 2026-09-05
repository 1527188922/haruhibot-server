package com.haruhi.botServer.vo;

import lombok.Data;

import java.util.List;

@Data
public class SystemLogQueryReq extends PageReq {

    private String traceId;
    private String businessModule;
    private String level;
    private String loggerName;
    private String className;
    private String methodName;
    private String threadName;
    private String message;
    private String throwable;
    private String requestMethod;
    private String requestUri;
    private String clientIp;
    private String userName;
    private String handlerClass;
    private String handlerMethod;

    // yyyy-MM-dd HH:mm:ss
    private List<String> datetimerange;
}
