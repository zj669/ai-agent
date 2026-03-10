package com.zj.aiagent.infrastructure.memory;

import com.zj.aiagent.domain.memory.port.VectorStore;
import com.zj.aiagent.domain.memory.valobj.Document;
import com.zj.aiagent.domain.memory.valobj.SearchRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Milvus 关闭时的空实现。
 *
 * 目标是保证应用在无向量存储场景下仍可启动，
 * 相关检索/存储请求统一退化为空结果并记录告警。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "milvus", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoOpVectorStoreAdapter implements VectorStore {

    @PostConstruct
    void init() {
        log.warn("[VectorStore] Milvus 未启用，已切换为 NoOpVectorStoreAdapter。向量检索与写入能力将返回空结果。");
    }

    @Override
    public List<String> search(String query, Long agentId, int topK) {
        log.warn("[VectorStore] 已跳过长期记忆检索：milvus.enabled=false, agentId={}, topK={}", agentId, topK);
        return List.of();
    }

    @Override
    public void store(Long agentId, String content, Map<String, Object> metadata) {
        log.warn("[VectorStore] 已跳过长期记忆写入：milvus.enabled=false, agentId={}", agentId);
    }

    @Override
    public void storeBatch(Long agentId, List<String> contents) {
        log.warn("[VectorStore] 已跳过长期记忆批量写入：milvus.enabled=false, agentId={}, size={}",
                agentId, contents == null ? 0 : contents.size());
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        log.warn("[VectorStore] 已跳过相似度检索：milvus.enabled=false, query={}",
                request == null ? null : request.getQuery());
        return List.of();
    }

    @Override
    public List<String> searchKnowledgeByDataset(String datasetId, String query, int topK) {
        log.warn("[VectorStore] 已跳过知识库检索：milvus.enabled=false, datasetId={}, topK={}", datasetId, topK);
        return List.of();
    }

    @Override
    public void addDocuments(List<Document> documents) {
        log.warn("[VectorStore] 已跳过知识文档写入：milvus.enabled=false, size={}",
                documents == null ? 0 : documents.size());
    }

    @Override
    public void deleteByMetadata(Map<String, Object> filter) {
        log.warn("[VectorStore] 已跳过向量删除：milvus.enabled=false, filter={}", filter);
    }
}
