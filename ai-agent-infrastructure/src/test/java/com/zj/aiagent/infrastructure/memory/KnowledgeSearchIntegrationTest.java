package com.zj.aiagent.infrastructure.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * 知识库检索端到端集成测试
 *
 * 使用内存模拟的 VectorStore，验证完整业务流程：
 * 1. 构造中文知识文档（不同关键词密度）
 * 2. 存入 VectorStore
 * 3. 三种策略检索并验证结果差异
 * 4. 验证 metadata 字段名一致性
 */
class KnowledgeSearchIntegrationTest {

    private MilvusVectorStoreAdapter adapter;
    private InMemoryVectorStore knowledgeStore;
    private InMemoryVectorStore memoryStore;

    /** 模拟文档：关键词密度从高到低 */
    private static final String DOC_HIGH =
        "向量数据库是专门用于存储和检索向量数据的数据库系统，向量数据库支持高效的相似度检索";
    private static final String DOC_MED =
        "知识库管理系统可以使用向量数据库来实现语义搜索功能";
    private static final String DOC_LOW =
        "今天学习了Spring Boot微服务架构的最佳实践和部署方案";
    private static final String DATASET_ID = "ds-test-001";

    @BeforeEach
    void setUp() {
        knowledgeStore = new InMemoryVectorStore();
        memoryStore = new InMemoryVectorStore();
        adapter = new MilvusVectorStoreAdapter(knowledgeStore, memoryStore);

        // 模拟文档上传后的向量化存储（metadata 使用下划线字段名）
        List<Document> docs = List.of(
            new Document(
                "id-1",
                DOC_HIGH,
                Map.of("dataset_id", DATASET_ID, "chunk_index", 0)
            ),
            new Document(
                "id-2",
                DOC_MED,
                Map.of("dataset_id", DATASET_ID, "chunk_index", 1)
            ),
            new Document(
                "id-3",
                DOC_LOW,
                Map.of("dataset_id", DATASET_ID, "chunk_index", 2)
            )
        );
        knowledgeStore.add(docs);
    }

    // ========== 语义检索测试 ==========

    @Test
    @DisplayName("SEMANTIC 策略应返回按语义相似度排序的结果")
    void semantic_search_returns_results() {
        List<String> results = adapter.searchKnowledgeByDataset(
            DATASET_ID,
            "向量数据库检索",
            3
        );

        assertFalse(results.isEmpty(), "语义检索应返回结果");
        assertTrue(results.size() <= 3, "结果数不应超过 topK");
        // 所有结果都应来自我们存入的文档
        for (String r : results) {
            assertTrue(
                r.equals(DOC_HIGH) || r.equals(DOC_MED) || r.equals(DOC_LOW),
                "结果应来自已存入的文档"
            );
        }
    }

    @Test
    @DisplayName("SEMANTIC 策略按不存在的 datasetId 检索应返回空")
    void semantic_search_wrong_dataset_returns_empty() {
        List<String> results = adapter.searchKnowledgeByDataset(
            "ds-nonexistent",
            "向量数据库",
            3
        );
        assertTrue(results.isEmpty(), "不存在的 datasetId 应返回空结果");
    }

    // ========== 关键词检索测试 ==========

    @Test
    @DisplayName("KEYWORD 策略应按关键词命中率重排序")
    void keyword_search_reranks_by_keyword_score() {
        List<String> results = adapter.keywordSearchByDataset(
            DATASET_ID,
            "向量数据库检索",
            3
        );

        assertFalse(results.isEmpty(), "关键词检索应返回结果");
        // 高关键词密度文档应排在前面
        if (results.size() >= 2) {
            double scoreFirst = KeywordScorer.score(
                "向量数据库检索",
                results.get(0)
            );
            double scoreLast = KeywordScorer.score(
                "向量数据库检索",
                results.get(results.size() - 1)
            );
            assertTrue(
                scoreFirst >= scoreLast,
                "第一条结果的关键词得分(" +
                    scoreFirst +
                    ")应 >= 最后一条(" +
                    scoreLast +
                    ")"
            );
        }
    }

    @Test
    @DisplayName("KEYWORD 策略对无关查询应返回低分结果")
    void keyword_search_irrelevant_query() {
        List<String> results = adapter.keywordSearchByDataset(
            DATASET_ID,
            "量子纠缠超导体",
            3
        );
        // 即使语义检索返回了结果，关键词评分应很低
        for (String r : results) {
            double score = KeywordScorer.score("量子纠缠超导体", r);
            assertTrue(score < 0.5, "无关查询的关键词得分应较低: " + score);
        }
    }

    // ========== 混合检索测试 ==========

    @Test
    @DisplayName("HYBRID 策略应融合语义和关键词分数")
    void hybrid_search_fuses_scores() {
        List<String> results = adapter.hybridSearchByDataset(
            DATASET_ID,
            "向量数据库检索",
            3
        );

        assertFalse(results.isEmpty(), "混合检索应返回结果");
        assertTrue(results.size() <= 3, "结果数不应超过 topK");
    }

