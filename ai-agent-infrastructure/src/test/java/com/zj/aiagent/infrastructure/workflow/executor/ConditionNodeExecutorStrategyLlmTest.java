package com.zj.aiagent.infrastructure.workflow.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.ConditionEvaluatorPort;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 单元测试: ConditionNodeExecutorStrategy LLM 模式
 *
 * 测试 LLM 语义路由模式的核心行为：
 * 1. prompt 包含 branch descriptions (Requirements 7.2)
 * 2. 重试逻辑：LLM 返回无效 ID → 重试 → 成功/失败 (Requirements 7.3)
 * 3. case-insensitive 匹配和 trim 空白 (Requirements 7.4)
 */
@ExtendWith(MockitoExtension.class)
class ConditionNodeExecutorStrategyLlmTest {

    @Mock
    private ConditionEvaluatorPort conditionEvaluator;

    @Mock
    private StreamPublisher streamPublisher;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ObjectMapper objectMapper;
    private ConditionNodeExecutorStrategy strategy;

    // 测试用分支数据
    private List<ConditionBranch> testBranches;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // 使用同步 Executor 简化测试
        Executor syncExecutor = Runnable::run;
        RestClient.Builder restClientBuilder = RestClient.builder();

        strategy = spy(new ConditionNodeExecutorStrategy(
                conditionEvaluator, objectMapper, syncExecutor, restClientBuilder));

