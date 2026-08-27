package com.haruhi.botServer.config.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.haruhi.botServer.entity.SystemLogSqlite;
import com.haruhi.botServer.mapper.SystemLogSqliteMapper;
import com.haruhi.botServer.utils.ApplicationContextProvider;
import com.haruhi.botServer.utils.DateTimeUtil;
import com.haruhi.botServer.utils.DbLog;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Marker;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class DatabaseLogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final String MDC_TRACE_ID = "traceId";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    private static final String MDC_REQUEST_URI = "requestUri";
    private static final String MDC_QUERY_STRING = "queryString";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_USER_NAME = "userName";
    private static final String MDC_HANDLER_CLASS = "handlerClass";
    private static final String MDC_HANDLER_METHOD = "handlerMethod";

    private BlockingQueue<SystemLogSqlite> queue;
    private volatile boolean running;
    private Thread worker;
    private int queueSize = 5000;
    private int maxMessageLength = 4000;
    private int maxThrowableLength = 8000;
    private boolean discardInfoWhenQueueFull = true;

    @Override
    public void start() {
        queue = new ArrayBlockingQueue<>(queueSize);
        running = true;
        worker = new Thread(this::consume, "db-log-appender");
        worker.setDaemon(true);
        worker.start();
        super.start();
    }

    @Override
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted() || !hasDbLogMarker(event)) {
            return;
        }
        SystemLogSqlite systemLog = convert(event);
        if (!queue.offer(systemLog) && shouldKeepWhenQueueFull(event)) {
            try {
                queue.offer(systemLog, 100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void consume() {
        while (running || !queue.isEmpty()) {
            try {
                SystemLogSqlite log = queue.poll(1, TimeUnit.SECONDS);
                if (log == null) {
                    continue;
                }
                SystemLogSqliteMapper mapper = ApplicationContextProvider.getBean(SystemLogSqliteMapper.class);
                mapper.insert(log);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                addError("Insert database log failed", e);
            }
        }
    }

    private SystemLogSqlite convert(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        StackTraceElement caller = firstCaller(event);

        SystemLogSqlite systemLog = new SystemLogSqlite();
        systemLog.setTraceId(mdc.get(MDC_TRACE_ID));
        systemLog.setBusinessModule(mdc.get(DbLog.MDC_KEY_BUSINESS_MODULE));
        systemLog.setLevel(event.getLevel().toString());
        systemLog.setLoggerName(event.getLoggerName());
        systemLog.setClassName(StringUtils.defaultIfBlank(mdc.get(DbLog.MDC_KEY_LOG_CLASS_NAME),
                caller == null ? null : caller.getClassName()));
        systemLog.setMethodName(StringUtils.defaultIfBlank(mdc.get(DbLog.MDC_KEY_LOG_METHOD_NAME),
                caller == null ? null : caller.getMethodName()));
        systemLog.setThreadName(event.getThreadName());
        systemLog.setMessage(limit(event.getFormattedMessage(), maxMessageLength));
        systemLog.setThrowable(limit(throwableToString(event.getThrowableProxy()), maxThrowableLength));
        systemLog.setRequestMethod(mdc.get(MDC_REQUEST_METHOD));
        systemLog.setRequestUri(mdc.get(MDC_REQUEST_URI));
        systemLog.setQueryString(mdc.get(MDC_QUERY_STRING));
        systemLog.setClientIp(mdc.get(MDC_CLIENT_IP));
        systemLog.setUserName(mdc.get(MDC_USER_NAME));
        systemLog.setHandlerClass(mdc.get(MDC_HANDLER_CLASS));
        systemLog.setHandlerMethod(mdc.get(MDC_HANDLER_METHOD));
        systemLog.setCreateTime(DateTimeUtil.dateTimeFormat(new Date(event.getTimeStamp()), DateTimeUtil.PatternEnum.yyyyMMddHHmmssSSS));
        return systemLog;
    }

    private boolean hasDbLogMarker(ILoggingEvent event) {
        List<Marker> markers = event.getMarkerList();
        if (markers == null || markers.isEmpty()) {
            return false;
        }
        return markers.stream().anyMatch(marker -> marker.contains(DbLog.DB_LOG_MARKER_NAME));
    }

    private boolean shouldKeepWhenQueueFull(ILoggingEvent event) {
        return !discardInfoWhenQueueFull || event.getLevel().isGreaterOrEqual(Level.WARN);
    }

    private StackTraceElement firstCaller(ILoggingEvent event) {
        StackTraceElement[] callerData = event.getCallerData();
        if (callerData == null || callerData.length == 0) {
            return null;
        }
        return callerData[0];
    }

    private String throwableToString(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        ThrowableProxyUtil.subjoinFirstLine(builder, throwableProxy);
        builder.append(System.lineSeparator());
        for (StackTraceElementProxy proxy : throwableProxy.getStackTraceElementProxyArray()) {
            builder.append(proxy).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String limit(String text, int maxLength) {
        if (StringUtils.isBlank(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public void setMaxThrowableLength(int maxThrowableLength) {
        this.maxThrowableLength = maxThrowableLength;
    }

    public void setDiscardInfoWhenQueueFull(boolean discardInfoWhenQueueFull) {
        this.discardInfoWhenQueueFull = discardInfoWhenQueueFull;
    }
}
