package com.zj.aiagemt.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class RestClientConfig {

    /**
     * 使用最新的 Apache HttpClient 5.x 创建 RestClient.Builder
     */
    @Bean("restClientBuilder1")
    @Primary
    public RestClient.Builder restClientBuilder() {
        // 1. 创建连接管理器
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(100)          // 最大连接数
                        .setMaxConnPerRoute(20)        // 每个路由的最大连接数
                        .build();

        // 2. 创建请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(60))    // 从连接池获取连接的超时时间
                .setConnectTimeout(Timeout.ofMinutes(3))               // 连接超时时间 3分钟
                .setResponseTimeout(Timeout.ofMinutes(15))             // 响应超时时间 15分钟
                .build();

        // 3. 创建 HttpClient
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryStrategy(new DefaultHttpRequestRetryStrategy(
                        3,                           // 最大重试次数
                        TimeValue.ofSeconds(2)       // 重试间隔
                ))
                .addRequestInterceptorFirst(new HttpRequestInterceptor() {
                    @Override
                    public void process(HttpRequest request, EntityDetails entity,
                                        HttpContext context) throws HttpException, IOException {
                        log.info("==> Apache HttpClient Request: {} {}",
                                request.getMethod(), request.getRequestUri());

                        // 记录请求头（排除敏感信息）
                        Arrays.stream(request.getHeaders())
                                .filter(header -> !header.getName().toLowerCase().contains("authorization"))
                                .forEach(header -> log.debug("Request Header: {}: {}",
                                        header.getName(), header.getValue()));
                    }
                })
                .addResponseInterceptorFirst(new HttpResponseInterceptor() {
                    @Override
                    public void process(HttpResponse response, EntityDetails entity,
                                        HttpContext context) throws HttpException, IOException {
                        long startTime = (Long) context.getAttribute("request.start.time");
                        long duration = System.currentTimeMillis() - startTime;

                        log.info("<== Apache HttpClient Response: {} in {} ms",
                                response.getCode(), duration);
                    }
                })
                .addExecInterceptorFirst("timing", new ExecChainHandler() {
                    @Override
                    public ClassicHttpResponse execute(ClassicHttpRequest request,
                                                       ExecChain.Scope scope,
                                                       ExecChain chain) throws IOException, HttpException {
                        scope.clientContext.setAttribute("request.start.time", System.currentTimeMillis());
                        return chain.proceed(request, scope);
                    }
                })
                .build();

        // 4. 创建 HttpComponentsClientHttpRequestFactory
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 5. 创建 RestClient.Builder
        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    headers.add(HttpHeaders.USER_AGENT, "SpringAI-RestClient-Apache5/1.0");
                    headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                })
                .requestInterceptors(interceptors -> {
                    interceptors.add(customRequestInterceptor());
                });
    }

    /**
     * 简化版本的 RestClient.Builder（如果不需要复杂配置）
     */
    @Bean("simpleRestClientBuilder")
    public RestClient.Builder simpleRestClientBuilder() {
        // 使用默认配置但设置超时时间
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMinutes(15))
                .setConnectionRequestTimeout(Timeout.ofSeconds(60))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    /**
     * 支持 SSL/TLS 的 RestClient.Builder
     */
    @Bean("sslRestClientBuilder")
    public RestClient.Builder sslRestClientBuilder() throws Exception {
        // 创建自定义的 SSL 上下文（如果需要忽略证书验证）
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (certificate, authType) -> true) // 信任所有证书
                .build();

        // 创建自定义的 Hostname Verifier（如果需要）
        HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;

        // 创建连接管理器
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                                .setSslContext(sslContext)
                                .setHostnameVerifier(hostnameVerifier)
                                .build())
                        .setMaxConnTotal(100)
                        .setMaxConnPerRoute(20)
                        .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMinutes(3))
                .setResponseTimeout(Timeout.ofMinutes(15))
                .setConnectionRequestTimeout(Timeout.ofSeconds(60))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }

    /**
     * 自定义请求拦截器
     */
    private ClientHttpRequestInterceptor customRequestInterceptor() {
        return (request, body, execution) -> {
            // 添加通用请求头
            if (!request.getHeaders().containsKey("X-Request-ID")) {
                request.getHeaders().add("X-Request-ID", UUID.randomUUID().toString());
            }

            // 记录请求信息
            long startTime = System.currentTimeMillis();
            log.info("==> RestClient Request: {} {}", request.getMethod(), request.getURI());

            try {
                ClientHttpResponse response = execution.execute(request, body);
                long duration = System.currentTimeMillis() - startTime;
                log.info("<== RestClient Response: {} in {} ms",
                        response.getStatusCode().value(), duration);
                return response;
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("RestClient Request failed after {} ms: {}", duration, e.getMessage());
                throw e;
            }
        };
    }

    /**
     * 创建具体的 RestClient 实例（可选）
     */
    @Bean
    @ConditionalOnProperty(name = "restclient.apache.auto-config", havingValue = "true", matchIfMissing = false)
    public RestClient restClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.build();
    }

    /**
     * 自定义的连接池监控 Bean（可选）
     */
    @Bean
    @ConditionalOnProperty(name = "restclient.apache.monitoring", havingValue = "true", matchIfMissing = false)
    public ScheduledExecutorService connectionPoolMonitor(RestClient.Builder restClientBuilder) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // 每30秒打印连接池状态
        scheduler.scheduleAtFixedRate(() -> {
            // 这里可以添加连接池监控逻辑
            log.debug("Connection pool monitoring...");
        }, 0, 30, TimeUnit.SECONDS);

        return scheduler;
    }
}