        // 构建测试分支
        testBranches = List.of(
                ConditionBranch.builder()
                        .priority(0)
                        .targetNodeId("node_purchase")
                        .description("用户表达了购买意向")
                        .isDefault(false)
                        .conditionGroups(List.of())
                        .build(),
                ConditionBranch.builder()
                        .priority(1)
                        .targetNodeId("node_question")
                        .description("用户在咨询问题")
                        .isDefault(false)
                        .conditionGroups(List.of())
                        .build(),
                ConditionBranch.builder()
                        .priority(2)
                        .targetNodeId("node_default")
                        .description("默认分支")
                        .isDefault(true)
                        .conditionGroups(List.of())
                        .build()
        );
    }

    // ========== 辅助方法 ==========

    /**
     * 创建 LLM 模式的条件节点
     */
    private Node createLlmConditionNode(String nodeId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("routingStrategy", "LLM");
        properties.put("model", "gpt-4");
        properties.put("baseUrl", "https://api.openai.com");
        properties.put("apiKey", "test-key");
        properties.put("branches", testBranches);

        NodeConfig config = NodeConfig.builder()
                .properties(properties)
                .build();

        return Node.builder()
                .nodeId(nodeId)
                .name("Test Condition Node")
                .type(NodeType.CONDITION)
                .config(config)
                .build();
    }

    /**
     * 创建 resolvedInputs
     */
    private Map<String, Object> createResolvedInputs() {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("userMessage", "我想买一台笔记本电脑");
        inputs.put("intent", "purchase");
        inputs.put("__outgoingEdges__", List.of()); // 内部字段，不应出现在 prompt 中
        return inputs;
    }

    /**
     * 设置 ChatClient mock 链式调用，返回指定响应
     */
    private void mockChatClientResponse(String response) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(response);
    }

    /**
     * 设置 ChatClient mock 链式调用，按顺序返回多个响应
     */
    private void mockChatClientResponses(String firstResponse, String secondResponse) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content())
                .thenReturn(firstResponse)
                .thenReturn(secondResponse);
    }

    // ========== 测试: prompt 包含 branch descriptions ==========

    @Test
    @DisplayName("should_include_branch_descriptions_in_prompt_when_llm_mode")
    void should_include_branch_descriptions_in_prompt_when_llm_mode() {
        // Given: 带有 description 的分支列表和上下文输入
        Map<String, Object> resolvedInputs = createResolvedInputs();

        // When: 构建 LLM prompt
        String prompt = strategy.buildLlmPrompt(testBranches, resolvedInputs);

        // Then: prompt 应包含每个非 default 分支的 description
        assertTrue(prompt.contains("用户表达了购买意向"),
                "Prompt should contain first branch description");
        assertTrue(prompt.contains("用户在咨询问题"),
                "Prompt should contain second branch description");

        // prompt 应包含目标节点 ID
        assertTrue(prompt.contains("node_purchase"),
                "Prompt should contain first branch target ID");
        assertTrue(prompt.contains("node_question"),
                "Prompt should contain second branch target ID");

        // prompt 不应包含 default 分支的描述作为选项
        // default 分支的 targetNodeId 不应出现在 "Available options" 部分
        String optionsSection = prompt.substring(prompt.indexOf("Available options:"));
        assertFalse(optionsSection.contains("node_default"),
                "Prompt should not include default branch as an option");

        // prompt 应包含上下文变量（排除 __ 前缀的内部字段）
        assertTrue(prompt.contains("userMessage"),
                "Prompt should contain context variable 'userMessage'");
        assertTrue(prompt.contains("intent"),
                "Prompt should contain context variable 'intent'");
        assertFalse(prompt.contains("__outgoingEdges__"),
                "Prompt should not contain internal fields starting with __");
    }

    // ========== 测试: 重试逻辑 ==========

    @Test
    @DisplayName("should_retry_with_clarification_when_first_llm_response_invalid")
    void should_retry_with_clarification_when_first_llm_response_invalid() throws Exception {
        // Given: LLM 第一次返回无效 ID，第二次返回有效 ID
        Node node = createLlmConditionNode("cond_1");
        Map<String, Object> resolvedInputs = createResolvedInputs();

        // mock buildChatClient 返回我们的 mock chatClient
        doReturn(chatClient).when(strategy).buildChatClient(any(NodeConfig.class));

        // 第一次返回无效 ID，第二次返回有效 ID
        mockChatClientResponses("invalid_node_id", "node_purchase");

        // When: 执行条件节点
        NodeExecutionResult result = strategy.executeAsync(node, resolvedInputs, streamPublisher).get();

        // Then: 应成功路由到 node_purchase（通过重试）
        assertTrue(result.isSuccess(), "Result should be successful");
        assertEquals("node_purchase", result.getSelectedBranchId(),
                "Should route to node_purchase after retry");

        // 验证 LLM 被调用了 2 次（初始 + 重试）
        verify(chatClient, times(2)).prompt();

        // 验证第二次调用使用了澄清 prompt
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec, times(2)).user(promptCaptor.capture());
        List<String> prompts = promptCaptor.getAllValues();

        // 第二个 prompt 应该是澄清 prompt，包含 "valid target ID" 相关内容
        String clarificationPrompt = prompts.get(1);
        assertTrue(clarificationPrompt.contains("not a valid target ID"),
                "Clarification prompt should mention invalid target ID");
        assertTrue(clarificationPrompt.contains("node_purchase"),
                "Clarification prompt should list valid target IDs");
        assertTrue(clarificationPrompt.contains("node_question"),
                "Clarification prompt should list all valid target IDs");
    }

    @Test
    @DisplayName("should_fallback_to_default_branch_when_both_attempts_fail")
    void should_fallback_to_default_branch_when_both_attempts_fail() throws Exception {
        // Given: LLM 两次都返回无效 ID
        Node node = createLlmConditionNode("cond_2");
        Map<String, Object> resolvedInputs = createResolvedInputs();

        doReturn(chatClient).when(strategy).buildChatClient(any(NodeConfig.class));

        // 两次都返回无效 ID
        mockChatClientResponses("completely_wrong_id", "still_wrong_id");

        // When: 执行条件节点
        NodeExecutionResult result = strategy.executeAsync(node, resolvedInputs, streamPublisher).get();

        // Then: 应 fallback 到 default 分支
        assertTrue(result.isSuccess(), "Result should be successful (fallback to default)");
        assertEquals("node_default", result.getSelectedBranchId(),
                "Should fallback to default branch when both LLM attempts fail");

        // 验证 LLM 被调用了 2 次
        verify(chatClient, times(2)).prompt();
    }

    // ========== 测试: case-insensitive 匹配 ==========

    @Test
    @DisplayName("should_match_target_id_case_insensitively")
    void should_match_target_id_case_insensitively() throws Exception {
        // Given: LLM 返回大写变体的目标 ID
        Node node = createLlmConditionNode("cond_3");
        Map<String, Object> resolvedInputs = createResolvedInputs();

        doReturn(chatClient).when(strategy).buildChatClient(any(NodeConfig.class));

        // LLM 返回大写版本
        mockChatClientResponse("NODE_PURCHASE");

        // When: 执行条件节点
        NodeExecutionResult result = strategy.executeAsync(node, resolvedInputs, streamPublisher).get();

        // Then: 应成功匹配（case-insensitive）
        assertTrue(result.isSuccess(), "Result should be successful");
        assertEquals("node_purchase", result.getSelectedBranchId(),
                "Should match target ID case-insensitively and return original case");

        // 验证 LLM 只被调用了 1 次（无需重试）
        verify(chatClient, times(1)).prompt();
    }

    // ========== 测试: trim 空白 ==========

    @Test
    @DisplayName("should_trim_whitespace_from_llm_response")
    void should_trim_whitespace_from_llm_response() throws Exception {
        // Given: LLM 返回带空白的响应
        Node node = createLlmConditionNode("cond_4");
        Map<String, Object> resolvedInputs = createResolvedInputs();

        doReturn(chatClient).when(strategy).buildChatClient(any(NodeConfig.class));

        // LLM 返回带前后空白和换行的响应
        mockChatClientResponse("  \t node_question \n ");

        // When: 执行条件节点
        NodeExecutionResult result = strategy.executeAsync(node, resolvedInputs, streamPublisher).get();

        // Then: 应成功匹配（trim 后匹配）
        assertTrue(result.isSuccess(), "Result should be successful");
        assertEquals("node_question", result.getSelectedBranchId(),
                "Should trim whitespace from LLM response and match correctly");

        // 验证 LLM 只被调用了 1 次（无需重试）
        verify(chatClient, times(1)).prompt();
    }
}
