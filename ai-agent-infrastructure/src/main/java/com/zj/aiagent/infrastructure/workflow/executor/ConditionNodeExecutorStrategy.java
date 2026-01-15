package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.config.ConditionNodeConfig;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.Branch;
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
 * 支持 SpEL 表达式和 LLM 语义路由两种模式
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
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                ConditionNodeConfig config = (ConditionNodeConfig) node.getConfig();

                String selectedBranchId;

                if (config.getRoutingStrategy() == ConditionNodeConfig.RoutingStrategy.EXPRESSION) {
                    selectedBranchId = evaluateByExpression(config.getBranches(), resolvedInputs);
                } else {
                    selectedBranchId = evaluateByLlm(config, resolvedInputs);
                }

                // 如果没有命中任何分支，使用默认分支
                if (selectedBranchId == null) {
                    selectedBranchId = config.getDefaultBranchId();
                    log.warn("[Condition Node {}] No branch matched, using default: {}",
                            node.getNodeId(), selectedBranchId);
                }

                log.info("[Condition Node {}] Selected branch: {}", node.getNodeId(), selectedBranchId);

                Map<String, Object> outputs = new HashMap<>();
                outputs.put("selectedBranch", selectedBranchId);

                return NodeExecutionResult.routing(selectedBranchId, outputs);

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
     * 表达式模式：逐个评估 SpEL 表达式
     */
    private String evaluateByExpression(List<Branch> branches, Map<String, Object> resolvedInputs) {
        if (branches == null || branches.isEmpty()) {
            return null;
        }

        // 构建评估上下文
        EvaluationContext context = new StandardEvaluationContext();
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            ((StandardEvaluationContext) context).setVariable(entry.getKey(), entry.getValue());
        }

        // 逐个评估分支条件
        for (Branch branch : branches) {
            String condition = branch.getCondition();
            if (condition == null || condition.isEmpty() || "true".equalsIgnoreCase(condition)) {
                return branch.getBranchId(); // 默认分支
            }

            try {
                Expression exp = PARSER.parseExpression(condition);
                Boolean result = exp.getValue(context, Boolean.class);
                if (Boolean.TRUE.equals(result)) {
                    return branch.getBranchId();
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate condition '{}': {}", condition, e.getMessage());
            }
        }

        return null;
    }

    /**
     * LLM 语义模式：让 LLM 选择最匹配的分支
     */
    private String evaluateByLlm(ConditionNodeConfig config, Map<String, Object> resolvedInputs) {
        if (config.getBranches() == null || config.getBranches().isEmpty()) {
            return null;
        }

        // 构建 Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following context, select the most appropriate branch.\n\n");
        prompt.append("Context:\n");
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        prompt.append("\nAvailable branches:\n");
        for (Branch branch : config.getBranches()) {
            prompt.append("- ").append(branch.getBranchId()).append(": ").append(branch.getDescription()).append("\n");
        }
        prompt.append("\nRespond with ONLY the branch ID (e.g., '").append(config.getBranches().get(0).getBranchId())
                .append("'):");

        // 调用 LLM
        ChatClient chatClient = chatClientBuilder.build();
        String response = chatClient.prompt()
                .user(prompt.toString())
                .call()
                .content();

        // 解析响应
        if (response != null) {
            String trimmed = response.trim();
            // 验证返回的分支ID是否有效
            for (Branch branch : config.getBranches()) {
                if (branch.getBranchId().equalsIgnoreCase(trimmed)) {
                    return branch.getBranchId();
                }
            }
            log.warn("LLM returned invalid branch ID: {}", trimmed);
        }

        return null;
    }
}
