package com.zj.aiagent.infrastructure.workflow.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.ConditionEvaluatorPort;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ConditionBranch;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 条件路由节点执行策略
 * 根据结构化条件分支或 LLM 语义路由选择下游节点
 *
 * 设计说明：
 * - EXPRESSION 模式：从 NodeConfig.properties["branches"] 解析 ConditionBranch 列表，
 *   通过 ConditionEvaluatorPort 进行结构化条件评估，替代直接使用 SpEL
 * - LLM 模式：从 NodeConfig.properties["branches"] 解析分支列表，使用 branch.description
 *   构建 Prompt 让 LLM 选择目标分支；支持重试逻辑和 default 分支兜底
 * - 向后兼容：如果 branches 配置为空，从 __outgoingEdges__ 做旧模型兼容转换
 */
@Slf4j
@Component
public class ConditionNodeExecutorStrategy implements NodeExecutorStrategy {

    private final ConditionEvaluatorPort conditionEvaluator;
    private final ObjectMapper objectMapper;
    private final Executor executor;
    private final RestClient.Builder restClientBuilder;

    public ConditionNodeExecutorStrategy(
            ConditionEvaluatorPort conditionEvaluator,
            ObjectMapper objectMapper,
            @Qualifier("nodeExecutorThreadPool") Executor executor,
            @Qualifier("restClientBuilder1") RestClient.Builder restClientBuilder) {
        this.conditionEvaluator = conditionEvaluator;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                NodeConfig config = node.getConfig();
                String routingStrategy = config.getString("routingStrategy", "EXPRESSION");

                if ("EXPRESSION".equalsIgnoreCase(routingStrategy)) {
                    return evaluateByStructuredCondition(node, config, resolvedInputs);
                } else {
                    return evaluateByLlmMode(node, config, resolvedInputs);
                }

            } catch (Exception e) {
                log.error("[Condition Node {}] Execution failed: {}", node.getNodeId(), e.getMessage(), e);
                streamPublisher.publishError(e.getMessage());
                return NodeExecutionResult.failed(e.getMessage());
            }
        }, executor);
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.CONDITION;
    }

    // ========== EXPRESSION 模式：结构化条件评估 ==========

    @SuppressWarnings("unchecked")
    private NodeExecutionResult evaluateByStructuredCondition(
            Node node, NodeConfig config, Map<String, Object> resolvedInputs) {

        List<ConditionBranch> branches = parseBranchesFromConfig(config, node.getNodeId());

        if (branches == null || branches.isEmpty()) {
            log.info("[Condition Node {}] No branches in config, falling back to legacy edge conversion",
                    node.getNodeId());
            List<Edge> outgoingEdges = (List<Edge>) resolvedInputs.get("__outgoingEdges__");
            if (outgoingEdges == null || outgoingEdges.isEmpty()) {
                log.error("[Condition Node {}] No branches defined and no outgoing edges found", node.getNodeId());
                return NodeExecutionResult.failed("No branches defined for condition node");
            }
            branches = convertLegacyEdgesToBranches(outgoingEdges, node.getNodeId());
        }

        ExecutionContext context = (ExecutionContext) resolvedInputs.get("__context__");
        if (context == null) {
            log.error("[Condition Node {}] ExecutionContext not found in resolvedInputs", node.getNodeId());
            return NodeExecutionResult.failed("ExecutionContext not available for condition evaluation");
        }

        ConditionBranch selectedBranch = conditionEvaluator.evaluate(branches, context);

        if (selectedBranch == null) {
            log.error("[Condition Node {}] No branch selected after evaluation", node.getNodeId());
            return NodeExecutionResult.failed("No matching condition and no default branch");
        }

        String selectedTargetNodeId = selectedBranch.getTargetNodeId();
        log.info("[Condition Node {}] Selected target: {}, priority: {}, isDefault: {}",
                node.getNodeId(), selectedTargetNodeId, selectedBranch.getPriority(), selectedBranch.isDefault());

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("selectedTarget", selectedTargetNodeId);
        outputs.put("selectedBranchPriority", selectedBranch.getPriority());
        outputs.put("selectedBranchIsDefault", selectedBranch.isDefault());

        return NodeExecutionResult.routing(selectedTargetNodeId, outputs);
    }

    private List<ConditionBranch> parseBranchesFromConfig(NodeConfig config, String nodeId) {
        Object branchesRaw = config.getProperties().get("branches");
        if (branchesRaw == null) {
            return null;
        }

        try {
            List<ConditionBranch> branches = objectMapper.convertValue(
                    branchesRaw,
                    new TypeReference<List<ConditionBranch>>() {});

            if (branches == null || branches.isEmpty()) {
                log.debug("[Condition Node {}] branches field is empty", nodeId);
                return null;
            }

            log.info("[Condition Node {}] Parsed {} branches from config", nodeId, branches.size());
            return branches;
        } catch (Exception e) {
            log.warn("[Condition Node {}] Failed to parse branches from config: {}",
                    nodeId, e.getMessage());
            return null;
        }
    }

    private List<ConditionBranch> convertLegacyEdgesToBranches(List<Edge> edges, String nodeId) {
        List<ConditionBranch> branches = new ArrayList<>();
        int priority = 0;
        boolean hasDefault = false;

        for (Edge edge : edges) {
            if (edge.isDefault()) {
                if (!hasDefault) {
                    branches.add(ConditionBranch.builder()
                            .priority(Integer.MAX_VALUE)
                            .targetNodeId(edge.getTarget())
                            .description("Legacy default branch")
                            .isDefault(true)
                            .conditionGroups(List.of())
                            .build());
                    hasDefault = true;
                } else {
                    log.warn("[Condition Node {}] Multiple default edges found, treating extra as non-default: {}",
                            nodeId, edge.getEdgeId());
                    branches.add(ConditionBranch.builder()
                            .priority(priority++)
                            .targetNodeId(edge.getTarget())
                            .description("Legacy conditional branch (from extra default edge)")
                            .isDefault(false)
                            .conditionGroups(List.of())
                            .build());
                }
            } else {
                log.info("[Condition Node {}] Legacy conditional edge '{}' with SpEL condition: {}",
                        nodeId, edge.getEdgeId(), edge.getCondition());
                branches.add(ConditionBranch.builder()
                        .priority(priority++)
                        .targetNodeId(edge.getTarget())
                        .description("Legacy conditional branch: " + edge.getCondition())
                        .isDefault(false)
                        .conditionGroups(List.of())
                        .build());
            }
        }

        if (!hasDefault && !branches.isEmpty()) {
            log.warn("[Condition Node {}] No default edge found, using last edge as default", nodeId);
            ConditionBranch lastBranch = branches.get(branches.size() - 1);
            branches.set(branches.size() - 1, ConditionBranch.builder()
                    .priority(Integer.MAX_VALUE)
                    .targetNodeId(lastBranch.getTargetNodeId())
                    .description("Legacy default branch (auto-assigned)")
                    .isDefault(true)
                    .conditionGroups(List.of())
                    .build());
        }

        log.info("[Condition Node {}] Converted {} legacy edges to branches (hasDefault={})",
                nodeId, branches.size(), hasDefault || !branches.isEmpty());
        return branches;
    }

    // ========== LLM 模式：基于 Branch 描述的语义路由 ==========

    @SuppressWarnings("unchecked")
    private NodeExecutionResult evaluateByLlmMode(
            Node node, NodeConfig config, Map<String, Object> resolvedInputs) {

        List<ConditionBranch> branches = parseBranchesFromConfig(config, node.getNodeId());

        if (branches == null || branches.isEmpty()) {
            log.info("[Condition Node {}] LLM mode: No branches in config, falling back to legacy edge conversion",
                    node.getNodeId());
            List<Edge> outgoingEdges = (List<Edge>) resolvedInputs.get("__outgoingEdges__");
            if (outgoingEdges == null || outgoingEdges.isEmpty()) {
                log.error("[Condition Node {}] LLM mode: No branches defined and no outgoing edges found",
                        node.getNodeId());
                return NodeExecutionResult.failed("No branches defined for condition node (LLM mode)");
            }
            branches = convertLegacyEdgesToBranches(outgoingEdges, node.getNodeId());
        }

        List<String> validTargetIds = branches.stream()
                .map(ConditionBranch::getTargetNodeId)
                .filter(id -> id != null && !id.isEmpty())
                .toList();

        if (validTargetIds.isEmpty()) {
            return NodeExecutionResult.failed("No valid target IDs in branches (LLM mode)");
        }

        ChatClient chatClient = buildChatClient(config);

        String initialPrompt = buildLlmPrompt(branches, resolvedInputs);
        String selectedTargetNodeId = callLlmAndMatchTarget(chatClient, initialPrompt, validTargetIds,
                node.getNodeId());

        if (selectedTargetNodeId == null) {
            log.warn("[Condition Node {}] LLM mode: First attempt returned invalid target, retrying with clarification",
                    node.getNodeId());
            String clarificationPrompt = buildClarificationPrompt(validTargetIds);
            selectedTargetNodeId = callLlmAndMatchTarget(chatClient, clarificationPrompt, validTargetIds,
                    node.getNodeId());
        }

        if (selectedTargetNodeId == null) {
            log.warn("[Condition Node {}] LLM mode: Retry also failed, falling back to default branch",
                    node.getNodeId());
            selectedTargetNodeId = findDefaultBranchTarget(branches);
        }

        if (selectedTargetNodeId == null) {
            return NodeExecutionResult.failed("LLM mode: No matching target and no default branch");
        }

        log.info("[Condition Node {}] LLM mode selected target: {}", node.getNodeId(), selectedTargetNodeId);

        Map<String, Object> outputs = new HashMap<>();
        outputs.put("selectedTarget", selectedTargetNodeId);
        outputs.put("routingMode", "LLM");

        return NodeExecutionResult.routing(selectedTargetNodeId, outputs);
    }

    /**
     * 创建 ChatClient 实例
     * 注意：使用 package-private 可见性以支持单元测试中的 mock（通过 Mockito spy）
     */
    ChatClient buildChatClient(NodeConfig config) {
        String model = config.getString("model");
        String apiUrl = config.getString("baseUrl");
        String apiKey = config.getString("apiKey");

        return ChatClient.builder(OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .apiKey(apiKey)
                        .baseUrl(apiUrl)
                        .restClientBuilder(restClientBuilder)
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .build())
                .build())
                .build();
    }

    /**
     * 构建 LLM Prompt，使用 branch.description 描述各分支
     * 注意：使用 package-private 可见性以支持单元测试验证 prompt 内容
     */
    String buildLlmPrompt(List<ConditionBranch> branches, Map<String, Object> resolvedInputs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following context, select the most appropriate next step.\n\n");
        prompt.append("Context:\n");
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            if (!entry.getKey().startsWith("__")) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        prompt.append("\nAvailable options:\n");

        String firstTargetId = null;
        for (ConditionBranch branch : branches) {
            if (branch.isDefault()) {
                continue;
            }
            String targetId = branch.getTargetNodeId();
            if (firstTargetId == null) {
                firstTargetId = targetId;
            }
            String description = branch.getDescription();
            if (description != null && !description.isEmpty()) {
                prompt.append("- ").append(targetId).append(": ").append(description).append("\n");
            } else {
                prompt.append("- ").append(targetId).append("\n");
            }
        }

        if (firstTargetId == null) {
            firstTargetId = branches.isEmpty() ? "unknown" : branches.get(0).getTargetNodeId();
        }

        prompt.append("\nRespond with ONLY the target ID (e.g., '").append(firstTargetId).append("'). ");
        prompt.append("Do not include any other text.");

        return prompt.toString();
    }

    /**
     * 构建澄清 Prompt（重试时使用）
     */
    private String buildClarificationPrompt(List<String> validTargetIds) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Your previous response was not a valid target ID. ");
        prompt.append("Please select exactly one of the following target IDs:\n");
        for (String targetId : validTargetIds) {
            prompt.append("- ").append(targetId).append("\n");
        }
        prompt.append("\nRespond with ONLY the target ID, nothing else.");
        return prompt.toString();
    }

    /**
     * 调用 LLM 并匹配目标 ID
     * 响应解析：trim 空白 + case-insensitive 匹配
     * 注意：使用 package-private 可见性以支持单元测试验证匹配逻辑
     */
    String callLlmAndMatchTarget(
            ChatClient chatClient, String prompt, List<String> validTargetIds, String nodeId) {
        try {
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null) {
                log.warn("[Condition Node {}] LLM returned null response", nodeId);
                return null;
            }

            String trimmed = response.trim();
            for (String validId : validTargetIds) {
                if (validId.equalsIgnoreCase(trimmed)) {
                    return validId;
                }
            }

            log.warn("[Condition Node {}] LLM returned invalid target ID: '{}'", nodeId, trimmed);
            return null;
        } catch (Exception e) {
            log.error("[Condition Node {}] LLM call failed: {}", nodeId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从分支列表中查找 default 分支的目标节点 ID
     */
    private String findDefaultBranchTarget(List<ConditionBranch> branches) {
        for (ConditionBranch branch : branches) {
            if (branch.isDefault()) {
                return branch.getTargetNodeId();
            }
        }
        return null;
    }
}
