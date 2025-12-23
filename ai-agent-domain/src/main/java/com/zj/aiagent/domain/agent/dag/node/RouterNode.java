package com.zj.aiagent.domain.agent.dag.node;

import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.ContextKey;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import com.zj.aiagent.shared.design.dag.DagNode;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import com.zj.aiagent.shared.design.dag.NodeExecutionResult;
import com.zj.aiagent.shared.design.dag.NodeRouteDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.*;

/**
 * 路由节点 - 条件路由选择下一个节点
 * 根据条件选择下一个节点,支持AI评估条件,支持规则表达式
 * 
 * 重构后：继承 AbstractConfigurableNode，统一节点类型体系
 */
@Slf4j
public class RouterNode extends AbstractConfigurableNode {

    private final Set<String> candidateNodes;

    public RouterNode(String nodeId, String nodeName, NodeConfig config, ApplicationContext applicationContext) {
        super(nodeId, nodeName, config, applicationContext);

        if (config == null) {
            throw new NodeConfigException("NodeConfig cannot be null for node: " + nodeId);
        }

        // 从配置中获取候选节点列表（使用标准字段 nextNodes）
        List<String> candidates = config.getNextNodes();
        this.candidateNodes = candidates != null ? new HashSet<>(candidates) : new HashSet<>();
    }

    @Override
    public Set<String> getCandidateNextNodes() {
        return candidateNodes;
    }

    @Override
    public boolean isRouterNode() {
        return true;
    }

    @Override
    public NodeType getNodeType() {
        return NodeType.ROUTER_NODE;
    }

    @Override
    protected NodeExecutionResult doExecute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            log.info("路由节点开始执行，评估下一步执行路径");

            // 获取路由策略（默认使用 AI）
            String routingStrategy = "AI";

            String selectedNode;
            if ("AI".equals(routingStrategy)) {
                selectedNode = evaluateByAI(context);
            } else {
                selectedNode = evaluateByRules(context);
            }

            log.info("路由节点执行完成，选择节点: {}", selectedNode);

            // 存储路由决策到 ContextKey（用于其他节点引用）
            context.setValue(ContextKey.ROUTER_DECISION.key(), selectedNode);

            // 构建路由结果
            NodeExecutionResult result;
            if (selectedNode == null || selectedNode.isEmpty()) {
                result = NodeExecutionResult.routing(NodeRouteDecision.stop(), "路由决策：停止执行");
            } else {
                result = NodeExecutionResult.routing(
                        NodeRouteDecision.continueWith(selectedNode),
                        "路由决策：继续执行节点 " + selectedNode);
            }

            // 存储完整的 NodeExecutionResult（而不是仅存储字符串）
            // 这样在恢复执行时可以正确解析路由决策
            context.setNodeResult(nodeId, result);

