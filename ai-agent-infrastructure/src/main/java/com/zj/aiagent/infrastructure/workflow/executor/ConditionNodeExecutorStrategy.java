package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.config.NodeConfig;
import com.zj.aiagent.domain.workflow.entity.Edge;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 条件路由节点执行策略
 * 根据边上的条件表达式选择下游节点
 * 
 * 设计说明：
 * - 条件节点本身只负责决策逻辑
 * - 条件表达式/描述存储在出边的 condition 字段中
 * - 支持 SpEL 表达式模式和 LLM 语义模式
 */
@Slf4j
@Component
public class ConditionNodeExecutorStrategy implements NodeExecutorStrategy {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private final ChatClient.Builder chatClientBuilder;
    private final Executor executor;

    public ConditionNodeExecutorStrategy(
            ChatClient.Builder chatClientBuilder,
            @Qualifier("nodeExecutorThreadPool") Executor executor) {
        this.chatClientBuilder = chatClientBuilder;
        this.executor = executor;
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

                // 从 resolvedInputs 获取出边列表
                List<Edge> outgoingEdges = (List<Edge>) resolvedInputs.get("__outgoingEdges__");
                if (outgoingEdges == null || outgoingEdges.isEmpty()) {
                    log.warn("[Condition Node {}] No outgoing edges found", node.getNodeId());
                    return NodeExecutionResult.failed("No outgoing edges defined for condition node");
                }

                String selectedTargetNodeId;
                String routingStrategy = config.getString("routingStrategy", "EXPRESSION");

                if ("EXPRESSION".equalsIgnoreCase(routingStrategy)) {
                    selectedTargetNodeId = evaluateByExpression(outgoingEdges, resolvedInputs);
                } else {
                    selectedTargetNodeId = evaluateByLlm(outgoingEdges, resolvedInputs);
                }

                // 如果没有命中任何条件，使用默认边
                if (selectedTargetNodeId == null) {
                    selectedTargetNodeId = findDefaultTarget(outgoingEdges);
                    log.warn("[Condition Node {}] No condition matched, using default: {}",
                            node.getNodeId(), selectedTargetNodeId);
                }

                if (selectedTargetNodeId == null) {
                    return NodeExecutionResult.failed("No matching condition and no default edge");
                }

                log.info("[Condition Node {}] Selected target: {}", node.getNodeId(), selectedTargetNodeId);

                Map<String, Object> outputs = new HashMap<>();
                outputs.put("selectedTarget", selectedTargetNodeId);

                return NodeExecutionResult.routing(selectedTargetNodeId, outputs);

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

    /**
     * 表达式模式：遍历出边，评估每条边的 condition（SpEL 表达式）
     */
    private String evaluateByExpression(List<Edge> edges, Map<String, Object> resolvedInputs) {
        // 构建评估上下文
        EvaluationContext context = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            if (!entry.getKey().startsWith("__")) {
                ((StandardEvaluationContext) context).setVariable(entry.getKey(), entry.getValue());
            }
        }

        // 逐条评估边的条件
        for (Edge edge : edges) {
            String condition = edge.getCondition();

            // 跳过无条件或默认条件的边
            if (edge.isDefault()) {
                continue;
            }

            try {
                Expression exp = PARSER.parseExpression(condition);
                Boolean result = exp.getValue(context, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return edge.getTarget();
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate condition '{}' on edge {}: {}",
                        condition, edge.getEdgeId(), e.getMessage());
            }
        }

        return null;
    }

    /**
     * LLM 语义模式：让 LLM 根据上下文选择最匹配的边
     */
    private String evaluateByLlm(List<Edge> edges, Map<String, Object> resolvedInputs) {
        // 构建 Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following context, select the most appropriate next step.\n\n");
        prompt.append("Context:\n");
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            if (!entry.getKey().startsWith("__")) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        prompt.append("\nAvailable options:\n");

        String firstTarget = null;
        for (Edge edge : edges) {
            if (firstTarget == null) {
                firstTarget = edge.getTarget();
            }
            String condition = edge.getCondition();
            if (condition != null && !condition.isEmpty() && !edge.isDefault()) {
                prompt.append("- ").append(edge.getTarget()).append(": ").append(condition).append("\n");
            }
        }
        prompt.append("\nRespond with ONLY the target ID (e.g., '").append(firstTarget).append("'):");

        // 调用 LLM
        ChatClient chatClient = chatClientBuilder.build();
        String response = chatClient.prompt()
                .user(prompt.toString())
                .call()
                .content();

        // 解析响应
        if (response != null) {
            String trimmed = response.trim();
            // 验证返回的目标ID是否有效
            for (Edge edge : edges) {
                if (edge.getTarget().equalsIgnoreCase(trimmed)) {
                    return edge.getTarget();
                }
            }
            log.warn("LLM returned invalid target ID: {}", trimmed);
        }

        return null;
    }

    /**
     * 查找默认边的目标节点
     */
    private String findDefaultTarget(List<Edge> edges) {
        for (Edge edge : edges) {
            if (edge.isDefault()) {
                return edge.getTarget();
            }
        }
        // 如果没有明确的默认边，返回第一条边
        return edges.isEmpty() ? null : edges.get(0).getTarget();
    }
}
