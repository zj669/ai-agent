package com.zj.aiagent.infrastructure.memory;

import com.zj.aiagent.domain.memory.port.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 Milvus 的 VectorStore 适配器
 * 
 * 将领域层 VectorStore 端口映射到 Spring AI 的 Milvus VectorStore
 * 
 * 功能：
 * 1. 知识库检索 (agent_knowledge_base)
 * 2. 长期记忆存储/检索 (agent_chat_memory)
 */
@Slf4j
@Component
@ConditionalOnBean(name = "memoryVectorStore")
public class MilvusVectorStoreAdapter implements VectorStore {

    private final org.springframework.ai.vectorstore.VectorStore knowledgeStore;
    private final org.springframework.ai.vectorstore.VectorStore memoryStore;

    public MilvusVectorStoreAdapter(
            @Qualifier("knowledgeVectorStore") org.springframework.ai.vectorstore.VectorStore knowledgeStore,
            @Qualifier("memoryVectorStore") org.springframework.ai.vectorstore.VectorStore memoryStore) {
        this.knowledgeStore = knowledgeStore;
        this.memoryStore = memoryStore;
        log.info("[MilvusVectorStoreAdapter] Initialized with knowledge and memory stores");
    }

    /**
     * 搜索长期记忆（LTM）
     * 从 agent_chat_memory 集合检索
     */
    @Override
    public List<String> search(String query, Long agentId, int topK) {
        log.debug("[VectorStore] Searching memory for query: '{}', agentId: {}, topK: {}",
                query.length() > 50 ? query.substring(0, 50) + "..." : query,
                agentId, topK);

        try {
            // 构建搜索请求，使用 agentId 过滤
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("agent_id == " + agentId)
                    .build();

            List<Document> results = memoryStore.similaritySearch(request);

            log.debug("[VectorStore] Found {} results", results.size());

            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("[VectorStore] Memory search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 搜索知识库
     * 从 agent_knowledge_base 集合检索
     */
    public List<String> searchKnowledge(String query, Long agentId, int topK) {
        log.debug("[VectorStore] Searching knowledge for query: '{}', agentId: {}, topK: {}",
                query.length() > 50 ? query.substring(0, 50) + "..." : query,
                agentId, topK);

        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("agent_id == " + agentId)
                    .build();

            List<Document> results = knowledgeStore.similaritySearch(request);

            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("[VectorStore] Knowledge search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 存储到长期记忆
     */
    @Override
    public void store(Long agentId, String content, Map<String, Object> metadata) {
        log.debug("[VectorStore] Storing content to memory, agentId: {}, length: {}",
                agentId, content.length());

        try {
            // 添加 agent_id 到元数据
            Map<String, Object> enrichedMetadata = new java.util.HashMap<>(metadata);
            enrichedMetadata.put("agent_id", agentId);
            enrichedMetadata.put("timestamp", System.currentTimeMillis());

            Document document = new Document(content, enrichedMetadata);
            memoryStore.add(List.of(document));

            log.debug("[VectorStore] Content stored successfully");

        } catch (Exception e) {
            log.error("[VectorStore] Failed to store content: {}", e.getMessage(), e);
        }
    }

    /**
     * 批量存储到长期记忆
     */
    @Override
    public void storeBatch(Long agentId, List<String> contents) {
        log.debug("[VectorStore] Batch storing {} items to memory for agentId: {}",
                contents.size(), agentId);

        try {
            List<Document> documents = contents.stream()
                    .map(content -> new Document(content, Map.of(
                            "agent_id", agentId,
                            "timestamp", System.currentTimeMillis())))
                    .collect(Collectors.toList());

            memoryStore.add(documents);

            log.debug("[VectorStore] Batch stored {} items successfully", contents.size());

        } catch (Exception e) {
            log.error("[VectorStore] Failed to batch store: {}", e.getMessage(), e);
        }
    }

    /**
     * 存储到知识库
     */
    public void storeKnowledge(Long agentId, String content, Map<String, Object> metadata) {
        log.debug("[VectorStore] Storing content to knowledge, agentId: {}, length: {}",
                agentId, content.length());

        try {
            Map<String, Object> enrichedMetadata = new java.util.HashMap<>(metadata);
            enrichedMetadata.put("agent_id", agentId);

            Document document = new Document(content, enrichedMetadata);
            knowledgeStore.add(List.of(document));

        } catch (Exception e) {
            log.error("[VectorStore] Failed to store knowledge: {}", e.getMessage(), e);
        }
    }

    // ==================== 知识库新增方法实现 ====================

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        log.debug("[VectorStore] Similarity search with request: query='{}', topK={}",
                request.getQuery() != null
                        ? request.getQuery().substring(0, Math.min(50, request.getQuery().length())) + "..."
                        : "null",
                request.getTopK());

        try {
            // 使用 knowledgeStore 执行检索
            return knowledgeStore.similaritySearch(request);
        } catch (Exception e) {
            log.warn("[VectorStore] Similarity search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<String> searchKnowledgeByDataset(String datasetId, String query, int topK) {
        log.debug("[VectorStore] Searching knowledge by dataset: datasetId={}, query='{}', topK={}",
                datasetId, query.substring(0, Math.min(50, query.length())), topK);

        try {
            // 构建 Filter: datasetId == xxx
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("dataset_id == '" + datasetId + "'")
                    .build();

            List<Document> results = knowledgeStore.similaritySearch(request);

            return results.stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("[VectorStore] Search by dataset failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        log.debug("[VectorStore] Adding {} documents to knowledge store", documents.size());

        try {
            knowledgeStore.add(documents);
            log.debug("[VectorStore] Successfully added {} documents", documents.size());
        } catch (Exception e) {
            log.error("[VectorStore] Failed to add documents: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to add documents to vector store", e);
        }
    }

    @Override
    public void deleteByMetadata(Map<String, Object> filter) {
        log.debug("[VectorStore] Deleting documents by metadata filter: {}", filter);

        try {
            // 1. 构建 Filter 表达式
            String filterExpression = buildFilterExpression(filter);
            if (filterExpression.isEmpty()) {
                log.warn("[VectorStore] Empty filter, skipping delete");
                return;
            }

            // 2. 检索所有匹配的文档
            // 注意：使用一个通用查询词避免空查询问题
            SearchRequest request = SearchRequest.builder()
                    .query("*") // 通配查询
                    .topK(1000) // 足够大的 topK 以获取所有匹配文档
                    .filterExpression(filterExpression)
                    .build();

            List<Document> results = knowledgeStore.similaritySearch(request);

            if (results.isEmpty()) {
                log.debug("[VectorStore] No documents found matching filter: {}", filter);
                return;
            }

            // 3. 提取 Document IDs
            List<String> ids = results.stream()
                    .map(Document::getId)
                    .filter(id -> id != null && !id.isEmpty())
                    .collect(Collectors.toList());

            if (ids.isEmpty()) {
                log.warn("[VectorStore] Documents found but no valid IDs to delete");
                return;
            }

            // 4. 批量删除
            log.info("[VectorStore] Deleting {} documents by metadata filter: {}", ids.size(), filter);
            knowledgeStore.delete(ids);

            log.debug("[VectorStore] Successfully deleted {} documents", ids.size());

        } catch (Exception e) {
            log.error("[VectorStore] Failed to delete by metadata: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete documents by metadata", e);
        }
    }

    /**
     * 构建 Milvus Filter 表达式
     * 
     * @param filter Metadata 过滤条件 (如: {"documentId": "doc_123"})
     * @return Milvus 过滤表达式 (如: "documentId == 'doc_123'")
     */
    private String buildFilterExpression(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {
            return "";
        }

        return filter.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        return key + " == '" + value + "'";
                    } else if (value instanceof Number) {
                        return key + " == " + value;
                    } else {
                        return key + " == '" + value.toString() + "'";
                    }
                })
                .collect(Collectors.joining(" and "));
    }
}
