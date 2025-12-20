package com.zj.aiagemt.common.auth;

import com.zj.aiagemt.common.auth.exception.AuthenticationException;
import com.zj.aiagemt.common.auth.filter.AuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证过滤链管理器
 * 
 * <p>
 * 管理和执行认证过滤链，按照过滤器的优先级顺序依次执行，
 * 任一过滤器认证成功即停止后续过滤器的执行。
 * 
 * <p>
 * 执行逻辑：
 * <ol>
 * <li>过滤器按优先级（{@link AuthenticationFilter#getOrder()}）排序</li>
 * <li>只有启用的过滤器（{@link AuthenticationFilter#isEnabled()} 返回true）才会执行</li>
 * <li>依次调用每个过滤器的 {@link AuthenticationFilter#authenticate(HttpServletRequest)}
 * 方法</li>
 * <li>任一过滤器返回非null的用户ID，认证成功，停止执行</li>
 * <li>过滤器抛出 {@link AuthenticationException}，认证失败，停止执行</li>
 * <li>所有过滤器都未能成功认证，抛出 {@link AuthenticationException}</li>
 * </ol>
 * 
 * @author zj
 * @since 2025-12-20
 */
@Component
@Slf4j
public class AuthenticationFilterChain {

    private final List<AuthenticationFilter> filters;

    /**
     * 构造认证过滤链
     * 
     * <p>
     * Spring会自动注入所有实现了 {@link AuthenticationFilter} 接口的Bean，
     * 并按照优先级排序，只保留启用的过滤器。
     * 
     * @param filters 所有认证过滤器（Spring自动注入）
     */
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

    /**
     * 执行认证过滤链
     * 
     * @param request HTTP请求
     * @return 认证成功的用户ID
     * @throws AuthenticationException 所有过滤器都未能成功认证时抛出
     */
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
