package com.zj.aiagent.auth;

import com.zj.aiagent.auth.exception.AuthenticationException;
import com.zj.aiagent.shared.utils.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

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
        // 请求结束后清理ThreadLocal，避免内存泄漏
        UserContext.clear();
    }
}
