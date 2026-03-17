package com.zj.aiagent.infrastructure.workflow.executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zj.aiagent.domain.knowledge.service.KnowledgeRetrievalService;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KnowledgeNodeExecutorStrategy 单元测试
 * 验证 null 穿透修复、strategy 传递、边界情况
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeNodeExecutorStrategyTest {

    @Mock
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @InjectMocks
    private KnowledgeNodeExecutorStrategy strategy;

    private Node buildNode(String datasetId, String searchStrategy, Long topK) {
        Map<String, Object> props = new HashMap<>();
        if (datasetId != null) props.put("knowledge_dataset_id", datasetId);
        if (searchStrategy != null) props.put(
            "search_strategy",
            searchStrategy
        );
        if (topK != null) props.put("knowledge_top_k", topK);
        NodeConfig config = NodeConfig.builder().properties(props).build();
        return Node.builder()
            .nodeId("node-1")
            .name("知识库节点")
            .type(NodeType.KNOWLEDGE)
            .config(config)
            .build();
    }

    // ========== null 穿透修复验证 ==========

    @Test
    @DisplayName("query 为 null 时应返回失败结果，不应产生 'null' 字符串")
    void null_query_should_fail() throws Exception {
        Node node = buildNode("ds-001", "SEMANTIC", 5L);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", null); // 关键：值为 null

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertFalse(result.isSuccess(), "query 为 null 时应返回失败");
        verify(knowledgeRetrievalService, never()).retrieveByDataset(
            any(),
            any(),
            anyInt(),
            any()
        );
    }

    @Test
    @DisplayName("query 不存在时应尝试 user_input")
    void missing_query_falls_back_to_user_input() throws Exception {
        Node node = buildNode("ds-001", "SEMANTIC", 5L);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("user_input", "什么是向量数据库");

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "什么是向量数据库",
                5,
                "SEMANTIC"
            )
        ).thenReturn(List.of("结果1"));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        verify(knowledgeRetrievalService).retrieveByDataset(
            "ds-001",
            "什么是向量数据库",
            5,
            "SEMANTIC"
        );
    }

    @Test
    @DisplayName("query 不存在时应兼容任意非系统字符串输入字段")
    void missing_query_falls_back_to_first_non_system_string_input()
        throws Exception {
        Node node = buildNode("ds-001", "SEMANTIC", 5L);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("field_123", "用户真实问题");
        inputs.put("__agentId__", 1L);

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "用户真实问题",
                5,
                "SEMANTIC"
            )
        ).thenReturn(List.of("结果1"));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        verify(knowledgeRetrievalService).retrieveByDataset(
            "ds-001",
            "用户真实问题",
            5,
            "SEMANTIC"
        );
    }

    @Test
    @DisplayName(
        "resolvedInputs 无有效查询词时应回退到执行上下文全局 inputs.query"
    )
    void missing_query_falls_back_to_execution_context_inputs()
        throws Exception {
        Node node = buildNode("ds-001", "SEMANTIC", 5L);
        ExecutionContext context = ExecutionContext.builder()
            .inputs(Map.of("query", "上下文里的问题"))
            .build();
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("field_123", "");
        inputs.put("__context__", context);

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "上下文里的问题",
                5,
                "SEMANTIC"
            )
        ).thenReturn(List.of("结果1"));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        verify(knowledgeRetrievalService).retrieveByDataset(
            "ds-001",
            "上下文里的问题",
            5,
            "SEMANTIC"
        );
    }

    @Test
    @DisplayName("query 和 user_input 都为 null 时应返回失败")
    void both_null_should_fail() throws Exception {
        Node node = buildNode("ds-001", "SEMANTIC", 5L);
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("query", null);
        inputs.put("user_input", null);

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertFalse(result.isSuccess());
        verify(knowledgeRetrievalService, never()).retrieveByDataset(
            any(),
            any(),
            anyInt(),
            any()
        );
    }

    // ========== strategy 传递验证 ==========

    @Test
    @DisplayName("strategy 应正确传递给 retrieveByDataset")
    void strategy_passed_correctly() throws Exception {
        Node node = buildNode("ds-001", "KEYWORD", 3L);
        Map<String, Object> inputs = Map.of("query", "机器学习");

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "机器学习",
                3,
                "KEYWORD"
            )
        ).thenReturn(List.of("结果1"));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        verify(knowledgeRetrievalService).retrieveByDataset(
            "ds-001",
            "机器学习",
            3,
            "KEYWORD"
        );
    }

    @Test
    @DisplayName("strategy 为空时应默认 SEMANTIC")
    void empty_strategy_defaults_to_semantic() throws Exception {
        Node node = buildNode("ds-001", null, 5L);
        Map<String, Object> inputs = Map.of("query", "测试查询");

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "测试查询",
                5,
                "SEMANTIC"
            )
        ).thenReturn(List.of("结果1"));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        verify(knowledgeRetrievalService).retrieveByDataset(
            "ds-001",
            "测试查询",
            5,
            "SEMANTIC"
        );
    }

    // ========== 边界情况 ==========

    @Test
    @DisplayName("datasetId 未配置时应返回失败")
    void missing_dataset_id_should_fail() throws Exception {
        Node node = buildNode(null, "SEMANTIC", 5L);
        Map<String, Object> inputs = Map.of("query", "测试");

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertFalse(result.isSuccess());
        verify(knowledgeRetrievalService, never()).retrieveByDataset(
            any(),
            any(),
            anyInt(),
            any()
        );
    }

    @Test
    @DisplayName("topK 未配置时应默认为 5")
    void missing_topk_defaults_to_5() throws Exception {
        Node node = buildNode("ds-001", "SEMANTIC", null);
        Map<String, Object> inputs = Map.of("query", "测试查询");

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "测试查询",
                5,
                "SEMANTIC"
            )
        ).thenReturn(List.of("结果1"));

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        verify(knowledgeRetrievalService).retrieveByDataset(
            "ds-001",
            "测试查询",
            5,
            "SEMANTIC"
        );
    }

    @Test
    @DisplayName("正常检索应返回 success 并包含 knowledge_list")
    void normal_retrieval_returns_success() throws Exception {
        Node node = buildNode("ds-001", "HYBRID", 3L);
        Map<String, Object> inputs = Map.of("query", "向量数据库");
        List<String> expected = List.of("知识片段1", "知识片段2", "知识片段3");

        when(
            knowledgeRetrievalService.retrieveByDataset(
                "ds-001",
                "向量数据库",
                3,
                "HYBRID"
            )
        ).thenReturn(expected);

        CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(
            node,
            inputs,
            null
        );
        NodeExecutionResult result = future.get();

        assertTrue(result.isSuccess());
        @SuppressWarnings("unchecked")
        List<String> knowledgeList = (List<String>) result
            .getOutputs()
            .get("knowledge_list");
        assertEquals(expected, knowledgeList);
    }
}
