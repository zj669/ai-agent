package com.zj.aiagent.domain.agent.dag.node;

import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import com.zj.aiagent.shared.design.dag.NodeExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 执行节点 - 任务执行
 * 执行具体任务,调用工具(MCP),生成执行结果
 */
@Slf4j
public class ActNode extends AbstractConfigurableNode {

    public ActNode(String nodeId, String nodeName, NodeConfig config, ApplicationContext applicationContext) {
        super(nodeId, nodeName, config, applicationContext);
    }

    @Override
    protected NodeExecutionResult doExecute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            // 获取规划结果
            String planResult = context.getValue("plan_result");
            String userInput = context.getValue("userInput", context.getValue("userMessage", ""));

            log.info("执行节点开始执行，规划结果: {}", planResult);

            // 构建执行提示词
            String executionPrompt = buildExecutionPrompt(userInput, planResult, context);

            // 调用AI执行任务(可能会调用MCP工具)
            String executionResult = callAI(executionPrompt, context);

            // 存储执行结果
            context.setValue("execution_result", executionResult);
            context.setNodeResult(nodeId, executionResult);

            // 更新执行历史
            updateExecutionHistory(context, executionResult);

            log.info("执行节点执行完成，执行结果: {}", executionResult);

            return NodeExecutionResult.content(executionResult);

        } catch (Exception e) {
            throw new DagNodeExecutionException("执行节点执行失败: " + e.getMessage(), e, nodeId, true);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ACT_NODE;
    }

    /**
     * 构建执行提示词
     */
    private String buildExecutionPrompt(String userInput, String planResult, DagExecutionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户任务: ").append(userInput).append("\n\n");

        if (planResult != null && !planResult.isEmpty()) {
            prompt.append("执行计划:\n").append(planResult).append("\n\n");
        }

        // 如果有之前的执行结果，加入上下文
        String previousResult = context.getValue("previous_execution_result");
        if (previousResult != null && !previousResult.isEmpty()) {
            prompt.append("之前的执行结果:\n").append(previousResult).append("\n\n");
        }

        prompt.append("请根据上述计划执行任务，使用必要的工具完成任务。");

        return prompt.toString();
    }

    /**
     * 更新执行历史
     */
    private void updateExecutionHistory(DagExecutionContext context, String executionResult) {
        String history = context.getValue("execution_history", "");
        history = history + "\n[执行结果]: " + executionResult;
        context.setValue("execution_history", history);
        context.setValue("previous_execution_result", executionResult);
    }
}
