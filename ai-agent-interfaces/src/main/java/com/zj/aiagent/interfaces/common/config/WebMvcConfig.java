package com.zj.aiagent.interfaces.common.config;

import com.zj.aiagent.interfaces.common.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final Environment environment;

    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/client/**", "/api/**") // 拦截所有 client 和 api 接口
                .excludePathPatterns(
                        "/client/user/login", // 排除登录
                        "/client/user/register", // 排除注册
                        "/client/user/send-code", // 排除发送验证码
                        "/client/user/email/sendCode", // 排除发送邮箱验证码
                        "/client/user/email/register", // 排除邮箱注册
                        "/client/user/resetPassword", // 排除重置密码
                        "/api/meta/**" // 排除元数据接口（无需认证）
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var corsConfig = registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // 生产环境使用配置的域名，开发环境允许所有来源
        if (isProdProfile()) {
            if (allowedOrigins != null && !allowedOrigins.isBlank()) {
                corsConfig.allowedOrigins(allowedOrigins.split(","));
            } else {
                // 生产环境未配置 CORS 域名时，禁止跨域访问
                corsConfig.allowedOrigins();
            }
        } else {
            // 开发环境允许所有来源
            corsConfig.allowedOriginPatterns("*");
        }
    }

    private boolean isProdProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
