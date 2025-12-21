package com.zj.aiagent.auth.filter;


import com.zj.aiagent.auth.base.AuthenticationFilter;
import com.zj.aiagent.auth.exception.AuthenticationException;
import com.zj.aiagent.config.AuthDebugProperties;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.domain.user.repository.UserRepository;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class DebugAuthenticationFilter implements AuthenticationFilter {

    @Resource
    private AuthDebugProperties debugProperties;

    @Resource
    private UserRepository userRepository;

    @Override
    public Long authenticate(HttpServletRequest request) {
        String userIdStr = request.getHeader(debugProperties.getHeaderName());

        // 未携带debug-user header，不处理
        if (StringUtils.isBlank(userIdStr)) {
            return null;
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty() || user.get().isActive()) {
                throw new AuthenticationException("Debug用户不存在");
            }
            log.warn("🔧 [DEBUG认证] 认证成功, userId: {}, URI: {}",
                    userId, request.getRequestURI());
            return userId;
        } catch (NumberFormatException e) {
            log.warn("[DEBUG认证] 用户ID格式错误: {}, URI: {}",
                    userIdStr, request.getRequestURI());
            throw new AuthenticationException("Debug用户ID格式错误");
        }
    }

    @Override
    public boolean isEnabled() {
        return debugProperties.isEnabled();
    }

    @Override
    public int getOrder() {
        return 10; // Debug认证优先级最高
    }

    @Override
    public String getName() {
        return "DEBUG";
    }
}
