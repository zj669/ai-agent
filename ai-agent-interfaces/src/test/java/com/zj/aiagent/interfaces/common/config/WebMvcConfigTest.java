package com.zj.aiagent.interfaces.common.config;

import com.zj.aiagent.interfaces.common.interceptor.LoginInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebMvcConfigTest {

    @Mock
    private LoginInterceptor loginInterceptor;

    @Mock
    private Environment environment;

    @Test
    void shouldExcludeRefreshEndpointFromAuthInterceptor() {
        WebMvcConfig config = new WebMvcConfig(loginInterceptor, environment);
        InterceptorRegistry registry = new InterceptorRegistry();

        config.addInterceptors(registry);

        @SuppressWarnings("unchecked")
        List<InterceptorRegistration> registrations =
                (List<InterceptorRegistration>) ReflectionTestUtils.getField(registry, "registrations");
        assertThat(registrations).isNotNull().hasSize(1);

        @SuppressWarnings("unchecked")
        List<String> excludePatterns =
                (List<String>) ReflectionTestUtils.getField(registrations.get(0), "excludePatterns");
        assertThat(excludePatterns).contains("/client/user/refresh");
    }
}
