package com.zj.aiagemt.common.interceptor;

import com.zj.aiagemt.utils.JwtUtil;
import com.zj.aiagemt.utils.UserContext;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录认证拦截器
 */
@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String requestURI = request.getRequestURI();
        log.debug("LoginInterceptor 拦截请求: {}", requestURI);

        // 1. 从请求头获取Token
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("请求未携带有效的Authorization Header, URI: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"info\":\"未登录或Token无效\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 2. 验证Token并解析用户ID
            Long userId = jwtUtil.parseToken(token);

            // 3. 将用户ID设置到ThreadLocal
            UserContext.setUserId(userId);
            log.debug("用户认证成功, userId: {}, URI: {}", userId, requestURI);

            return true;
        } catch (JwtException e) {
            log.warn("Token验证失败: {}, URI: {}", e.getMessage(), requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"401\",\"info\":\"Token无效或已过期\",\"data\":null}");
            return false;
        } catch (Exception e) {
            log.error("Token解析异常: {}, URI: {}", e.getMessage(), requestURI, e);
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
