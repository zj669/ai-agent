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

                // 设置超时
                Long timeout = config.getLong("readTimeout");
                if (timeout == null) {
                    timeout = 30000L;
                }
                Duration requestTimeout = Duration.ofMillis(timeout);

                // 获取 Headers
                Map<String, Object> headers = config.getMap("headers");
                String bodyTemplate = config.getString("bodyTemplate");

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

        String result = template;
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            // 跳过 __ 前缀的内部变量
            if (entry.getKey().startsWith("__")) continue;
            // null 值跳过替换，保留原始占位符
            if (entry.getValue() != null) {
                String value = entry.getValue().toString();
                result = result.replace("#{" + entry.getKey() + "}", value);
                // 支持 {{key}} Mustache 风格占位符
                result = result.replace("{{" + entry.getKey() + "}}", value);
            }
        }
        return result;
    }
}
