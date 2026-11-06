package com.haruhi.botserver.infrastructure.logging;

/**
 * 日志调用方类名，替换 logback 的 %C{0}（简单类名）。
 *
 * <p>默认取 DbLog 写入 MDC 的真实调用方类名，非 DbLog 日志回退到调用栈。</p>
 */
public class LogClassConverter extends CallerInfoConverter {

    public LogClassConverter() {
        super(DbLog.MDC_KEY_LOG_CLASS_NAME);
    }

    @Override
    protected String pick(StackTraceElement callerFrame) {
        return callerFrame.getClassName();
    }

    @Override
    protected String render(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot < 0 ? className : className.substring(lastDot + 1);
    }
}
