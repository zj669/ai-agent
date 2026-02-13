package com.zj.aiagent.interfaces.common.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * HTTPS 配置
 *
 * 生产环境强制使用 HTTPS,自动将 HTTP 请求重定向到 HTTPS
 *
 * @author backend-developer-2
 * @since 2025-02-10
 */
@Configuration
@Profile("prod")  // 仅在生产环境启用
public class HttpsConfig {

    /**
     * 配置 Tomcat,支持 HTTP 到 HTTPS 的重定向
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                // 配置安全约束,强制使用 HTTPS
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");

                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");

                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };

        // 添加 HTTP 连接器,用于重定向到 HTTPS
        tomcat.addAdditionalTomcatConnectors(createHttpConnector());

        return tomcat;
    }

    /**
     * 创建 HTTP 连接器
     *
     * 监听 8080 端口,将所有 HTTP 请求重定向到 HTTPS (8443 端口)
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);  // 重定向到 HTTPS 端口
        return connector;
    }
}
