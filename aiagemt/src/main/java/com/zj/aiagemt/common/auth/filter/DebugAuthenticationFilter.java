package com.zj.aiagemt.common.auth.filter;

import com.zj.aiagemt.common.auth.exception.AuthenticationException;
import com.zj.aiagemt.config.AuthDebugProperties;
import com.zj.aiagemt.model.entity.User;
import com.zj.aiagemt.repository.UserRepository;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Debug认证过滤器
 * 
 * <p>
 * 用于测试环境的便捷认证方式，允许通过HTTP Header直接传入用户ID。
 * 可通过配置文件的 {@code auth.debug.enabled} 属性启用或禁用。
 * 
 * <p>
 * 请求头格式：{@code debug-user: <userId>}（header名称可配置）
 * 
 * <p>
 * <strong>安全警告：</strong>
 * 此过滤器仅用于开发和测试环境，生产环境必须通过配置禁用！
 * 
 * @author zj
 * @since 2025-12-20
 */
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
            User user = userRepository.selectUserById(userId);
            if(user == null){
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
