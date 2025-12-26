package com.zj.aiagent.infrastructure.context.rag;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeChunk;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knowledge Repository 内存实现
 * <p>
 * 基于内存存储的临时实现，生产环境应替换为向量数据库
 */
@Slf4j
@Repository
public class InMemoryKnowledgeRepository implements KnowledgeRepository {

    // 临时存储: executionId -> 知识列表
    private final Map<String, List<KnowledgeChunk>> knowledgeStore = new ConcurrentHashMap<>();

    @Override
    public List<KnowledgeChunk> retrieve(String executionId, String query, int topK) {
        // TODO: 实现向量检索
        List<KnowledgeChunk> allKnowledge = knowledgeStore.getOrDefault(executionId, new ArrayList<>());
        int limit = Math.min(topK, allKnowledge.size());

        log.debug("[{}] [InMemory] 检索知识: query={}, 返回 {} 条（总共 {} 条）",
                executionId, query, limit, allKnowledge.size());

        return new ArrayList<>(allKnowledge.subList(Math.max(0, allKnowledge.size() - limit), allKnowledge.size()));
    }

    @Override
    public String add(String executionId, String content, Map<String, Object> metadata) {
        String knowledgeId = UUID.randomUUID().toString();

        KnowledgeChunk chunk = KnowledgeChunk.builder()
                .id(knowledgeId)
                .content(content)
                .metadata(metadata != null ? metadata : Map.of())
                .score(1.0)
                .build();

        knowledgeStore.computeIfAbsent(executionId, k -> new ArrayList<>()).add(chunk);

        log.debug("[{}] [InMemory] 添加知识: id={}, {} 字符", executionId, knowledgeId, content.length());
        return knowledgeId;
    }

    @Override
    public void delete(String executionId, String knowledgeId) {
        List<KnowledgeChunk> knowledge = knowledgeStore.get(executionId);
        if (knowledge != null) {
            knowledge.removeIf(chunk -> chunk.getId().equals(knowledgeId));
            log.debug("[{}] [InMemory] 删除知识: {}", executionId, knowledgeId);
        }
    }

    @Override
    public List<String> batchAdd(String executionId, List<String> contents, List<Map<String, Object>> metadataList) {
        List<String> ids = new ArrayList<>();

        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i);
            Map<String, Object> metadata = i < metadataList.size() ? metadataList.get(i) : Map.of();
            String id = add(executionId, content, metadata);
            ids.add(id);
        }

        log.info("[{}] [InMemory] 批量添加知识: {} 条", executionId, ids.size());
        return ids;
    }
}
