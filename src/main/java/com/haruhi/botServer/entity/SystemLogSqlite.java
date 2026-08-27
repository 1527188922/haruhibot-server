package com.haruhi.botServer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.haruhi.botServer.constant.DataBaseConst;
import com.haruhi.botServer.utils.DateTimeUtil;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = DataBaseConst.T_SYSTEM_LOG)
public class SystemLogSqlite {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
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
    private String queryString;
    private String clientIp;
    private String userName;
    private String handlerClass;
    private String handlerMethod;
    private String createTime;

    public Date createTimeParsed() {
        return DateTimeUtil.parseDate(createTime, DateTimeUtil.PatternEnum.yyyyMMddHHmmssSSS);
    }
}
