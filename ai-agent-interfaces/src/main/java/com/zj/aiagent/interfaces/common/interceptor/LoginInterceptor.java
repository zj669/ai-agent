package com.zj.aiagent.interfaces.common.interceptor;

import com.zj.aiagent.config.AuthDebugProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final AuthStrategyFactory authStrategyFactory;
    private final AuthDebugProperties authDebugProperties;

    private static final String HEADER_Authorization = "Authorization";

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
                    // TODO: Set user context (e.g. ThreadLocal)
                    log.info("Debug Authentication successful for UserID: {}", debugUserId);
                    return true;
                }
            }
        }

        // 2. Check JWT
        String token = request.getHeader(HEADER_Authorization);
        if (token != null && token.startsWith("Bearer ")) {
            String finalToken = token.substring(7);
            boolean jwtAuth = authStrategyFactory.getStrategy("JWT")
                    .map(s -> s.authenticate(finalToken))
                    .orElse(false);
            if (jwtAuth) {
                // TODO: Parse token and set user context
                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }
}
