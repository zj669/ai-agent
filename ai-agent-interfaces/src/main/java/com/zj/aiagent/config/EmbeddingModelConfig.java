package com.zj.aiagent.config;

import lombok.Data;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Embedding模型配置类
 * 从配置文件读取 spring.ai.openai.embedding 相关配置
 */
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
     * 创建 EmbeddingModel Bean
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .model(model)
                .dimensions(1024)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, embeddingOptions);
    }
}
