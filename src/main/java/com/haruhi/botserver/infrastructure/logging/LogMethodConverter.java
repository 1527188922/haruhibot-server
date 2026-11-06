package com.haruhi.botserver.infrastructure.logging;

/**
 * 日志调用方方法名，替换 logback 的 %M。
 *
 * <p>默认取 DbLog 写入 MDC 的真实调用方方法名，非 DbLog 日志回退到调用栈。</p>
 */
public class LogMethodConverter extends CallerInfoConverter {

    public LogMethodConverter() {
        super(DbLog.MDC_KEY_LOG_METHOD_NAME);
    }

    @Override
    protected String pick(StackTraceElement callerFrame) {
        return callerFrame.getMethodName();
    }

    @Override
    protected String render(String rawValue) {
        return rawValue;
    }
}
