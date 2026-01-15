package com.zj.aiagent.domain.memory.port;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.List;
import java.util.Map;

/**
 * 向量存储端口
 * 
 * 职责:
 * 1. 长期记忆 (LTM) 存储和检索
 * 2. 知识库文档存储和检索
 * 3. 支持基于 Metadata Filter 的精准隔离
 * 
 * 实现方应对接向量数据库（如 Milvus、Pinecone、PgVector）
 */
public interface VectorStore {

    /**
     * 根据查询文本检索相关记忆
     * 
     * @param query   查询文本（用户意图）
     * @param agentId Agent ID（用于范围隔离）
     * @param topK    返回结果数量
     * @return 相关记忆列表（文本形式）
     */
    List<String> search(String query, Long agentId, int topK);

    /**
     * 根据查询文本检索相关记忆（使用默认 topK）
     */
    default List<String> search(String query, Long agentId) {
        return search(query, agentId, 5);
    }

    /**
     * 存储记忆到向量库
     * 
     * @param agentId  Agent ID
     * @param content  记忆内容
     * @param metadata 元数据（如来源、时间等）
     */
    void store(Long agentId, String content, java.util.Map<String, Object> metadata);

    /**
     * 批量存储记忆
     */
    void storeBatch(Long agentId, List<String> contents);

    // ==================== 知识库相关方法 ====================

    /**
     * 使用 Spring AI SearchRequest 进行高级检索
     * 支持 Metadata Filter 过滤条件
     * 
     * @param request Spring AI 的 SearchRequest 对象
     * @return 检索结果 (Document 列表)
     */
    List<Document> similaritySearch(SearchRequest request);

    /**
     * 根据 datasetId 和 query 检索知识库
     * (便捷方法)
     */
    default List<String> searchKnowledgeByDataset(String datasetId, String query, int topK) {
        // 子类应实现此方法
        throw new UnsupportedOperationException("searchKnowledgeByDataset not implemented");
    }

    /**
     * 批量存储文档到知识库
     * 
     * @param documents Spring AI 的 Document 列表（包含 content 和 metadata）
     */
    void addDocuments(List<Document> documents);

    /**
     * 根据 Metadata 删除向量
     * 
     * @param filter Metadata 过滤条件 (如: {"documentId": "doc_123"})
     */
    void deleteByMetadata(Map<String, Object> filter);
}
