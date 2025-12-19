package com.zj.aiagemt.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试环境的向量存储配置
 * 
 * <p>
 * 使用内存实现的 SimpleVectorStore 替代生产环境的 PgVectorStore，
 * 避免测试依赖外部数据库
 * </p>
 */
@TestConfiguration
public class TestVectorStoreConfig {

    /**
     * 为测试环境提供 SimpleVectorStore
     * 
     * @param embeddingModel Embedding模型
     * @return SimpleVectorStore实例
     */
    @Bean
    @Primary // 标记为主要Bean，覆盖生产环境的PgVectorStore
    public VectorStore testVectorStore(@Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel) {
        // SimpleVectorStore 通过Builder创建
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
