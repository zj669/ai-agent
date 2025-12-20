package com.zj.aiagemt.common.interceptor;

import com.zj.aiagemt.common.auth.AuthenticationFilterChain;
import com.zj.aiagemt.common.auth.exception.AuthenticationException;
import com.zj.aiagemt.utils.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证拦截器
 * 
 * <p>
 * 使用认证过滤链模式进行身份认证，支持多种认证方式的组合使用。
 * 
 * @author zj
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private AuthenticationFilterChain authenticationFilterChain;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestURI = request.getRequestURI();
        log.debug("LoginInterceptor 拦截请求: {}", requestURI);

        try {
            // 执行认证过滤链
            Long userId = authenticationFilterChain.doFilter(request);

            // 将用户ID设置到ThreadLocal
            UserContext.setUserId(userId);
            log.debug("用户认证成功, userId: {}, URI: {}", userId, requestURI);

            return true;
        } catch (AuthenticationException e) {
            log.warn("认证失败: {}, URI: {}", e.getMessage(), requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(String.format(
                    "{\"code\":\"401\",\"info\":\"%s\",\"data\":null}", e.getMessage()));
            return false;
        } catch (Exception e) {
            log.error("认证过程异常: {}, URI: {}", e.getMessage(), requestURI, e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"info\":\"认证失败\",\"data\":null}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 请求结束后清理ThreadLocal
        UserContext.clear();
    }
}