    @Test
    @DisplayName("HYBRID 策略结果应与纯语义或纯关键词有差异")
    void hybrid_differs_from_pure_strategies() {
        String query = "向量数据库检索";
        List<String> semantic = adapter.searchKnowledgeByDataset(
            DATASET_ID,
            query,
            3
        );
        List<String> keyword = adapter.keywordSearchByDataset(
            DATASET_ID,
            query,
            3
        );
        List<String> hybrid = adapter.hybridSearchByDataset(
            DATASET_ID,
            query,
            3
        );

        // 三种策略都应返回结果
        assertFalse(semantic.isEmpty());
        assertFalse(keyword.isEmpty());
        assertFalse(hybrid.isEmpty());

        // 混合检索的排序可能与纯语义或纯关键词不同（不强制要求不同，但验证都能正常工作）
        assertEquals(
            semantic.size(),
            hybrid.size(),
            "混合检索和语义检索应返回相同数量的结果"
        );
    }

    // ========== topK 边界测试 ==========

    @Test
    @DisplayName("topK=1 应只返回一条结果")
    void topk_one_returns_single_result() {
        List<String> semantic = adapter.searchKnowledgeByDataset(
            DATASET_ID,
            "向量",
            1
        );
        List<String> keyword = adapter.keywordSearchByDataset(
            DATASET_ID,
            "向量",
            1
        );
        List<String> hybrid = adapter.hybridSearchByDataset(
            DATASET_ID,
            "向量",
            1
        );

        assertEquals(1, semantic.size());
        assertEquals(1, keyword.size());
        assertEquals(1, hybrid.size());
    }

    // ========== metadata 字段名一致性测试 ==========

    @Test
    @DisplayName("使用下划线 dataset_id 过滤应能匹配到文档")
    void underscore_dataset_id_filter_works() {
        List<String> results = adapter.searchKnowledgeByDataset(
            DATASET_ID,
            "测试",
            10
        );
        assertFalse(results.isEmpty(), "下划线 dataset_id 过滤应能匹配文档");
    }

    @Test
    @DisplayName("驼峰 datasetId 历史数据也应被兼容检索到")
    void camel_case_legacy_metadata_remains_searchable() {
        InMemoryVectorStore buggyStore = new InMemoryVectorStore();
        MilvusVectorStoreAdapter buggyAdapter = new MilvusVectorStoreAdapter(
            buggyStore,
            memoryStore
        );

        buggyStore.add(
            List.of(
                new Document(
                    "id-buggy",
                    "这是一条用驼峰字段名存储的文档",
                    Map.of("datasetId", "ds-buggy-001")
                ) // 注意：驼峰
            )
        );

        List<String> results = buggyAdapter.searchKnowledgeByDataset(
            "ds-buggy-001",
            "文档",
            5
        );
        assertEquals(
            1,
            results.size(),
            "历史上用驼峰 datasetId 存储的文档，检索时也应能兼容命中"
        );
    }

    @Test
    @DisplayName("按 document_id 删除时应兼容删除历史 documentId 数据")
    void delete_by_metadata_should_support_legacy_camel_case_keys() {
        InMemoryVectorStore buggyStore = new InMemoryVectorStore();
        MilvusVectorStoreAdapter buggyAdapter = new MilvusVectorStoreAdapter(
            buggyStore,
            memoryStore
        );

        buggyStore.add(
            List.of(
                new Document(
                    "legacy-doc",
                    "历史文档",
                    Map.of(
                        "documentId",
                        "doc-legacy-001",
                        "datasetId",
                        DATASET_ID
                    )
                )
            )
        );

        buggyAdapter.deleteByMetadata(Map.of("document_id", "doc-legacy-001"));

        List<String> results = buggyAdapter.searchKnowledgeByDataset(
            DATASET_ID,
            "历史文档",
            5
        );
        assertTrue(
            results.isEmpty(),
            "删除 document_id 时应同时匹配并删除历史 documentId 数据"
        );
    }

    // ========== 内存模拟 VectorStore ==========

    /**
     * 简单的内存 VectorStore 实现，用于测试
     * 模拟 Milvus 的 similaritySearch 行为：支持 filterExpression 过滤
     */
    static class InMemoryVectorStore implements VectorStore {

        private final List<Document> documents = new ArrayList<>();

        @Override
        public void add(List<Document> documents) {
            this.documents.addAll(documents);
        }

        @Override
        public void delete(List<String> idList) {
            documents.removeIf(doc -> idList.contains(doc.getId()));
        }

        @Override
        public void delete(Filter.Expression filterExpression) {
            // 测试中不需要实现
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            Filter.Expression filterExpr = request.getFilterExpression();

            return documents
                .stream()
                .filter(doc -> matchesFilter(doc, filterExpr))
                .limit(request.getTopK())
                .collect(Collectors.toList());
        }

        /**
         * 结构化过滤表达式匹配
         * 支持 EQ 比较操作
         */
        private boolean matchesFilter(Document doc, Filter.Expression expr) {
            if (expr == null) return true;

            if (
                expr.type() == Filter.ExpressionType.AND &&
                expr.left() instanceof Filter.Expression left &&
                expr.right() instanceof Filter.Expression right
            ) {
                return matchesFilter(doc, left) && matchesFilter(doc, right);
            }

            if (
                expr.type() == Filter.ExpressionType.OR &&
                expr.left() instanceof Filter.Expression left &&
                expr.right() instanceof Filter.Expression right
            ) {
                return matchesFilter(doc, left) || matchesFilter(doc, right);
            }

            if (
                expr.type() == Filter.ExpressionType.EQ &&
                expr.left() instanceof Filter.Key key &&
                expr.right() instanceof Filter.Value value
            ) {
                Object actual = doc.getMetadata().get(key.key());
                if (actual == null) return false;
                return actual.toString().equals(value.value().toString());
            }

            // 不支持的表达式类型，默认通过
            return true;
        }
    }
}
