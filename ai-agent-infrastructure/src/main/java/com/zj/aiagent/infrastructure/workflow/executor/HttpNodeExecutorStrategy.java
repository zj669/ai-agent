package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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

    public HttpNodeExecutorStrategy(
            WebClient.Builder webClientBuilder,
            @Qualifier("nodeExecutorThreadPool") Executor executor) {
        this.webClientBuilder = webClientBuilder;
        this.executor = executor;
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
                String url = resolveTemplate(config.getString("url"), resolvedInputs);
                String methodStr = config.getString("method", "GET");
                HttpMethod method = HttpMethod.valueOf(methodStr.toUpperCase());

                log.info("[HTTP Node {}] {} {}", node.getNodeId(), method, url);

                // 构建请求
                WebClient webClient = webClientBuilder.build();
                WebClient.RequestBodySpec requestSpec = webClient
                        .method(method)
                        .uri(url);

                // 设置 Headers
                Map<String, Object> headers = config.getMap("headers");
                if (headers != null) {
                    headers.forEach((key, value) -> {
                        if (value != null) {
                            requestSpec.header(key, resolveTemplate(value.toString(), resolvedInputs));
                        }
                    });
                }

                // 设置 Body
                WebClient.ResponseSpec responseSpec;
                String bodyTemplate = config.getString("bodyTemplate");
                if (bodyTemplate != null && !bodyTemplate.isEmpty()) {
                    String body = resolveTemplate(bodyTemplate, resolvedInputs);
                    responseSpec = requestSpec
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(body)
                            .retrieve();
                } else {
                    responseSpec = requestSpec.retrieve();
                }

                // 执行请求
                Long timeout = config.getLong("readTimeout");
                if (timeout == null) {
                    timeout = 30000L;
                }
                String response = responseSpec
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(timeout))
                        .block();

                log.info("[HTTP Node {}] Response received, length: {}", node.getNodeId(),
                        response != null ? response.length() : 0);

                // 构建输出
                Map<String, Object> outputs = new HashMap<>();
                outputs.put("response", response);
                outputs.put("body", response);
                outputs.put("statusCode", 200); // 简化处理

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
     */
    private String resolveTemplate(String template, Map<String, Object> resolvedInputs) {
        if (template == null)
            return null;

        String result = template;
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            String placeholder = "#{" + entry.getKey() + "}";
            if (entry.getValue() != null) {
                result = result.replace(placeholder, entry.getValue().toString());
            }
        }
        return result;
    }
}
