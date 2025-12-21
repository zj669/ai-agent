package com.zj.aiagent.domain.agent.dag.node;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * React 模式节点 - Reasoning-Acting-Observing 循环
 * 
 * React 模式流程：
 * 1. Thought (思考): AI 分析当前状态，决定下一步行动
 * 2. Action (行动): 执行具体的行动
 * 3. Observation (观察): 获取行动的结果
 * 4. 重复 1-3 直到达成目标或达到最大迭代次数
 */
@Slf4j
public class ReactNode extends AbstractConfigurableNode {

    private static final int DEFAULT_MAX_ITERATIONS = 5;
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(.+?)(?:\\n|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+?)(?:\\n|$)",
            Pattern.CASE_INSENSITIVE);

    public ReactNode(String nodeId, String nodeName, NodeConfig config, ApplicationContext applicationContext) {
        super(nodeId, nodeName, config, applicationContext);
    }

    @Override
    protected String doExecute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            int maxIterations = getMaxIterations();
            String goal = context.getValue("userMessage", context.getValue("userInput", "完成用户任务"));

            log.info("React 节点开始执行，目标: {}, 最大迭代: {}", goal, maxIterations);

            List<Map<String, String>> iterations = new ArrayList<>();

            for (int i = 0; i < maxIterations; i++) {
                log.info("===== React 迭代 {}/{} =====", i + 1, maxIterations);

                // 1. Thought: 思考下一步行动
                String thoughtPrompt = buildThoughtPrompt(context, iterations, goal, i + 1);
                String thoughtResponse = callAI(thoughtPrompt, context);

                log.info("Thought: {}", thoughtResponse);

                // 检查是否完成
                if (isTaskComplete(thoughtResponse)) {
                    String finalAnswer = extractFinalAnswer(thoughtResponse);
                    log.info("任务完成！最终答案: {}", finalAnswer);

                    // 记录最后一轮
                    Map<String, String> finalIteration = new HashMap<>();
                    finalIteration.put("iteration", String.valueOf(i + 1));
                    finalIteration.put("thought", thoughtResponse);
                    finalIteration.put("status", "COMPLETE");
                    finalIteration.put("finalAnswer", finalAnswer);
                    iterations.add(finalIteration);

                    context.setValue("react_iterations", iterations);
                    return formatFinalResult(finalAnswer, iterations);
                }

                // 2. Action: 提取并执行行动
                String action = extractAction(thoughtResponse);
                String actionInput = extractActionInput(thoughtResponse);

                log.info("Action: {} with input: {}", action, actionInput);

                // 3. Observation: 执行行动并观察结果
                String observation = executeAction(action, actionInput, context);

                log.info("Observation: {}", observation);

                // 记录本轮迭代
                Map<String, String> iteration = new HashMap<>();
                iteration.put("iteration", String.valueOf(i + 1));
                iteration.put("thought", thoughtResponse);
                iteration.put("action", action);
                iteration.put("actionInput", actionInput);
                iteration.put("observation", observation);
                iterations.add(iteration);
            }

            // 达到最大迭代次数
            log.warn("达到最大迭代次数 {}, 未能完成任务", maxIterations);
            context.setValue("react_iterations", iterations);

            return formatIncompleteResult(iterations, maxIterations);

        } catch (Exception e) {
            throw new DagNodeExecutionException("React 节点执行失败: " + e.getMessage(), e, nodeId, true);
        }
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.REACT_NODE;
    }

    /**
     * 构建思考提示词
     */
    private String buildThoughtPrompt(DagExecutionContext context, List<Map<String, String>> iterations,
            String goal, int currentIteration) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个 ReAct 智能体，使用 Thought-Action-Observation 循环来解决问题。\n\n");
        prompt.append("目标: ").append(goal).append("\n\n");

        // 可用的行动
        prompt.append("可用的行动:\n");
        List<String> availableActions = getAvailableActions();
        for (String action : availableActions) {
            prompt.append("- ").append(action).append("\n");
        }
        prompt.append("\n");

        // 之前的迭代历史
        if (!iterations.isEmpty()) {
            prompt.append("之前的尝试:\n");
            for (Map<String, String> iter : iterations) {
                prompt.append("迭代 ").append(iter.get("iteration")).append(":\n");
                prompt.append("  Thought: ").append(iter.get("thought")).append("\n");
                prompt.append("  Action: ").append(iter.get("action")).append("\n");
                prompt.append("  Observation: ").append(iter.get("observation")).append("\n\n");
            }
        }

        prompt.append("当前是第 ").append(currentIteration).append(" 次迭代。\n\n");
        prompt.append("请按以下格式回复:\n");
        prompt.append("Thought: [你的思考过程]\n");
        prompt.append("Action: [选择的行动]\n");
        prompt.append("Action Input: [行动的输入参数]\n\n");
        prompt.append("如果你认为已经可以回答问题，请使用:\n");
        prompt.append("Thought: [总结思考]\n");
        prompt.append("Final Answer: [最终答案]\n");

        return prompt.toString();
    }

    /**
     * 检查任务是否完成
     */
    private boolean isTaskComplete(String thoughtResponse) {
        return thoughtResponse.toLowerCase().contains("final answer:");
    }

    /**
     * 提取最终答案
     */
    private String extractFinalAnswer(String thoughtResponse) {
        Pattern pattern = Pattern.compile("Final Answer:\\s*(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(thoughtResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return thoughtResponse;
    }

    /**
     * 提取行动
     */
    private String extractAction(String thoughtResponse) {
        Matcher matcher = ACTION_PATTERN.matcher(thoughtResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "unknown";
    }

    /**
     * 提取行动输入
     */
    private String extractActionInput(String thoughtResponse) {
        Matcher matcher = ACTION_INPUT_PATTERN.matcher(thoughtResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    /**
     * 执行行动
     */
    private String executeAction(String action, String actionInput, DagExecutionContext context) {
        try {
            return switch (action.toLowerCase()) {
                case "search", "搜索" -> executeSearch(actionInput, context);
                case "calculate", "计算" -> executeCalculate(actionInput, context);
                case "lookup", "查询" -> executeLookup(actionInput, context);
                case "finish", "完成" -> actionInput;
                default -> "未知行动: " + action + "。请选择有效的行动。";
            };
        } catch (Exception e) {
            log.error("执行行动失败: {}", action, e);
            return "行动执行失败: " + e.getMessage();
        }
    }

    /**
     * 执行搜索行动
     */
    private String executeSearch(String query, DagExecutionContext context) {
        // 这里可以集成实际的搜索服务
        log.info("执行搜索: {}", query);

        // 简化实现：使用 AI 模拟搜索结果
        String searchPrompt = String.format(
                "用户搜索: %s\n\n请提供相关的信息和答案。如果这是一个计算问题，请直接给出答案。",
                query);

        return callAI(searchPrompt, context);
    }

    /**
     * 执行计算行动
     */
    private String executeCalculate(String expression, DagExecutionContext context) {
        log.info("执行计算: {}", expression);

        // 简化实现：使用 AI 进行计算
        String calcPrompt = String.format(
                "请计算: %s\n\n只返回计算结果，不要有其他解释。",
                expression);

        return callAI(calcPrompt, context);
    }

    /**
     * 执行查询行动
     */
    private String executeLookup(String query, DagExecutionContext context) {
        log.info("执行查询: {}", query);

        // 可以查询之前的结果或上下文
        Object result = context.getValue(query);
        if (result != null) {
            return result.toString();
        }

        return "未找到相关信息: " + query;
    }

    /**
     * 获取可用行动列表
     */
    private List<String> getAvailableActions() {
        @SuppressWarnings("unchecked")
        List<String> customActions = (List<String>) config.getCustomConfig().get("availableActions");

        if (customActions != null && !customActions.isEmpty()) {
            return customActions;
        }

        // 默认行动
        return List.of(
                "Search - 搜索相关信息",
                "Calculate - 执行数学计算",
                "Lookup - 查询已知信息");
    }

    /**
     * 获取最大迭代次数
     */
    private int getMaxIterations() {
        Object maxIter = config.getCustomConfig().get("maxIterations");
        if (maxIter instanceof Number) {
            return ((Number) maxIter).intValue();
        }
        return DEFAULT_MAX_ITERATIONS;
    }

    /**
     * 格式化最终结果
     */
    private String formatFinalResult(String finalAnswer, List<Map<String, String>> iterations) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "SUCCESS");
        result.put("finalAnswer", finalAnswer);
        result.put("totalIterations", iterations.size());
        result.put("iterations", iterations);

        return JSON.toJSONString(result);
    }

    /**
     * 格式化未完成结果
     */
    private String formatIncompleteResult(List<Map<String, String>> iterations, int maxIterations) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "INCOMPLETE");
        result.put("message", "达到最大迭代次数 " + maxIterations + "，未能完成任务");
        result.put("totalIterations", iterations.size());
        result.put("iterations", iterations);

        return JSON.toJSONString(result);
    }
}
