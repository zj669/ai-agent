package com.zj.aiagent.application.knowledge;

import com.zj.aiagent.domain.memory.port.VectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KnowledgeRetrievalServiceImpl 策略分发单元测试
 * Mock VectorStore，验证不同 strategy 调用正确的下游方法
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeRetrievalServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private KnowledgeRetrievalServiceImpl service;

    private static final String DATASET_ID = "ds-001";
    private static final String QUERY = "什么是向量数据库";
    private static final int TOP_K = 5;
    private static final List<String> MOCK_RESULTS = List.of("结果1", "结果2", "结果3");

    // ========== 策略分发测试 ==========

    @Test
    @DisplayName("SEMANTIC 策略应调用 searchKnowledgeByDataset")
    void semantic_strategy_calls_searchKnowledgeByDataset() {
        when(vectorStore.searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "SEMANTIC");

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K);
        verify(vectorStore, never()).keywordSearchByDataset(any(), any(), anyInt());
        verify(vectorStore, never()).hybridSearchByDataset(any(), any(), anyInt());
    }

    @Test
    @DisplayName("KEYWORD 策略应调用 keywordSearchByDataset")
    void keyword_strategy_calls_keywordSearchByDataset() {
        when(vectorStore.keywordSearchByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "KEYWORD");

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).keywordSearchByDataset(DATASET_ID, QUERY, TOP_K);
        verify(vectorStore, never()).searchKnowledgeByDataset(any(), any(), anyInt());
    }

    @Test
    @DisplayName("HYBRID 策略应调用 hybridSearchByDataset")
    void hybrid_strategy_calls_hybridSearchByDataset() {
        when(vectorStore.hybridSearchByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "HYBRID");

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).hybridSearchByDataset(DATASET_ID, QUERY, TOP_K);
        verify(vectorStore, never()).searchKnowledgeByDataset(any(), any(), anyInt());
    }

    @Test
    @DisplayName("未知策略应走默认 SEMANTIC 路径")
    void unknown_strategy_falls_back_to_semantic() {
        when(vectorStore.searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "UNKNOWN_STRATEGY");

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K);
    }

    @Test
    @DisplayName("策略大小写不敏感")
    void strategy_case_insensitive() {
        when(vectorStore.keywordSearchByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "keyword");

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).keywordSearchByDataset(DATASET_ID, QUERY, TOP_K);
    }

    // ========== 回退与异常测试 ==========

    @Test
    @DisplayName("UnsupportedOperationException 应回退到语义检索")
    void unsupported_operation_falls_back_to_semantic() {
        when(vectorStore.keywordSearchByDataset(DATASET_ID, QUERY, TOP_K))
                .thenThrow(new UnsupportedOperationException("keywordSearchByDataset not implemented"));
        when(vectorStore.searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "KEYWORD");

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K);
    }

    @Test
    @DisplayName("运行时异常应返回空列表")
    void runtime_exception_returns_empty_list() {
        when(vectorStore.searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K))
                .thenThrow(new RuntimeException("Milvus connection failed"));

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K, "SEMANTIC");

        assertTrue(results.isEmpty());
    }

    // ========== 默认重载方法测试 ==========

    @Test
    @DisplayName("3 参数 retrieveByDataset 应默认使用 SEMANTIC 策略")
    void three_param_overload_defaults_to_semantic() {
        when(vectorStore.searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K)).thenReturn(MOCK_RESULTS);

        List<String> results = service.retrieveByDataset(DATASET_ID, QUERY, TOP_K);

        assertEquals(MOCK_RESULTS, results);
        verify(vectorStore).searchKnowledgeByDataset(DATASET_ID, QUERY, TOP_K);
    }
}