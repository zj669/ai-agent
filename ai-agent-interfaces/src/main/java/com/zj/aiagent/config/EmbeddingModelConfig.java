package com.zj.aiagent.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static java.util.Objects.nonNull;

/**
 * Embedding模型配置类
 * 从配置文件读取 spring.ai.openai.embedding 相关配置
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "spring.ai.openai.embedding")
public class EmbeddingModelConfig {

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * Embedding API端点
     */
    private String endpoint;

    /**
     * 模型名称
     */
    private String model;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型选项（如 num_ctx 等）
     */
    private Map<String, Object> options;

    /**
     * 向量维度（需与 Milvus embedding-dimension 保持一致）
     */
    private int dimensions = 1024;

    /**
     * 创建 EmbeddingModel Bean
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        String resolvedBaseUrl = resolveBaseUrl();
        log.warn("[EmbeddingConfig] baseUrl={}, endpoint={}, resolvedBaseUrl={}, model={}, dimensions={}",
                baseUrl,
                endpoint,
                resolvedBaseUrl,
                model,
                dimensions);

        String resolvedEmbeddingsPath = resolveEmbeddingsPath(resolvedBaseUrl);
        log.warn("[EmbeddingConfig] resolvedEmbeddingsPath={}", resolvedEmbeddingsPath);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(resolvedBaseUrl)
                .embeddingsPath(resolvedEmbeddingsPath)
                .apiKey(apiKey)
                .build();

        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(model)
                .dimensions(dimensions)
                .build();

        // 覆写 dimensions()，直接返回配置值，避免向远端服务发起探测请求
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, embeddingOptions) {
            @Override
            public int dimensions() {
                return dimensions;
            }
        };
    }

    /**
     * 统一处理 baseUrl / endpoint：
     * - 若 endpoint 形如 .../v1/embeddings，则降级为 .../v1（OpenAiApi 会自动拼 /embeddings）
     * - 否则优先使用 baseUrl
     */
    private String resolveBaseUrl() {
        if (nonNull(endpoint) && !endpoint.isBlank()) {
            String ep = endpoint.trim();
            if (ep.endsWith("/embeddings")) {
                return ep.substring(0, ep.length() - "/embeddings".length());
            }
            return ep;
        }
        return baseUrl;
    }

    /**
     * Spring AI OpenAiApi 默认 embeddingsPath 是 /v1/embeddings。
     * 当 baseUrl 已经包含 /v1 时，需要覆写成 /embeddings，避免拼成 /v1/v1/embeddings。
     */
    private String resolveEmbeddingsPath(String resolvedBaseUrl) {
        if (resolvedBaseUrl != null && resolvedBaseUrl.endsWith("/v1")) {
            return "/embeddings";
        }
        return "/v1/embeddings";
    }
}
