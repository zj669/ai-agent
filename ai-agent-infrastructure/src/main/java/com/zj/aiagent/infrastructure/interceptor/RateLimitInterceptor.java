package com.zj.aiagent.infrastructure.interceptor;

import com.google.common.util.concurrent.RateLimiter;
import com.zj.aiagent.domain.workflow.entity.InterceptResult;
import com.zj.aiagent.domain.workflow.entity.NodeExecutionContext;
import com.zj.aiagent.domain.workflow.entity.config.RateLimitConfig;
import com.zj.aiagent.domain.workflow.interfaces.NodeExecutionInterceptor;
import com.zj.aiagent.shared.design.workflow.StateUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流拦截器
 * <p>
 * 基于令牌桶算法，限制节点的执行频率
 * </p>
 */
@Slf4j
@Component
public class RateLimitInterceptor implements NodeExecutionInterceptor {

    /**
     * 节点级别的限流器缓存
     */
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @Override
    public InterceptResult beforeExecution(NodeExecutionContext context) {
        RateLimitConfig config = context.getConfig("RATE_LIMIT", RateLimitConfig.class);

        if (config != null && Boolean.TRUE.equals(config.getEnabled())) {
            // 计算每秒速率
            double permitsPerSecond = config.getMaxRequestsPerMinute() / 60.0;

            RateLimiter limiter = rateLimiters.computeIfAbsent(
                    context.getNodeId(),
                    k -> RateLimiter.create(permitsPerSecond));

            if (!limiter.tryAcquire()) {
                log.warn("节点 {} 达到限流阈值，每分钟最多 {} 次请求",
                        context.getNodeId(), config.getMaxRequestsPerMinute());
                return InterceptResult.pause("达到限流阈值，等待执行");
            }

            log.debug("节点 {} 通过限流检查", context.getNodeId());
        }

        return InterceptResult.proceed();
    }

    @Override
    public InterceptResult afterExecution(NodeExecutionContext context, StateUpdate update) {
        return InterceptResult.proceed();
    }

    @Override
    public int getOrder() {
        return 50; // 限流优先级最高
    }
}