            return result;

        } catch (Exception e) {
            throw new DagNodeExecutionException("路由节点执行失败: " + e.getMessage(), e, nodeId, true);
        }
    }

    /**
     * 通过AI评估选择节点
     */
    private String evaluateByAI(DagExecutionContext context) throws DagNodeExecutionException {
        // 构建路由提示词
        String routingPrompt = buildRoutingPrompt(context);

        // 使用父类的 callAI 方法
        String aiDecision = callAI(routingPrompt, context);

        // 解析AI返回的节点ID
        return parseNodeIdFromAIResponse(aiDecision);
    }

    /**
     * 通过规则评估选择节点
     */
    @SuppressWarnings("unchecked")
    private String evaluateByRules(DagExecutionContext context) {
        // 规则路由已禁用，返回第一个候选节点
        List<Map<String, String>> rules = null;

        if (rules == null || rules.isEmpty()) {
            log.warn("未配置路由规则，返回第一个候选节点");
            return candidateNodes.iterator().next();
        }

        // 评估规则
        for (Map<String, String> rule : rules) {
            String condition = rule.get("condition");
            String targetNode = rule.get("targetNode");

            if (evaluateCondition(condition, context)) {
                return targetNode;
            }
        }

        // 默认返回第一个候选节点
        return candidateNodes.isEmpty() ? null : candidateNodes.iterator().next();
    }

    /**
     * 构建路由提示词
     */
    private String buildRoutingPrompt(DagExecutionContext context) {
        StringBuilder prompt = new StringBuilder();

        // 从领域对象获取 DagGraph
        DagGraph dagGraph = context.getDagGraph();
        if (dagGraph != null) {
            // 自动获取候选节点信息
            prompt.append("候选节点及其功能：\n");
            for (String candidateId : candidateNodes) {
                Object nodeObj = dagGraph.getNode(candidateId);
                if (nodeObj != null) {
                    String nodeName = getNodeNameFromObj(nodeObj);
                    String nodeDescription = getNodeDescription(nodeObj);

                    prompt.append("- ").append(candidateId)
                            .append(" (").append(nodeName).append("): ")
                            .append(nodeDescription)
                            .append("\n");
                }
            }
            prompt.append("\n");
        } else {
            // 如果没有DagGraph，回退到简单列表
            prompt.append("候选节点: ").append(String.join(", ", candidateNodes)).append("\n\n");
        }

        // 从领域对象添加执行历史
        String executionHistory = context.getExecutionData().getHistoryAsString();
        if (!executionHistory.isEmpty()) {
            prompt.append("执行历史:\n").append(executionHistory).append("\n\n");
        }

        // 添加最近的执行结果
        String executionResult = context.getExecutionData().getExecutionResult();
        if (executionResult != null && !executionResult.isEmpty()) {
            prompt.append("最近执行结果:\n").append(executionResult).append("\n\n");
        }

        // 添加前序节点的结果（用于辅助决策）
        addPreviousNodeResults(prompt, context);

        prompt.append("请从候选节点中选择一个最合适的节点继续执行，只返回节点ID。");

        return prompt.toString();
    }

    /**
     * 获取节点名称
     */
    private String getNodeNameFromObj(Object nodeObj) {
        if (nodeObj instanceof DagNode) {
            return ((DagNode<?, ?>) nodeObj).getNodeName();
        }
        return "Unknown";
    }

    /**
     * 获取节点描述（从configuration中提取systemPrompt）
     */
    private String getNodeDescription(Object nodeObj) {
        try {
            if (nodeObj instanceof AbstractConfigurableNode node) {
                // 从config中获取systemPrompt作为节点功能描述
                Map<String, Object> customConfig = node.config.getCustomConfig();
                if (customConfig != null && !customConfig.isEmpty()) {
                    return customConfig.get("functionDescription").toString();
                }
            }
        } catch (Exception e) {
            log.warn("获取节点描述失败: {}", e.getMessage());
        }
        return "未提供描述";
    }

    /**
     * 添加前序节点的执行结果到提示词中
     */
    private void addPreviousNodeResults(StringBuilder prompt, DagExecutionContext context) {
        // 获取所有已执行节点的结果
        var nodeResults = context.getAllNodeResults();
        if (nodeResults != null && !nodeResults.isEmpty()) {
            prompt.append("前序节点执行结果：\n");
            nodeResults.forEach((nodeId, result) -> {
                if (!nodeId.equals(this.nodeId)) { // 排除自身
                    String resultStr = result != null ? result.toString() : "null";
                    // 截取结果前200字符
                    if (resultStr.length() > 200) {
                        resultStr = resultStr.substring(0, 200) + "...";
                    }
                    prompt.append("  - ").append(nodeId).append(": ")
                            .append(resultStr).append("\n");
                }
            });
            prompt.append("\n");
        }
    }

    /**
     * 从AI响应中解析节点ID
     */
    private String parseNodeIdFromAIResponse(String aiResponse) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return null;
        }

        // 尝试在响应中查找候选节点ID
        for (String nodeId : candidateNodes) {
            if (aiResponse.contains(nodeId)) {
                return nodeId;
            }
        }

        // 如果没找到，返回第一个候选节点
        return candidateNodes.isEmpty() ? null : candidateNodes.iterator().next();
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition, DagExecutionContext context) {
        if (condition == null || condition.isEmpty()) {
            return false;
        }

        // 简单的条件评估(可以后续扩展为表达式引擎)
        try {
            // 支持简单的key==value格式
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String expectedValue = parts[1].trim().replace("\"", "").replace("'", "");

                    // 提取context.get('key')中的key
                    if (key.contains("context.get(")) {
                        key = key.substring(key.indexOf("'") + 1, key.lastIndexOf("'"));
                    }

                    Object actualValue = context.getValue(key);
                    return expectedValue.equals(String.valueOf(actualValue));
                }
            }

            // 支持简单的key>value格式
            if (condition.contains(">")) {
                String[] parts = condition.split(">");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    int expectedValue = Integer.parseInt(parts[1].trim());

                    if (key.contains("context.get(")) {
                        key = key.substring(key.indexOf("'") + 1, key.lastIndexOf("'"));
                    }

                    Object actualValue = context.getValue(key);
                    if (actualValue instanceof Number) {
                        return ((Number) actualValue).intValue() > expectedValue;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            log.error("条件评估失败: {}", condition, e);
            return false;
        }
    }
}
