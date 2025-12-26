package com.zj.aiagent.domain.knowledge.repository;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeChunk;

import java.util.List;
import java.util.Map;

/**
 * 知识库存储接口
 * <p>
 * 定义知识的持久化和检索技术接口，具体实现由基础设施层提供
 * （如向量数据库、Elasticsearch、Spring AI VectorStore 等）
 */
public interface KnowledgeRepository {

    /**
     * 检索相关知识
     *
     * @param executionId 执行ID
     * @param query       查询文本
     * @param topK        返回top K个结果
     * @return 相关知识片段列表（按相关性排序）
     */
    List<KnowledgeChunk> retrieve(String executionId, String query, int topK);

    /**
     * 添加知识
     *
     * @param executionId 执行ID
     * @param content     知识内容
     * @param metadata    元数据
     * @return 知识ID
     */
    String add(String executionId, String content, Map<String, Object> metadata);

    /**
     * 删除知识
     *
     * @param executionId 执行ID
     * @param knowledgeId 知识ID
     */
    void delete(String executionId, String knowledgeId);

    /**
     * 批量添加知识
     *
     * @param executionId  执行ID
     * @param contents     知识内容列表
     * @param metadataList 元数据列表
     * @return 知识ID列表
     */
    List<String> batchAdd(String executionId, List<String> contents, List<Map<String, Object>> metadataList);
}
