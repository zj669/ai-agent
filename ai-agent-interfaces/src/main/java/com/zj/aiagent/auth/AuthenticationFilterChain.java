package com.zj.aiagent.auth;

import com.zj.aiagent.auth.base.AuthenticationFilter;
import com.zj.aiagent.auth.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;



@Component
@Slf4j
public class AuthenticationFilterChain {

    private final List<AuthenticationFilter> filters;

    public AuthenticationFilterChain(List<AuthenticationFilter> filters) {
        // 按照优先级排序，只保留启用的过滤器
        this.filters = filters.stream()
                .filter(AuthenticationFilter::isEnabled)
                .sorted(Comparator.comparingInt(AuthenticationFilter::getOrder))
                .collect(Collectors.toList());

        log.info("认证过滤链初始化完成, 启用的过滤器: [{}]",
                this.filters.stream()
                        .map(f -> f.getName() + "(优先级:" + f.getOrder() + ")")
                        .collect(Collectors.joining(", ")));
    }

    public Long doFilter(HttpServletRequest request) {
        log.debug("开始执行认证过滤链, URI: {}", request.getRequestURI());

        for (AuthenticationFilter filter : filters) {
            try {
                Long userId = filter.authenticate(request);
                if (userId != null) {
                    log.debug("认证成功, 使用过滤器: {}, userId: {}",
                            filter.getName(), userId);
                    return userId;
                }
                log.debug("过滤器 [{}] 未处理此请求，继续尝试下一个过滤器", filter.getName());
            } catch (AuthenticationException e) {
                // 认证失败，直接抛出异常
                log.warn("过滤器 [{}] 认证失败: {}", filter.getName(), e.getMessage());
                throw e;
            } catch (Exception e) {
                // 其他异常，记录日志并继续尝试下一个过滤器
                log.error("过滤器 [{}] 执行异常: {}", filter.getName(), e.getMessage(), e);
            }
        }

        // 所有过滤器都未能成功认证
        log.warn("所有认证过滤器都未能成功认证, URI: {}", request.getRequestURI());
        throw new AuthenticationException("未登录或认证信息无效");
    }
}
