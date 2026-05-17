package com.zj.aiagent.infrastructure.workflow.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import com.zj.aiagent.infrastructure.workflow.template.PromptTemplateResolver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * HTTP 节点执行策略
 * 使用 WebClient 发送 HTTP 请求
 */
@Slf4j
@Component
public class HttpNodeExecutorStrategy implements NodeExecutorStrategy {

    private final WebClient.Builder webClientBuilder;
    private final Executor executor;
    private final ObjectMapper objectMapper;
    private final PromptTemplateResolver promptTemplateResolver;

    public HttpNodeExecutorStrategy(
            WebClient.Builder webClientBuilder,
            @Qualifier("nodeExecutorThreadPool") Executor executor,
            ObjectMapper objectMapper,
            PromptTemplateResolver promptTemplateResolver) {
        this.webClientBuilder = webClientBuilder;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.promptTemplateResolver = promptTemplateResolver;
    }

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                NodeConfig config = node.getConfig();

                // 解析 URL
                String url = resolveTemplate(firstNonBlank(
                        config.getString("http_url"),
                        config.getString("url")
                ), resolvedInputs);
                String methodStr = firstNonBlank(
                        config.getString("http_method"),
                        config.getString("method"),
                        "GET"
                );
                HttpMethod method = HttpMethod.valueOf(methodStr.toUpperCase());

                log.info("[HTTP Node {}] {} {}", node.getNodeId(), method, url);

                // 设置超时
                Long timeout = config.getLong("readTimeout");
                if (timeout == null) {
                    timeout = config.getLong("timeout");
                }
                if (timeout == null) {
                    timeout = 30000L;
                }
                Duration requestTimeout = Duration.ofMillis(timeout);

                // 获取 Headers
                Map<String, Object> headers = resolveHeaders(config, resolvedInputs);
                String bodyTemplate = firstNonBlank(
                        config.getString("http_body"),
                        config.getString("bodyTemplate")
                );

                // 使用 exchangeToMono 获取状态码
                WebClient webClient = webClientBuilder.build();
                int[] statusCodeHolder = new int[1];
                String[] responseHolder = new String[1];

                try {
                    String bodyResult;
                    if (bodyTemplate != null && !bodyTemplate.isEmpty()) {
                        String body = resolveTemplate(bodyTemplate, resolvedInputs);
                        bodyResult = webClient.method(method)
                                .uri(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .headers(h -> {
                                    if (headers != null) {
                                        headers.forEach((key, value) -> {
                                            if (value != null) {
                                                h.set(key, resolveTemplate(value.toString(), resolvedInputs));
                                            }
                                        });
                                    }
                                })
                                .bodyValue(body)
                                .exchangeToMono(clientResponse -> {
                                    statusCodeHolder[0] = clientResponse.statusCode().value();
                                    return clientResponse.bodyToMono(String.class);
                                })
                                .block(requestTimeout);
                    } else {
                        bodyResult = webClient.method(method)
                                .uri(url)
                                .headers(h -> {
                                    if (headers != null) {
                                        headers.forEach((key, value) -> {
                                            if (value != null) {
                                                h.set(key, resolveTemplate(value.toString(), resolvedInputs));
                                            }
                                        });
                                    }
                                })
                                .exchangeToMono(clientResponse -> {
                                    statusCodeHolder[0] = clientResponse.statusCode().value();
                                    return clientResponse.bodyToMono(String.class);
                                })
                                .block(requestTimeout);
                    }
                    responseHolder[0] = bodyResult;
                } catch (Exception e) {
                    log.error("[HTTP Node {}] Request failed: {}", node.getNodeId(), e.getMessage());
                    streamPublisher.publishError(e.getMessage());
                    return NodeExecutionResult.failed(e.getMessage());
                }

                int statusCode = statusCodeHolder[0];
                String response = responseHolder[0] != null ? responseHolder[0] : "";

                log.info("[HTTP Node {}] Response received, statusCode: {}, length: {}",
                        node.getNodeId(), statusCode, response.length());

                // 构建输出
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("response", response);
                outputs.put("body", response);
                outputs.put("statusCode", statusCode);
                outputs.put(
                        "http_response",
                        Map.of(
                                "statusCode", statusCode,
                                "body", response,
                                "response", response
                        )
                );

                return NodeExecutionResult.success(outputs);

            } catch (Exception e) {
                log.error("[HTTP Node {}] Execution failed: {}", node.getNodeId(), e.getMessage(), e);
                streamPublisher.publishError(e.getMessage());
                return NodeExecutionResult.failed(e.getMessage());
            }
        }, executor);
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.HTTP;
    }

    /**
     * 解析模板中的占位符
     * 支持 #{key} 和 {{key}} 两种格式
     */
    private String resolveTemplate(String template, Map<String, Object> resolvedInputs) {
        if (template == null)
            return null;

        ExecutionContext context =
                (ExecutionContext) resolvedInputs.get("__context__");
        return promptTemplateResolver.resolve(template, resolvedInputs, context);
    }

    private Map<String, Object> resolveHeaders(
            NodeConfig config,
            Map<String, Object> resolvedInputs) {
        Map<String, Object> headers = config.getMap("headers");
        if (headers == null) {
            headers = config.getMap("http_headers");
        }
        if (headers != null) {
            return headers;
        }

        String headerText = firstNonBlank(
                config.getString("http_headers"),
                config.getString("headers")
        );
        if (headerText == null) {
            return null;
        }

        String resolvedHeaderText = resolveTemplate(headerText, resolvedInputs);
        try {
            return objectMapper.readValue(
                    resolvedHeaderText,
                    new TypeReference<Map<String, Object>>() {}
            );
        } catch (Exception e) {
            log.warn(
                    "[HTTP Node] Failed to parse headers as JSON, headerText={}",
                    resolvedHeaderText
            );
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
