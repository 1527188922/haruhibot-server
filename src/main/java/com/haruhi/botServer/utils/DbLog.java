package com.haruhi.botServer.utils;

import com.haruhi.botServer.constant.BusinessModuleEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class DbLog {
    public static final String DB_LOG_MARKER_NAME = "DB_LOG";
    public static final String MDC_KEY_BUSINESS_MODULE = "businessModule";
    public static final String MDC_KEY_LOG_CLASS_NAME = "logClassName";
    public static final String MDC_KEY_LOG_METHOD_NAME = "logMethodName";

    private static final Marker DB_LOG_MARKER = MarkerFactory.getMarker(DB_LOG_MARKER_NAME);
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private DbLog() {
    }

    public static void debug(String businessModule, String message, Object... args) {
        StackWalker.StackFrame callerFrame = getCallerFrame();
        withMdc(businessModule, callerFrame, () -> getLogger(callerFrame).debug(DB_LOG_MARKER, message, args));
    }

    public static void debug(BusinessModuleEnum businessModule, String message, Object... args) {
        debug(getBusinessModuleName(businessModule), message, args);
    }

    private static void info(String businessModule, String message, Object... args) {
        StackWalker.StackFrame callerFrame = getCallerFrame();
        withMdc(businessModule, callerFrame, () -> getLogger(callerFrame).info(DB_LOG_MARKER, message, args));
    }

    public static void info(BusinessModuleEnum businessModule, String message, Object... args) {
        info(getBusinessModuleName(businessModule), message, args);
    }

    public static void warn(String businessModule, String message, Object... args) {
        StackWalker.StackFrame callerFrame = getCallerFrame();
        withMdc(businessModule, callerFrame, () -> getLogger(callerFrame).warn(DB_LOG_MARKER, message, args));
    }

    public static void warn(BusinessModuleEnum businessModule, String message, Object... args) {
        warn(getBusinessModuleName(businessModule), message, args);
    }

    private static void error(String businessModule, String message, Object... args) {
        StackWalker.StackFrame callerFrame = getCallerFrame();
        withMdc(businessModule, callerFrame, () -> getLogger(callerFrame).error(DB_LOG_MARKER, message, args));
    }

    public static void error(BusinessModuleEnum businessModule, String message, Object... args) {
        error(getBusinessModuleName(businessModule), message, args);
    }

    private static void withMdc(String businessModule, StackWalker.StackFrame callerFrame, Runnable runnable) {
        String oldBusinessModule = MDC.get(MDC_KEY_BUSINESS_MODULE);
        String oldLogClassName = MDC.get(MDC_KEY_LOG_CLASS_NAME);
        String oldLogMethodName = MDC.get(MDC_KEY_LOG_METHOD_NAME);
        if (StringUtils.isNotBlank(businessModule)) {
            MDC.put(MDC_KEY_BUSINESS_MODULE, businessModule);
        }
        MDC.put(MDC_KEY_LOG_CLASS_NAME, callerFrame.getClassName());
        MDC.put(MDC_KEY_LOG_METHOD_NAME, callerFrame.getMethodName());
        try {
            runnable.run();
        } finally {
            restore(MDC_KEY_BUSINESS_MODULE, oldBusinessModule);
            restore(MDC_KEY_LOG_CLASS_NAME, oldLogClassName);
            restore(MDC_KEY_LOG_METHOD_NAME, oldLogMethodName);
        }
    }

    private static StackWalker.StackFrame getCallerFrame() {
        return STACK_WALKER.walk(frames -> frames
                .filter(frame -> !DbLog.class.equals(frame.getDeclaringClass()))
                .findFirst()
                .orElseThrow());
    }

    private static Logger getLogger(StackWalker.StackFrame callerFrame) {
        return LoggerFactory.getLogger(callerFrame.getDeclaringClass());
    }

    private static String getBusinessModuleName(BusinessModuleEnum businessModule) {
        return businessModule == null ? null : businessModule.getName();
    }

    private static void restore(String key, String oldValue) {
        if (oldValue == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, oldValue);
        }
    }
}
