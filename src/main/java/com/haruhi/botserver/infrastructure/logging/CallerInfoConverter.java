package com.haruhi.botserver.infrastructure.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 日志调用方信息转换器基类。
 *
 * <p>DbLog 内部是在 lambda 中调用 slf4j 的（{@code () -> logger.error(...)}），
 * logback 的 %C/%M 依据真实调用栈计算，得到的类是 DbLog 自己，
 * 日志里就会打印成 {@code DbLog.lambda$error$3} 这种，看不到真正的业务调用方。
 * 由于 DbLog 已经把真实调用方类名、方法名写入 MDC，这里优先读 MDC。</p>
 *
 * <p>非 DbLog 输出的日志（如框架日志）MDC 中没有这两个值，
 * 此时自行回溯调用栈，跳过 logback/slf4j 以及日志封装自身的栈帧，
 * 取第一个业务栈帧，保证与原来的 %C/%M 行为一致。</p>
 */
abstract class CallerInfoConverter extends ClassicConverter {

    private static final String[] SKIPPED_CLASS_PREFIXES = {
            "ch.qos.logback.",
            "org.slf4j.",
            "org.apache.log4j.",
            "org.apache.commons.logging."
    };

    private final String mdcKey;

    CallerInfoConverter(String mdcKey) {
        this.mdcKey = mdcKey;
    }

    @Override
    public String convert(ILoggingEvent event) {
        String fromMdc = readMdc(event);
        if (StringUtils.isNotBlank(fromMdc)) {
            return render(fromMdc);
        }
        StackTraceElement callerFrame = findCallerFrame();
        return callerFrame == null ? "" : render(pick(callerFrame));
    }

    /**
     * 从调用栈帧中取出转换器需要的原始值（类名或方法名）
     */
    protected abstract String pick(StackTraceElement callerFrame);

    /**
     * 把原始值渲染成最终输出的字符串
     */
    protected abstract String render(String rawValue);

    private String readMdc(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc == null) {
            return null;
        }
        return mdc.get(mdcKey);
    }

    private StackTraceElement findCallerFrame() {
        for (StackTraceElement frame : new Throwable().getStackTrace()) {
            if (!isSkipped(frame.getClassName())) {
                return frame;
            }
        }
        return null;
    }

    private boolean isSkipped(String className) {
        // DbLog 与转换器自身（基类、当前子类）的栈帧都不是业务调用方
        if (className.equals(DbLog.class.getName())
                || className.equals(CallerInfoConverter.class.getName())
                || className.equals(getClass().getName())) {
            return true;
        }
        for (String prefix : SKIPPED_CLASS_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
