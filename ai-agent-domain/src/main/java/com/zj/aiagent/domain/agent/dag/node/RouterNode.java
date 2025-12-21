package com.zj.aiagent.domain.agent.dag.node;

import com.alibaba.fastjson.JSON;
import com.zj.aiagent.domain.agent.dag.config.NodeConfig;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.entity.NodeExecutionLog;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import com.zj.aiagent.domain.agent.dag.repository.IDagExecutionRepository;
import com.zj.aiagent.shared.design.dag.ConditionalDagNode;
import com.zj.aiagent.shared.design.dag.DagNode;
import com.zj.aiagent.shared.design.dag.DagNodeExecutionException;
import com.zj.aiagent.shared.design.dag.NodeRouteDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * 路由节点 - 条件路由选择下一个节点
 * 根据条件选择下一个节点,支持AI评估条件,支持规则表达式
 * 
 * 注意：RouterNode直接实现ConditionalDagNode，不继承AbstractConfigurableNode
 * 因为返回类型不同（NodeRouteDecision vs String）
 */
@Slf4j
public class RouterNode implements ConditionalDagNode<DagExecutionContext> {

    protected final String nodeId;
    protected final String nodeName;
    protected final NodeConfig config;
    protected final ApplicationContext applicationContext;
    private final Set<String> candidateNodes;

    // 创建一个helper来复用ChatClient构建逻辑
    private final NodeConfigHelper configHelper;

    public RouterNode(String nodeId, String nodeName, NodeConfig config, ApplicationContext applicationContext) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.config = config;
        this.applicationContext = applicationContext;
        this.configHelper = new NodeConfigHelper(config, applicationContext);

        if (config == null) {
            throw new NodeConfigException("NodeConfig cannot be null for node: " + nodeId);
        }

        // 从配置中获取候选节点列表
        List<String> candidates = (List<String>) config.getCustomConfig().get("candidateNodes");
        this.candidateNodes = candidates != null ? new HashSet<>(candidates) : new HashSet<>();
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getCandidateNextNodes() {
        return candidateNodes;
    }

    @Override
    public void beforeExecute(DagExecutionContext context) {
        log.info("路由节点 [{}] ({}) 开始执行", nodeName, nodeId);

        try {
            // 创建节点执行日志
            NodeExecutionLog executionLog = NodeExecutionLog.builder()
                    .instanceId(context.getInstanceId())
                    .agentId(context.getAgentId())
                    .conversationId(context.getConversationId())
                    .nodeId(nodeId)
                    .nodeType(NodeType.ROUTER_NODE.name())
                    .nodeName(nodeName)
                    .build();

            // 记录输入数据
            // 如果是第一个节点(没有前序结果),记录用户输入;否则记录前序节点结果
            String inputJson;
            Map<String, Object> nodeResults = context.getAllNodeResults();
            if (nodeResults == null || nodeResults.isEmpty()) {
                // 第一个节点 - 记录用户输入
                String userInput = context.getValue("userInput", "");
                Map<String, Object> inputData = new HashMap<>();
                inputData.put("userInput", userInput);
                inputJson = JSON.toJSONString(inputData);
            } else {
                // 后续节点 - 记录前序节点结果
                inputJson = JSON.toJSONString(nodeResults);
            }
            executionLog.start(inputJson);

            // 保存日志并存入context
            IDagExecutionRepository repository = getExecutionRepository();
            if (repository != null) {
                executionLog = repository.saveNodeLog(executionLog);
                context.setValue("_node_log_" + nodeId, executionLog);
            }
        } catch (Exception e) {
            log.warn("保存路由节点执行日志失败，不影响执行", e);
        }
    }

    @Override
    public void afterExecute(DagExecutionContext context, NodeRouteDecision result, Exception exception) {
        // 获取执行日志
        NodeExecutionLog executionLog = (NodeExecutionLog) context.getValue("_node_log_" + nodeId);

        if (exception != null) {
            log.error("路由节点 [{}] ({}) 执行失败", nodeName, nodeId, exception);

            // 更新日志为失败
            if (executionLog != null) {
                try {
                    executionLog.fail(exception.getMessage(), getStackTrace(exception));
                    IDagExecutionRepository repository = getExecutionRepository();
                    if (repository != null) {
                        repository.updateNodeLog(executionLog);
                    }
                } catch (Exception e) {
                    log.warn("更新路由节点执行日志失败，不影响执行", e);
                }
            }
        } else {
            log.info("路由节点 [{}] ({}) 执行成功，路由决策: {}", nodeName, nodeId,
                    result != null ? result.getNextNodeIds() : "STOP");

            // 更新日志为成功
            if (executionLog != null) {
                try {
                    String outputJson = result != null ? JSON.toJSONString(result) : "";
                    executionLog.succeed(outputJson);
                    IDagExecutionRepository repository = getExecutionRepository();
                    if (repository != null) {
                        repository.updateNodeLog(executionLog);
                    }
                } catch (Exception e) {
                    log.warn("更新路由节点执行日志失败，不影响执行", e);
                }
            }
        }
    }

