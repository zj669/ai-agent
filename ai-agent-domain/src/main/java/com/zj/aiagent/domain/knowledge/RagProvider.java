package com.zj.aiagent.domain.knowledge;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeChunk;

import java.util.List;
import java.util.Map;

/**
 * RAG Provider - 管理知识库检索
 */
public interface RagProvider {

    /**
     * 检索相关知识
     * 
     * @param executionId 执行ID
     * @param query       查询文本
     * @param topK        返回Top K个结果
     * @return 相关知识片段
     */
    List<KnowledgeChunk> retrieveKnowledge(String executionId, String query, int topK);

    /**
     * 添加知识到向量库
     * 
     * @param executionId 执行ID
     * @param content     知识内容
     * @param metadata    元数据
     */
    void addKnowledge(String executionId, String content, Map<String, Object> metadata);

    /**
     * 删除知识
     * 
     * @param executionId 执行ID
     * @param knowledgeId 知识ID
     */
    void deleteKnowledge(String executionId, String knowledgeId);
}
