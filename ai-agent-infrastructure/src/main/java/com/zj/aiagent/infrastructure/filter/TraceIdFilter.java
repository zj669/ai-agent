package com.zj.aiagent.infrastructure.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Trace-ID过滤器
 * 为每个HTTP请求自动生成或传播trace-id,便于分布式追踪和日志关联
 * 
 * @author zj
 * @since 2026-01-05
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "trace-id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 1. 尝试从请求头获取trace-id(上游服务传入)
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            
            // 2. 如果没有,则生成新的trace-id
            if (traceId == null || traceId.trim().isEmpty()) {
                traceId = generateTraceId();
            }

            // 3. 存储到MDC,供日志使用
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 4. 添加到响应头,方便下游服务使用
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            // 5. 继续处理请求
            chain.doFilter(request, response);
            
        } finally {
            // 6. 请求处理完成后清理MDC,避免内存泄漏
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /**
     * 生成trace-id
     * 格式: 时间戳-UUID前8位
     * 示例: 1735900800123-a1b2c3d4
     */
    private String generateTraceId() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        long timestamp = System.currentTimeMillis();
        return timestamp + "-" + uuid.substring(0, 8);
    }
}