    @Override
    public long getTimeoutMillis() {
        return config.getTimeout() != null ? config.getTimeout() : 0;
    }

    /**
     * 获取仓储
     */
    private IDagExecutionRepository getExecutionRepository() {
        try {
            return applicationContext.getBean(IDagExecutionRepository.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取堆栈跟踪
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        // 截断堆栈,避免太长
        if (stackTrace.length() > 2000) {
            stackTrace = stackTrace.substring(0, 2000) + "...";
        }
        return stackTrace;
    }

    @Override
    public NodeRouteDecision execute(DagExecutionContext context) throws DagNodeExecutionException {
        try {
            log.info("路由节点开始执行，评估下一步执行路径");

            // 获取路由策略
            String routingStrategy = (String) config.getCustomConfig().getOrDefault("routingStrategy", "AI");

            String selectedNode;
            if ("AI".equals(routingStrategy)) {
                selectedNode = evaluateByAI(context);
            } else {
                selectedNode = evaluateByRules(context);
            }

            log.info("路由节点执行完成，选择节点: {}", selectedNode);

            // 存储路由决策
            context.setValue("router_decision", selectedNode);
            context.setNodeResult(nodeId, selectedNode);

            if (selectedNode == null || selectedNode.isEmpty()) {
                return NodeRouteDecision.stop();
            }

            return NodeRouteDecision.continueWith(selectedNode);

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

        // 使用helper调用AI评估
        String aiDecision = configHelper.callAI(routingPrompt, context);

        // 解析AI返回的节点ID
        return parseNodeIdFromAIResponse(aiDecision);
    }

    /**
     * NodeConfigHelper - 复用ChatClient构建逻辑
     */
    private static class NodeConfigHelper extends AbstractConfigurableNode {

        public NodeConfigHelper(NodeConfig config, ApplicationContext applicationContext) {
            super("helper", "helper", config, applicationContext);
        }

        @Override
        protected String doExecute(DagExecutionContext context) {
            return null;
        }

        @Override
        public NodeType getNodeType() {
            return null;
        }

        // 暴露callAI方法供RouterNode使用
        @Override
        public String callAI(String userMessage, DagExecutionContext context) throws DagNodeExecutionException {
            return super.callAI(userMessage, context);
        }
    }

    /**
     * 通过规则评估选择节点
     */
    private String evaluateByRules(DagExecutionContext context) {
        // 获取规则列表
        List<Map<String, String>> rules = (List<Map<String, String>>) config.getCustomConfig().get("routingRules");

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

        // 添加自定义提示词（如果有）
        String customPrompt = (String) config.getCustomConfig().get("routingPrompt");
        if (customPrompt != null && !customPrompt.isEmpty()) {
            prompt.append(customPrompt).append("\n\n");
        }

        // 尝试从context中获取DagGraph
        Object dagGraphObj = context.getValue("__DAG_GRAPH__");
        if (dagGraphObj instanceof DagGraph dagGraph) {
            // 自动获取候选节点信息
            prompt.append("候选节点及其功能：\n");
            for (String candidateId : candidateNodes) {
                Object nodeObj = dagGraph.getNode(candidateId);
                if (nodeObj != null) {
                    String nodeName = getNodeName(nodeObj);
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

        // 添加执行历史
        String executionHistory = context.getValue("execution_history", "");
        if (!executionHistory.isEmpty()) {
            prompt.append("执行历史:\n").append(executionHistory).append("\n\n");
        }

        // 添加最近的执行结果
        String executionResult = context.getValue("execution_result", "");
        if (!executionResult.isEmpty()) {
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
    private String getNodeName(Object nodeObj) {
        if (nodeObj instanceof DagNode) {
            return ((DagNode) nodeObj).getNodeName();
        } else if (nodeObj instanceof ConditionalDagNode) {
            return ((ConditionalDagNode) nodeObj).getNodeName();
        }
        return "Unknown";
    }

    /**
     * 获取节点描述（从configuration中提取systemPrompt）
     */
    private String getNodeDescription(Object nodeObj) {
        try {
            if (nodeObj instanceof AbstractConfigurableNode) {
                AbstractConfigurableNode node = (AbstractConfigurableNode) nodeObj;
                // 从config中获取systemPrompt作为节点功能描述
                String systemPrompt = node.config.getSystemPrompt();
                if (systemPrompt != null && !systemPrompt.isEmpty()) {
                    // 截取前100个字符作为描述
                    return systemPrompt.length() > 100 ? systemPrompt.substring(0, 100) + "..." : systemPrompt;
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
