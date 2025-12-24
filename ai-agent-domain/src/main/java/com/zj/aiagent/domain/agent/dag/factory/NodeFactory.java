package com.zj.aiagent.domain.agent.dag.factory;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zj.aiagent.domain.agent.dag.config.*;
import com.zj.aiagent.domain.agent.dag.context.DagExecutionContext;
import com.zj.aiagent.domain.agent.dag.entity.GraphJsonSchema;
import com.zj.aiagent.domain.agent.dag.exception.NodeConfigException;
import com.zj.aiagent.domain.agent.dag.node.*;
import com.zj.aiagent.shared.design.dag.DagNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点工厂 - 根据配置创建节点实例
 */
@Slf4j
@Component
public class NodeFactory {

    private final ApplicationContext applicationContext;

    public NodeFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 根据节点定义创建节点实例
     * 统一返回 AbstractConfigurableNode 类型
     * 
     * @param nodeDef 节点定义
     * @param edges   边定义列表（用于自动推断 RouterNode 的候选节点）
     */
    public AbstractConfigurableNode createNode(
            GraphJsonSchema.NodeDefinition nodeDef,
            List<GraphJsonSchema.EdgeDefinition> edges) {
        String nodeType = nodeDef.getNodeType();
        String nodeId = nodeDef.getNodeId();
        String nodeName = nodeDef.getNodeName();

        // 解析NodeConfig
        NodeConfig config = parseNodeConfig(nodeDef.getConfig());

        // 对于 ROUTER_NODE，从边中自动提取候选节点（前端不会传 nextNodes，完全由后端解析）
        if ("ROUTER_NODE".equals(nodeType) && edges != null) {
            List<String> candidateNodes = extractCandidateNodesFromEdges(nodeId, edges);
            if (!candidateNodes.isEmpty()) {
                config.setNextNodes(candidateNodes);
                log.info("为 RouterNode {} 自动提取候选节点: {}", nodeId, candidateNodes);
            } else {
                log.warn("RouterNode {} 没有找到任何候选节点（没有出边）", nodeId);
            }
        }

        // 根据类型创建节点
        return switch (nodeType) {
            case "PLAN_NODE" -> new PlanNode(nodeId, nodeName, config, applicationContext);
            case "ACT_NODE" -> new ActNode(nodeId, nodeName, config, applicationContext);
            // case "HUMAN_NODE" -> new HumanNode(nodeId, nodeName, config,
            // applicationContext); // 已废弃
            case "ROUTER_NODE" -> new RouterNode(nodeId, nodeName, config, applicationContext);
            case "REACT_NODE" -> new ReactNode(nodeId, nodeName, config, applicationContext);
            default -> throw new NodeConfigException("Unknown node type: " + nodeType);
        };
    }

    /**
     * 从边定义中提取候选节点
     * 返回以指定节点为 source 的所有边的 target 节点
     */
    private List<String> extractCandidateNodesFromEdges(String nodeId, List<GraphJsonSchema.EdgeDefinition> edges) {
        List<String> candidates = new ArrayList<>();
        for (GraphJsonSchema.EdgeDefinition edge : edges) {
            if (nodeId.equals(edge.getSource())) {
                candidates.add(edge.getTarget());
            }
        }
        return candidates;
    }

    /**
     * 解析节点配置
     */
    private NodeConfig parseNodeConfig(Object configObj) {
        if (configObj == null) {
            throw new NodeConfigException("Node config cannot be null");
        }

        JSONObject configJson = (JSONObject) JSON.toJSON(configObj);

        NodeConfig.NodeConfigBuilder builder = NodeConfig.builder();

        // 系统提示词(路由节点等可能为空)
        String systemPrompt = configJson.getString("systemPrompt");
        // RouterNode 等节点不需要 systemPrompt
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            builder.systemPrompt(systemPrompt);
        }

        // 模型配置
        if (configJson.containsKey("model") && configJson.getJSONObject("model") != null
                && !configJson.getJSONObject("model").isEmpty()) {
            builder.model(parseModelConfig(configJson.getJSONObject("model")));
        }

        // // 记忆配置
        // if (configJson.containsKey("memory")) {
        // builder.memory(parseMemoryConfig(configJson.getJSONObject("memory")));
        // }

        // Advisor配置
        if (configJson.containsKey("advisors") && configJson.getJSONArray("advisors") != null
                && !configJson.getJSONArray("advisors").isEmpty()) {
            builder.advisors(parseAdvisorConfigs(configJson.getJSONArray("advisors")));
        }

        // MCP工具配置
        if (configJson.containsKey("mcpTools") && configJson.getJSONArray("mcpTools") != null
                && !configJson.getJSONArray("mcpTools").isEmpty()) {
            builder.mcpTools(parseMcpToolConfigs(configJson.getJSONArray("mcpTools")));
        }

        // 人工介入配置
        if (configJson.containsKey("humanIntervention") && configJson.getJSONObject("humanIntervention") != null
                && !configJson.getJSONObject("humanIntervention").isEmpty()) {
            builder.humanIntervention(parseHumanInterventionConfig(configJson.getJSONObject("humanIntervention")));
        }

        // 候选节点列表（用于 RouterNode）
        if (configJson.containsKey("nextNodes") && configJson.getJSONObject("nextNodes") != null
                && !configJson.getJSONObject("nextNodes").isEmpty()) {
            List<String> nextNodes = configJson.getList("nextNodes", String.class);
            builder.nextNodes(nextNodes);
        }
        if(configJson.containsKey("userPrompt") && configJson.getJSONObject("userPrompt") != null){
            JSONObject userPrompt = configJson.getJSONObject("userPrompt");
            if(userPrompt.containsKey("userPrompt") && userPrompt.getJSONObject("userPrompt") != null) {
                builder.userPrompt(userPrompt.getString("userPrompt"));
            }
        }

