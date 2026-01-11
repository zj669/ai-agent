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
                .addPathPatterns("/client/**") // 拦截所有 client 接口
                .excludePathPatterns(
                        "/client/user/login", // 排除登录
                        "/client/user/register", // 排除注册
                        "/client/user/send-code" // 排除发送验证码
                );
    }
}
