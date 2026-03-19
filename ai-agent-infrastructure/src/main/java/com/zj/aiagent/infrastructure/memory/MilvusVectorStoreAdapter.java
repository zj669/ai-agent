package com.zj.aiagent.infrastructure.memory;

import com.zj.aiagent.domain.memory.port.VectorStore;
import com.zj.aiagent.domain.memory.valobj.Document;
import com.zj.aiagent.domain.memory.valobj.SearchRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 基于 Milvus 的 VectorStore 适配器
 *
 * 将领域层 VectorStore 端口映射到 Spring AI 的 Milvus VectorStore
 * 负责 domain 层值对象与 Spring AI 类型之间的转换
 *
 * 功能：
 * 1. 知识库检索 (agent_knowledge_base)
 * 2. 长期记忆存储/检索 (agent_chat_memory)
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "milvus",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class MilvusVectorStoreAdapter implements VectorStore {

    private final org.springframework.ai.vectorstore.VectorStore knowledgeStore;
    private final org.springframework.ai.vectorstore.VectorStore memoryStore;

    public MilvusVectorStoreAdapter(
        @Qualifier(
            "knowledgeVectorStore"
        ) org.springframework.ai.vectorstore.VectorStore knowledgeStore,
        @Qualifier(
            "memoryVectorStore"
        ) org.springframework.ai.vectorstore.VectorStore memoryStore
    ) {
        this.knowledgeStore = knowledgeStore;
        this.memoryStore = memoryStore;
        log.info(
            "[MilvusVectorStoreAdapter] Initialized with knowledge and memory stores"
        );
    }

    /**
     * 搜索长期记忆（LTM）
     * 从 agent_chat_memory 集合检索
     */
    @Override
    public List<String> search(String query, Long agentId, int topK) {
        log.debug(
            "[VectorStore] Searching memory for query: '{}', agentId: {}, topK: {}",
            query.length() > 50 ? query.substring(0, 50) + "..." : query,
            agentId,
            topK
        );

        try {
            // 构建搜索请求，使用 agentId 过滤
            org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("agent_id == " + agentId)
                    .build();

            List<org.springframework.ai.document.Document> results =
                memoryStore.similaritySearch(request);

            log.debug("[VectorStore] Found {} results", results.size());

            return results
                .stream()
                .map(org.springframework.ai.document.Document::getText)
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
        log.debug(
            "[VectorStore] Searching knowledge for query: '{}', agentId: {}, topK: {}",
            query.length() > 50 ? query.substring(0, 50) + "..." : query,
            agentId,
            topK
        );

        try {
            org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression("agent_id == " + agentId)
                    .build();

            List<org.springframework.ai.document.Document> results =
                knowledgeStore.similaritySearch(request);

            return results
                .stream()
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(
                "[VectorStore] Knowledge search failed: {}",
                e.getMessage()
            );
            return List.of();
        }
    }

    /**
     * 存储到长期记忆
     */
    @Override
    public void store(
        Long agentId,
        String content,
        Map<String, Object> metadata
    ) {
        log.debug(
            "[VectorStore] Storing content to memory, agentId: {}, length: {}",
            agentId,
            content.length()
        );

        try {
            // 添加 agent_id 到元数据
            Map<String, Object> enrichedMetadata = new java.util.HashMap<>(
                metadata
            );
            enrichedMetadata.put("agent_id", agentId);
            enrichedMetadata.put("timestamp", System.currentTimeMillis());

            org.springframework.ai.document.Document document =
                new org.springframework.ai.document.Document(
                    content,
                    enrichedMetadata
                );
            memoryStore.add(List.of(document));

            log.debug("[VectorStore] Content stored successfully");
        } catch (Exception e) {
            log.error(
                "[VectorStore] Failed to store content: {}",
                e.getMessage(),
                e
            );
        }
    }

    /**
     * 批量存储到长期记忆
     */
    @Override
    public void storeBatch(Long agentId, List<String> contents) {
        log.debug(
            "[VectorStore] Batch storing {} items to memory for agentId: {}",
            contents.size(),
            agentId
        );

        try {
            List<org.springframework.ai.document.Document> documents = contents
                .stream()
                .map(content ->
                    new org.springframework.ai.document.Document(
                        content,
                        Map.of(
                            "agent_id",
                            agentId,
                            "timestamp",
                            System.currentTimeMillis()
                        )
                    )
                )
                .collect(Collectors.toList());

            memoryStore.add(documents);

            log.debug(
                "[VectorStore] Batch stored {} items successfully",
                contents.size()
            );
        } catch (Exception e) {
            log.error(
                "[VectorStore] Failed to batch store: {}",
                e.getMessage(),
                e
            );
        }
    }

    /**
     * 存储到知识库
     */
    public void storeKnowledge(
        Long agentId,
        String content,
        Map<String, Object> metadata
    ) {
        log.debug(
            "[VectorStore] Storing content to knowledge, agentId: {}, length: {}",
            agentId,
            content.length()
        );

        try {
            Map<String, Object> enrichedMetadata = new java.util.HashMap<>(
                metadata
            );
            enrichedMetadata.put("agent_id", agentId);

            org.springframework.ai.document.Document document =
                new org.springframework.ai.document.Document(
                    content,
                    enrichedMetadata
                );
            knowledgeStore.add(List.of(document));
        } catch (Exception e) {
            log.error(
                "[VectorStore] Failed to store knowledge: {}",
                e.getMessage(),
                e
            );
        }
    }

    // ==================== 知识库新增方法实现 ====================

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        log.debug(
            "[VectorStore] Similarity search with request: query='{}', topK={}",
            request.getQuery() != null
                ? request
                      .getQuery()
                      .substring(0, Math.min(50, request.getQuery().length())) +
                  "..."
                : "null",
            request.getTopK()
        );

        try {
            // 转换为 Spring AI 的 SearchRequest
            org.springframework.ai.vectorstore.SearchRequest springAiRequest =
                toSpringAiSearchRequest(request);

            // 使用 knowledgeStore 执行检索
            List<org.springframework.ai.document.Document> springAiResults =
                knowledgeStore.similaritySearch(springAiRequest);

            // 转换为 domain 层的 Document
            return springAiResults
                .stream()
                .map(this::toDomainDocument)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(
                "[VectorStore] Similarity search failed: {}",
                e.getMessage()
            );
            return List.of();
        }
    }

    @Override
    public List<String> searchKnowledgeByDataset(
        String datasetId,
        String query,
        int topK
    ) {
        log.debug(
            "[VectorStore] Searching knowledge by dataset: datasetId={}, query='{}', topK={}",
            datasetId,
            query.substring(0, Math.min(50, query.length())),
            topK
        );

        try {
            // 构建 Filter: datasetId == xxx
            org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .filterExpression(
                        buildMetadataCondition("dataset_id", datasetId)
                    )
                    .build();

            List<org.springframework.ai.document.Document> results =
                knowledgeStore.similaritySearch(request);

            return results
                .stream()
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(
                "[VectorStore] Search by dataset failed: {}",
                e.getMessage()
            );
            return List.of();
        }
    }

    @Override
    public List<String> keywordSearchByDataset(
        String datasetId,
        String query,
        int topK
    ) {
        log.debug(
            "[VectorStore] Keyword search by dataset: datasetId={}, query='{}', topK={}",
            datasetId,
            query.substring(0, Math.min(50, query.length())),
            topK
        );

        try {
            // 先用语义检索获取较大候选集
            int candidateSize = topK * 3;
            org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(query)
                    .topK(candidateSize)
                    .filterExpression(
                        buildMetadataCondition("dataset_id", datasetId)
                    )
                    .build();

            List<org.springframework.ai.document.Document> candidates =
                knowledgeStore.similaritySearch(request);

            // 使用 KeywordScorer 对候选集做关键词评分，按关键词相关性重排序
            return candidates
                .stream()
                .sorted((a, b) ->
                    Double.compare(
                        KeywordScorer.score(query, b.getText()),
                        KeywordScorer.score(query, a.getText())
                    )
                )
                .limit(topK)
                .map(org.springframework.ai.document.Document::getText)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(
                "[VectorStore] Keyword search by dataset failed, falling back to semantic: {}",
                e.getMessage()
            );
            return searchKnowledgeByDataset(datasetId, query, topK);
        }
    }

    @Override
    public List<String> hybridSearchByDataset(
        String datasetId,
        String query,
        int topK
    ) {
        log.debug(
            "[VectorStore] Hybrid search by dataset: datasetId={}, query='{}', topK={}",
            datasetId,
            query.substring(0, Math.min(50, query.length())),
            topK
        );

        try {
            // 语义检索获取候选集
            int candidateSize = topK * 3;
            org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query(query)
                    .topK(candidateSize)
                    .filterExpression(
                        buildMetadataCondition("dataset_id", datasetId)
                    )
                    .build();

            List<org.springframework.ai.document.Document> candidates =
                knowledgeStore.similaritySearch(request);

            if (candidates.isEmpty()) {
                return List.of();
            }

            // 计算语义分数（基于排序位置归一化：第1名=1.0，最后一名接近0）
            int total = candidates.size();

            // 加权融合：语义 0.7 + 关键词 0.3
            final double semanticWeight = 0.7;
            final double keywordWeight = 0.3;

            List<
                Map.Entry<org.springframework.ai.document.Document, Double>
            > scored = new java.util.ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                org.springframework.ai.document.Document doc = candidates.get(
                    i
                );
                double semanticScore = 1.0 - ((double) i / total);
                double keywordScore = KeywordScorer.score(query, doc.getText());
                double hybridScore =
                    semanticWeight * semanticScore +
                    keywordWeight * keywordScore;
                scored.add(Map.entry(doc, hybridScore));
            }

            // 按融合分数重排序
            return scored
                .stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(entry -> entry.getKey().getText())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn(
                "[VectorStore] Hybrid search by dataset failed, falling back to semantic: {}",
                e.getMessage()
            );
            return searchKnowledgeByDataset(datasetId, query, topK);
        }
    }

    @Override
    public void addDocuments(List<Document> documents) {
        log.debug(
            "[VectorStore] Adding {} documents to knowledge store",
            documents.size()
        );

        try {
            // 转换为 Spring AI 的 Document
            List<org.springframework.ai.document.Document> springAiDocuments =
                documents
                    .stream()
                    .map(this::toSpringAiDocument)
                    .collect(Collectors.toList());

            knowledgeStore.add(springAiDocuments);
            log.debug(
                "[VectorStore] Successfully added {} documents",
                documents.size()
            );
        } catch (Exception e) {
            log.error(
                "[VectorStore] Failed to add documents: {}",
                e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Failed to add documents to vector store",
                e
            );
        }
    }

    @Override
    public void deleteByMetadata(Map<String, Object> filter) {
        log.debug(
            "[VectorStore] Deleting documents by metadata filter: {}",
            filter
        );

        try {
            // 1. 构建 Filter 表达式
            String filterExpression = buildFilterExpression(filter);
            if (filterExpression.isEmpty()) {
                log.warn("[VectorStore] Empty filter, skipping delete");
                return;
            }

            // 2. 检索所有匹配的文档
            // 注意：使用一个通用查询词避免空查询问题
            org.springframework.ai.vectorstore.SearchRequest request =
                org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query("*") // 通配查询
                    .topK(1000) // 足够大的 topK 以获取所有匹配文档
                    .filterExpression(filterExpression)
                    .build();

            List<org.springframework.ai.document.Document> results =
                knowledgeStore.similaritySearch(request);

            if (results.isEmpty()) {
                log.debug(
                    "[VectorStore] No documents found matching filter: {}",
                    filter
                );
                return;
            }

            // 3. 提取 Document IDs
            List<String> ids = results
                .stream()
                .map(org.springframework.ai.document.Document::getId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

            if (ids.isEmpty()) {
                log.warn(
                    "[VectorStore] Documents found but no valid IDs to delete"
                );
                return;
            }

            // 4. 批量删除
            log.info(
                "[VectorStore] Deleting {} documents by metadata filter: {}",
                ids.size(),
                filter
            );
            knowledgeStore.delete(ids);

            log.debug(
                "[VectorStore] Successfully deleted {} documents",
                ids.size()
            );
        } catch (Exception e) {
            log.error(
                "[VectorStore] Failed to delete by metadata: {}",
                e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Failed to delete documents by metadata",
                e
            );
        }
    }

    // ==================== 类型转换方法 ====================

    /**
     * 将 domain 层的 Document 转换为 Spring AI 的 Document
     */
    private org.springframework.ai.document.Document toSpringAiDocument(
        Document domainDoc
    ) {
        return new org.springframework.ai.document.Document(
            domainDoc.getId(),
            domainDoc.getContent(),
            domainDoc.getMetadata()
        );
    }

    /**
     * 将 Spring AI 的 Document 转换为 domain 层的 Document
     */
    private Document toDomainDocument(
        org.springframework.ai.document.Document springAiDoc
    ) {
        return Document.builder()
            .id(springAiDoc.getId())
            .content(springAiDoc.getText())
            .metadata(springAiDoc.getMetadata())
            .build();
    }

    /**
     * 将 domain 层的 SearchRequest 转换为 Spring AI 的 SearchRequest
     */
    private org.springframework.ai.vectorstore.SearchRequest toSpringAiSearchRequest(
        SearchRequest domainRequest
    ) {
        org.springframework.ai.vectorstore.SearchRequest.Builder builder =
            org.springframework.ai.vectorstore.SearchRequest.builder()
                .query(domainRequest.getQuery())
                .topK(domainRequest.getTopK());

        if (domainRequest.getFilterExpression() != null) {
            builder.filterExpression(domainRequest.getFilterExpression());
        }

        if (domainRequest.getSimilarityThreshold() != null) {
            builder.similarityThreshold(domainRequest.getSimilarityThreshold());
        }

        return builder.build();
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

        return filter
            .entrySet()
            .stream()
            .map(entry ->
                buildMetadataCondition(entry.getKey(), entry.getValue())
            )
            .collect(Collectors.joining(" and "));
    }

    private String buildMetadataCondition(String key, Object value) {
        List<String> candidateKeys = resolveMetadataAliases(key);
        if (candidateKeys.size() == 1) {
            return buildSingleCondition(candidateKeys.get(0), value);
        }
        return candidateKeys
            .stream()
            .map(candidateKey -> buildSingleCondition(candidateKey, value))
            .collect(Collectors.joining(" or ", "(", ")"));
    }

    private List<String> resolveMetadataAliases(String key) {
        return switch (key) {
            case "dataset_id", "datasetId" -> List.of(
                "dataset_id",
                "datasetId"
            );
            case "agent_id", "agentId" -> List.of("agent_id", "agentId");
            case "document_id", "documentId" -> List.of(
                "document_id",
                "documentId"
            );
            case "chunk_index", "chunkIndex" -> List.of(
                "chunk_index",
                "chunkIndex"
            );
            default -> List.of(key);
        };
    }

    private String buildSingleCondition(String key, Object value) {
        if (value instanceof Number) {
            return key + " == " + value;
        }
        return key + " == '" + escapeFilterValue(value) + "'";
    }

    private String escapeFilterValue(Object value) {
        return value == null ? "" : value.toString().replace("'", "\\'");
    }
}
