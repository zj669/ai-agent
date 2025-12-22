package com.zj.aiagent.domain.agent.dag.node;

import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.ContextKey;
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
            // 从领域对象获取规划结果和用户输入
            String planResult = context.getExecutionData().getPlanResult();
            String userInput = context.getUserInputData().getEffectiveInput("");

            log.info("执行节点开始执行，规划结果: {}", planResult);

            // 构建执行提示词
            String executionPrompt = buildExecutionPrompt(userInput, planResult, context);

            // 调用AI执行任务(可能会调用MCP工具)
            String executionResult = callAI(executionPrompt, context);

            // 更新领域对象中的执行结果
            context.getExecutionData().updateExecutionResult(executionResult);
            context.setNodeResult(nodeId, executionResult);

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
        String previousResult = context.getExecutionData().getPreviousExecutionResult();
        if (previousResult != null && !previousResult.isEmpty()) {
            prompt.append("之前的执行结果:\n").append(previousResult).append("\n\n");
        }

        prompt.append("请根据上述计划执行任务，使用必要的工具完成任务。");

        return prompt.toString();
    }

    // updateExecutionHistory 方法已移动到 ExecutionData.updateExecutionResult()
}