        if (configJson.containsKey("resilience") && configJson.getJSONObject("resilience") != null
                && !configJson.getJSONObject("resilience").isEmpty()) {
            builder.resilience(ResilienceConfig.builder()
                    .timeoutMs(configJson.getLong("timeoutMs"))
                    .maxRetries(configJson.getInteger("maxRetries"))
                    .retryDelayMs(configJson.getLong("retryDelayMs"))
                    .retryMultiplier(configJson.getDouble("retryMultiplier"))
                    .maxRetryDelayMs(configJson.getLong("maxRetryDelayMs"))
                    .maxConcurrent(configJson.getInteger("maxConcurrent"))
                    .build());
        }

        Map<String, Object> customConfig = new HashMap<>();
        for (String key : configJson.keySet()) {
            if (!isStandardConfigKey(key)) {
                customConfig.put(key, configJson.get(key));
            }
        }
        if (!customConfig.isEmpty()) {
            builder.customConfig(customConfig);
        }

        return builder.build();
    }

    /**
     * 解析模型配置
     */
    private ModelConfig parseModelConfig(JSONObject modelJson) {
        if (modelJson == null) {
            return null;
        }

        return ModelConfig.builder()
                .baseUrl(modelJson.getString("baseUrl"))
                .apiKey(modelJson.getString("apiKey"))
                .modelName(modelJson.getString("modelName"))
                .temperature(modelJson.getDouble("temperature"))
                .maxTokens(modelJson.getInteger("maxTokens"))
                .topP(modelJson.getDouble("topP"))
                .frequencyPenalty(modelJson.getDouble("frequencyPenalty"))
                .presencePenalty(modelJson.getDouble("presencePenalty"))
                .build();
    }

    /**
     * 解析记忆配置
     */
    private MemoryConfig parseMemoryConfig(JSONObject memoryJson) {
        if (memoryJson == null) {
            return null;
        }

        MemoryConfig.MemoryConfigBuilder builder = MemoryConfig.builder()
                .enabled(memoryJson.getBoolean("enabled"))
                .retrieveSize(memoryJson.getInteger("retrieveSize"))
                .conversationId(memoryJson.getString("conversationId"));

        // 解析记忆类型
        String typeStr = memoryJson.getString("type");
        if (typeStr != null) {
            try {
                builder.type(MemoryConfig.MemoryType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown memory type: {}, using default", typeStr);
            }
        }

        return builder.build();
    }

    /**
     * 解析Advisor配置列表
     */
    private List<AdvisorConfig> parseAdvisorConfigs(Object advisorsObj) {
        if (advisorsObj == null) {
            return null;
        }

        List<AdvisorConfig> advisors = new ArrayList<>();
        List<Map<String, Object>> advisorList = (List<Map<String, Object>>) advisorsObj;

        for (Map<String, Object> advisorMap : advisorList) {
            AdvisorConfig config = AdvisorConfig.builder()
                    .advisorId((String) advisorMap.get("advisorId"))
                    .advisorType((String) advisorMap.get("advisorType"))
                    .config((Map<String, Object>) advisorMap.get("config"))
                    .build();
            advisors.add(config);
        }

        return advisors;
    }

    /**
     * 解析MCP工具配置列表
     */
    private List<McpToolConfig> parseMcpToolConfigs(Object mcpToolsObj) {
        if (mcpToolsObj == null) {
            return null;
        }

        List<McpToolConfig> mcpTools = new ArrayList<>();
        List<Map<String, String>> mcpList = (List<Map<String, String>>) mcpToolsObj;

        for (Map<String, String> mcpMap : mcpList) {
            McpToolConfig config = McpToolConfig.builder()
                    .mcpId(mcpMap.get("mcpId"))
                    .mcpName(mcpMap.get("mcpName"))
                    .mcpType(mcpMap.get("mcpType"))
                    .build();
            mcpTools.add(config);
        }

        return mcpTools;
    }

    /**
     * 解析人工介入配置
     */
    private HumanInterventionConfig parseHumanInterventionConfig(JSONObject hiJson) {
        if (hiJson == null || hiJson.isEmpty()) {
            return null;
        }

        HumanInterventionConfig.HumanInterventionConfigBuilder builder = HumanInterventionConfig.builder()
                .enabled(hiJson.getBoolean("enabled"))
                .checkMessage(hiJson.getString("checkMessage"))
                .allowModifyOutput(hiJson.getBoolean("allowModifyOutput"))
                .timeout(hiJson.getLong("timeout"));

        // 解析介入时机
        String timingStr = hiJson.getString("timing");
        if (timingStr != null) {
            try {
                builder.timing(HumanInterventionConfig.InterventionTiming.valueOf(timingStr));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown intervention timing: {}, using default AFTER", timingStr);
            }
        }

        return builder.build();
    }

    /**
     * 判断是否为标准配置键
     */
    private boolean isStandardConfigKey(String key) {
        return "systemPrompt".equals(key) ||
                "model".equals(key) ||
                "memory".equals(key) ||
                "advisors".equals(key) ||
                "mcpTools".equals(key) ||
                "timeout".equals(key);
    }

    /**
     * EndNode - 结束节点(简单实现)
     */
    private static class EndNode implements DagNode<DagExecutionContext, String> {
        private final String nodeId;
        private final String nodeName;

        public EndNode(String nodeId, String nodeName) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
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
        public java.util.Set<String> getDependencies() {
            return java.util.Collections.emptySet();
        }

        @Override
        public String execute(DagExecutionContext context) {
            log.info("DAG执行完成，节点: {}", nodeId);
            context.setValue("dag_completed", true);
            return "DAG_COMPLETED";
        }
    }
}
