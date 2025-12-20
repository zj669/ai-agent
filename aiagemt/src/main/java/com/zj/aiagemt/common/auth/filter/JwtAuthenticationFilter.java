package com.zj.aiagemt.common.auth.filter;

import com.zj.aiagemt.common.auth.exception.AuthenticationException;
import com.zj.aiagemt.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT认证过滤器
 * 
 * <p>
 * 从HTTP请求头的Authorization字段中提取Bearer Token，
 * 验证Token的有效性并解析出用户ID。
 * 
 * <p>
 * 请求头格式：{@code Authorization: Bearer <token>}
 * 
 * @author zj
 * @since 2025-12-20
 */
@Component
@Slf4j
public class JwtAuthenticationFilter implements AuthenticationFilter {

    @Resource
    private JwtUtil jwtUtil;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Long authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        // 未携带JWT Token，不处理
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("[JWT认证] 请求未携带Authorization Header或格式不正确, URI: {}",
                    request.getRequestURI());
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            Long userId = jwtUtil.parseToken(token);
            log.debug("[JWT认证] 认证成功, userId: {}, URI: {}",
                    userId, request.getRequestURI());
            return userId;
        } catch (JwtException e) {
            log.warn("[JWT认证] Token验证失败: {}, URI: {}",
                    e.getMessage(), request.getRequestURI());
            throw new AuthenticationException("Token无效或已过期");
        } catch (Exception e) {
            log.error("[JWT认证] Token解析异常: {}, URI: {}",
                    e.getMessage(), request.getRequestURI(), e);
            throw new AuthenticationException("Token解析失败");
        }
    }

    @Override
    public boolean isEnabled() {
        return true; // JWT认证始终启用
    }

    @Override
    public int getOrder() {
        return 100; // JWT认证优先级较低
    }

    @Override
    public String getName() {
        return "JWT";
    }
}
