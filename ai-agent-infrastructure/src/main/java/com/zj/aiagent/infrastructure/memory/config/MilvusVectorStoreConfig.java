package com.zj.aiagent.infrastructure.memory.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Milvus VectorStore 配置类
 * 
 * 双集合配置：
 * 1. knowledgeVectorStore - agent_knowledge_base (知识库)
 * 2. memoryVectorStore - agent_chat_memory (长期记忆)
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "milvus")
@ConditionalOnProperty(prefix = "milvus", name = "enabled", havingValue = "true", matchIfMissing = false)
public class MilvusVectorStoreConfig {

    /**
     * Milvus 服务地址
     */
    private String host = "localhost";

    /**
     * Milvus 服务端口
     */
    private int port = 19530;

    /**
     * 用户名（可选）
     */
    private String username = "";

    /**
     * 密码（可选）
     */
    private String password = "";

    /**
     * 知识库集合名称
     */
    private String knowledgeCollectionName = "agent_knowledge_base";

    /**
     * 记忆集合名称
     */
    private String memoryCollectionName = "agent_chat_memory";

    /**
     * 向量维度（需与 Embedding 模型匹配）
     */
    private int embeddingDimension = 768;

    /**
     * Milvus 客户端 Bean
     */
    @Bean
    public MilvusServiceClient milvusServiceClient() {
        log.info("[Milvus] Connecting to {}:{}", host, port);

        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port);

        if (username != null && !username.isEmpty()) {
            builder.withAuthorization(username, password);
        }

        return new MilvusServiceClient(builder.build());
    }

    /**
     * 知识库 VectorStore
     * 用于 RAG 检索
     */
    @Bean(name = "knowledgeVectorStore")
    @Primary
    public MilvusVectorStore knowledgeVectorStore(
            MilvusServiceClient milvusClient,
            EmbeddingModel embeddingModel) {
        log.info("[Milvus] Initializing knowledge VectorStore: collection={}", knowledgeCollectionName);

        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .collectionName(knowledgeCollectionName)
                .embeddingDimension(embeddingDimension)
                .build();
    }

    /**
     * 记忆 VectorStore
     * 用于长期记忆存储和检索
     */
    @Bean(name = "memoryVectorStore")
    public MilvusVectorStore memoryVectorStore(
            MilvusServiceClient milvusClient,
            EmbeddingModel embeddingModel) {
        log.info("[Milvus] Initializing memory VectorStore: collection={}", memoryCollectionName);

        return MilvusVectorStore.builder(milvusClient, embeddingModel)
                .collectionName(memoryCollectionName)
                .embeddingDimension(embeddingDimension)
                .build();
    }
}
