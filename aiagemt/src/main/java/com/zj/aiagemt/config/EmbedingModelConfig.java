package com.zj.aiagemt.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

@Component
public class EmbedingModelConfig {
    @Value("${spring.ai.ollama.embedding.model}")
    private String embeddingModel;
    @Value("${spring.ai.ollama.base-url}")
    private String baseUrl;

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel() {
        return new OllamaEmbeddingModel(OllamaApi.builder().baseUrl(baseUrl).build()
                , OllamaOptions.builder().model(embeddingModel).build()
                , ObservationRegistry.create()
        , ModelManagementOptions.builder().timeout(Duration.ofSeconds(5)).maxRetries(3).build()
        );
    }
    @Bean
    public PgVectorStore pgVectorStore(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate, WebClient.Builder client) {
        OllamaApi build2 = OllamaApi.builder().baseUrl(baseUrl).webClientBuilder(client).build();
        OllamaOptions build = OllamaOptions.builder().model(embeddingModel).build();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        ModelManagementOptions build1 = ModelManagementOptions.builder().timeout(Duration.ofSeconds(5)).maxRetries(3).build();
        OllamaEmbeddingModel ollamaEmbeddingModel = new OllamaEmbeddingModel(build2, build, observationRegistry, build1);
        return PgVectorStore.builder(jdbcTemplate, ollamaEmbeddingModel).build();
    }
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
