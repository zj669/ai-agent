package com.zj.aiagent.interfaces.common.interceptor;

import com.zj.aiagent.config.AuthDebugProperties;
import com.zj.aiagent.domain.auth.service.ITokenService;
import com.zj.aiagent.shared.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 * 
 * 职责：
 * 1. 验证用户认证（Debug 模式或 JWT）
 * 2. 设置 UserContext（ThreadLocal）
 * 3. 请求完成后清理上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final AuthStrategyFactory authStrategyFactory;
    private final AuthDebugProperties authDebugProperties;
    private final ITokenService tokenService;

    private static final String HEADER_AUTHORIZATION = "Authorization";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 1. Check Debug Mode
        if (authDebugProperties.isEnabled()) {
            String debugUserId = request.getHeader(authDebugProperties.getHeaderName());
            if (debugUserId != null) {
                boolean debugAuth = authStrategyFactory.getStrategy("DEBUG")
                        .map(s -> s.authenticate(debugUserId))
                        .orElse(false);
                if (debugAuth) {
                    // 设置用户上下文
                    Long userId = Long.parseLong(debugUserId);
                    UserContext.setUserId(userId);
                    log.info("Debug Authentication successful for UserID: {}", userId);
                    return true;
                }
            }
        }

        // 2. Check JWT
        String token = request.getHeader(HEADER_AUTHORIZATION);
        if (token != null && token.startsWith("Bearer ")) {
            String jwtToken = token.substring(7);
            boolean jwtAuth = authStrategyFactory.getStrategy("JWT")
                    .map(s -> s.authenticate(jwtToken))
                    .orElse(false);
            if (jwtAuth) {
                // 从 Token 解析用户 ID 并设置上下文
                Long userId = tokenService.getUserIdFromToken(jwtToken);
                if (userId != null) {
                    UserContext.setUserId(userId);
                    log.debug("JWT Authentication successful for UserID: {}", userId);
                    return true;
                }
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex) throws Exception {
        // 清理用户上下文，防止 ThreadLocal 内存泄漏
        UserContext.clear();
    }
}
