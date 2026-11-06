package com.haruhi.botserver.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 校验日志中的调用方类名/方法名不再是 DbLog.lambda$xxx$n
 */
class CallerInfoConverterTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesCallerInfoFromMdc() {
        MDC.put(DbLog.MDC_KEY_LOG_CLASS_NAME, "com.haruhi.botserver.features.bilibili.service.BilibiliService");
        MDC.put(DbLog.MDC_KEY_LOG_METHOD_NAME, "sendGetRequest");

        ILoggingEvent event = newEvent();

        assertEquals("BilibiliService", new LogClassConverter().convert(event));
        assertEquals("sendGetRequest", new LogMethodConverter().convert(event));
    }

    @Test
    void fallsBackToCallStackWithoutMdc() {
        ILoggingEvent event = newEvent();

        assertEquals("CallerInfoConverterTest", new LogClassConverter().convert(event));
        assertEquals("fallsBackToCallStackWithoutMdc", new LogMethodConverter().convert(event));
    }

    private ILoggingEvent newEvent() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(CallerInfoConverterTest.class.getName());
        return new LoggingEvent(CallerInfoConverterTest.class.getName(), logger, Level.INFO, "message", null, null);
    }
}
