package com.zj.aiagent.domain.agent.dag.node;

import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import com.zj.aiagent.shared.design.dag.NodeExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 规划节点 - 任务规划
 * 分析用户任务,制定执行计划,评估任务复杂度
 */
@Slf4j
public class PlanNode extends AbstractConfigurableNode {

    public PlanNode(String nodeId, String nodeName, NodeConfig config, ApplicationContext applicationContext) {
        super(nodeId, nodeName, config, applicationContext);
    }

    @Override
    protected NodeExecutionResult doExecute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            // 从context获取用户输入
            String userInput = context.getValue("userInput");
            if (userInput == null || userInput.isEmpty()) {
                userInput = context.getValue("userMessage", "");
            }

            log.info("规划节点开始执行，用户输入: {}", userInput);

            // 构建规划提示词
            String planningPrompt = buildPlanningPrompt(userInput, context);

            // 调用AI进行任务规划
            String planResult = callAI(planningPrompt, context);

            // 存储规划结果
            context.setValue("plan_result", planResult);
            context.setNodeResult(nodeId, planResult);

            log.info("规划节点执行完成，规划结果: {}", planResult);

            return NodeExecutionResult.content(planResult);

        } catch (Exception e) {
            throw new DagNodeExecutionException("规划节点执行失败: " + e.getMessage(), e, nodeId, true);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.PLAN_NODE;
    }

    /**
     * 构建规划提示词
     */
    private String buildPlanningPrompt(String userInput, DagExecutionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户任务: ").append(userInput).append("\n\n");

        // 如果有执行历史，加入上下文
        String executionHistory = context.getValue("execution_history");
        if (executionHistory != null && !executionHistory.isEmpty()) {
            prompt.append("之前的执行历史:\n").append(executionHistory).append("\n\n");
        }

        prompt.append("请根据上述任务制定详细的执行计划。");

        return prompt.toString();
    }
}
