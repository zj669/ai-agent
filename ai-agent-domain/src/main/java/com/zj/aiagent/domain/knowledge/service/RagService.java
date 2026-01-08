package com.zj.aiagent.domain.knowledge.service;

import com.zj.aiagent.domain.knowledge.RagProvider;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeChunk;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * RAG 领域服务
 * <p>
 * 实现知识检索的业务逻辑，依赖技术接口（Repository）
 */
@Slf4j
@Service
@AllArgsConstructor
public class RagService implements RagProvider {

    private final KnowledgeRepository knowledgeRepository;

    @Override
    public List<KnowledgeChunk> retrieveKnowledge(String executionId, String query, int topK) {
        log.debug("[{}] 检索知识: query={}, topK={}", executionId, query, topK);

        List<KnowledgeChunk> results = knowledgeRepository.retrieve(executionId, query, topK);

        log.info("[{}] 检索到 {} 条相关知识", executionId, results.size());
        return results;
    }

    @Override
    public void addKnowledge(String executionId, String content, Map<String, Object> metadata) {
        log.debug("[{}] 添加知识: {} 字符", executionId, content.length());

        String knowledgeId = knowledgeRepository.add(executionId, content, metadata);

        log.info("[{}] 知识已添加: {}", executionId, knowledgeId);
    }

    @Override
    public void deleteKnowledge(String executionId, String knowledgeId) {
        log.info("[{}] 删除知识: {}", executionId, knowledgeId);

        knowledgeRepository.delete(executionId, knowledgeId);

        log.debug("[{}] 知识已删除", executionId);
    }

    // ========== 扩展业务方法 ==========

    /**
     * 批量添加知识（业务逻辑封装）
     *
     * @param executionId  执行ID
     * @param contents     知识内容列表
     * @param metadataList 元数据列表
     */
    public void batchAddKnowledge(String executionId, List<String> contents, List<Map<String, Object>> metadataList) {
        log.info("[{}] 批量添加知识: {} 条", executionId, contents.size());

        List<String> ids = knowledgeRepository.batchAdd(executionId, contents, metadataList);

        log.info("[{}] 批量添加完成: {} 条成功", executionId, ids.size());
    }
}
