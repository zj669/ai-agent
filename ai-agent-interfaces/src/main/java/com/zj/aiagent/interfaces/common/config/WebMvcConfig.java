package com.zj.aiagent.interfaces.common.config;

import com.zj.aiagent.interfaces.common.interceptor.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

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
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
