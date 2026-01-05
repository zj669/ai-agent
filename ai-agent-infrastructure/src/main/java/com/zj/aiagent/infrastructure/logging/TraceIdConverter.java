package com.zj.aiagent.infrastructure.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.MDC;

/**
 * 自定义Trace-ID转换器
 * 只在trace-id存在时才输出,避免系统内部日志显示空字符串
 * 
 * @author zj
 * @since 2026-01-05
 */
public class TraceIdConverter extends ClassicConverter {

    private static final String TRACE_ID_MDC_KEY = "trace-id";

    @Override
    public String convert(ILoggingEvent event) {
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.trim().isEmpty()) {
            return " [" + traceId + "]";
        }
        return "";
    }
}
