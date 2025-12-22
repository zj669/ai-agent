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
     * 返回 Object 类型因为 RouterNode 实现的是 ConditionalDagNode，不是 DagNode
     */
    public Object createNode(GraphJsonSchema.NodeDefinition nodeDef) {
        String nodeType = nodeDef.getNodeType();
        String nodeId = nodeDef.getNodeId();
        String nodeName = nodeDef.getNodeName();

        // 解析NodeConfig
        NodeConfig config = parseNodeConfig(nodeDef.getConfig());

        // 根据类型创建节点
        return switch (nodeType) {
            case "PLAN_NODE" -> new PlanNode(nodeId, nodeName, config, applicationContext);
            case "ACT_NODE" -> new ActNode(nodeId, nodeName, config, applicationContext);
            case "HUMAN_NODE" -> new HumanNode(nodeId, nodeName, config, applicationContext);
            case "ROUTER_NODE" -> new RouterNode(nodeId, nodeName, config, applicationContext);
            case "REACT_NODE" -> new ReactNode(nodeId, nodeName, config, applicationContext);
            default -> throw new NodeConfigException("Unknown node type: " + nodeType);
        };
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

        // 系统提示词(必填)
        String systemPrompt = configJson.getString("systemPrompt");
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            throw new NodeConfigException("systemPrompt is required");
        }
        builder.systemPrompt(systemPrompt);

        // 模型配置
        if (configJson.containsKey("model")) {
            builder.model(parseModelConfig(configJson.getJSONObject("model")));
        }

        // // 记忆配置
        // if (configJson.containsKey("memory")) {
        // builder.memory(parseMemoryConfig(configJson.getJSONObject("memory")));
        // }

        // Advisor配置
        if (configJson.containsKey("advisors")) {
            builder.advisors(parseAdvisorConfigs(configJson.getJSONArray("advisors")));
        }

        // MCP工具配置
        if (configJson.containsKey("mcpTools")) {
            builder.mcpTools(parseMcpToolConfigs(configJson.getJSONArray("mcpTools")));
        }

        // 超时时间
        if (configJson.containsKey("timeout")) {
            builder.timeout(configJson.getLong("timeout"));
        }

        // 自定义配置
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
