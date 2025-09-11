package com.zj.aiagemt.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

@Component
public class EmbedingModelConfig {
    @Value("${spring.ai.openai.embedding.model}")
    private String embeddingModel;
    @Value("${spring.ai.openai.embedding.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.embedding.endpoint}")
    private String endpoint;

    @Bean
    public PgVectorStore pgVectorStore(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate, WebClient.Builder client) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey("sk-3765ba6955534eeba26b2838cd30a600")
                .embeddingsPath( endpoint)
                .completionsPath( endpoint)
                .webClientBuilder(client).build();
        MetadataMode embed = MetadataMode.EMBED;
        OpenAiEmbeddingOptions build = OpenAiEmbeddingOptions.builder().model(embeddingModel).dimensions(768).build();
        RetryTemplate retryTemplate = RetryTemplate.defaultInstance();


        OpenAiEmbeddingModel embeddingModel = new OpenAiEmbeddingModel(openAiApi,embed, build, retryTemplate);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
