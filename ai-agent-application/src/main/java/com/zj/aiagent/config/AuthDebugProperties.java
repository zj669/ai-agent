package com.zj.aiagent.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Debug认证配置 - 应用层
 * 
 * <p>
 * 用于测试环境的便捷认证方式，允许通过HTTP Header直接传入用户ID。
 * 
 * <p>
 * <strong>安全警告：</strong>
 * 此功能仅用于开发和测试环境，生产环境必须禁用！
 * 
 * @author zj
 * @since 2025-12-20
 */
@Configuration
@ConfigurationProperties(prefix = "auth.debug")
@Data
@Slf4j
public class AuthDebugProperties {

    /**
     * 是否启用debug认证模式
     * 默认为false，生产环境必须保持false
     */
    private boolean enabled = false;

    /**
     * Debug模式使用的HTTP Header名称
     * 默认为 "debug-user"
     */
    private String headerName = "debug-user";

    /**
     * 初始化时输出警告信息
     */
    @PostConstruct
    public void init() {
        if (enabled) {
            log.warn("⚠️ ========================================");
            log.warn("⚠️  DEBUG认证模式已启用！");
            log.warn("⚠️  Header名称: {}", headerName);
            log.warn("⚠️  此模式仅用于开发/测试环境");
            log.warn("⚠️  请确保不在生产环境使用！");
            log.warn("⚠️ ========================================");
        } else {
            log.info("Debug认证模式已禁用");
        }
    }
}
