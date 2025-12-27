package com.zj.aiagent.infrastructure.parse;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.model.parse.ModelConfigParseFactory;
import com.zj.aiagent.domain.model.parse.entity.ModelConfigEntity;
import com.zj.aiagent.domain.model.parse.entity.ModelConfigResult;
import com.zj.aiagent.domain.prompt.parse.PromptConfigParseFactory;
import com.zj.aiagent.domain.prompt.parse.entity.PromptConfigResult;
import com.zj.aiagent.domain.toolbox.parse.McpConfigParseFactory;
import com.zj.aiagent.domain.toolbox.parse.entity.McpConfigResult;
import com.zj.aiagent.domain.workflow.entity.EdgeDefinitionEntity;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.infrastructure.parse.adpater.NodeExecutorFactory;
import com.zj.aiagent.infrastructure.parse.convert.ConfigConvert;
import com.zj.aiagent.infrastructure.parse.entity.GraphJsonSchema;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiApiPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiModelPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiNodeTemplateMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentRepository;
import com.zj.aiagent.infrastructure.persistence.repository.IAiApiRepository;
import com.zj.aiagent.infrastructure.persistence.repository.IAiModelRepository;
import com.zj.aiagent.shared.design.workflow.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Repository
public class WorkflowGraphFactory {
    private final IAiAgentRepository agentRepository;
    private final McpConfigParseFactory mcpConfigParseFactory;
    private final PromptConfigParseFactory promptConfigParseFactory;
    private final ModelConfigParseFactory modelConfigParseFactory;
    private final NodeExecutorFactory nodeExecutorFactory;
    private final AiNodeTemplateMapper nodeTemplateMapper;
    private final IAiModelRepository aiModelRepository;
    private final IAiApiRepository aiApiRepository;

    public WorkflowGraph loadDagByAgentId(String agentId) {
        Long agentIdLong = Long.parseLong(agentId);
        AiAgentPO aiAgent = agentRepository.getById(agentIdLong);
        if (aiAgent == null) {
            throw new RuntimeException("Agent not found: agentId=" + agentId);
        }

        if (aiAgent.getGraphJson() == null || aiAgent.getGraphJson().isEmpty()) {
            throw new RuntimeException("Agent graph_json is empty: agentId=" + agentId);
        }

        return loadDagFromJson(aiAgent.getGraphJson());
    }

    public WorkflowGraph loadDagFromJson(String graphJson) {
        try {
            log.info("开始解析graph_json");

            // 解析JSON
            GraphJsonSchema schema = JSON.parseObject(graphJson, GraphJsonSchema.class);

            if (schema == null || schema.getNodes() == null || schema.getNodes().isEmpty()) {
                throw new RuntimeException("Invalid graph_json: nodes cannot be empty");
            }

            // 构建节点映射和配置映射
            Map<String, NodeExecutor> nodeMap = new HashMap<>();
            Map<String, JSONObject> nodeConfigMap = new HashMap<>();

            for (GraphJsonSchema.NodeDefinition nodeDef : schema.getNodes()) {
                NodeExecutor node;
                JSONObject nodeConfig;

                // 【新逻辑】优先使用 templateId
                if (StringUtils.hasText(nodeDef.getTemplateId())) {
                    log.info("使用模板创建节点: templateId={}, nodeId={}",
                            nodeDef.getTemplateId(), nodeDef.getNodeId());

                    // 加载模板并合并配置
                    AiNodeTemplatePO template = loadTemplate(nodeDef.getTemplateId());
                    nodeConfig = mergeUserConfigToJson(template, nodeDef.getUserConfig());

                    // 创建节点
                    node = createNodeFromTemplate(nodeDef, template, nodeConfig);
                } else if (StringUtils.hasText(nodeDef.getConfig())) {
                    // 【兼容逻辑】回退到旧的 config 模式
                    log.warn("使用旧版 config 创建节点: nodeId={} (建议升级到 templateId)",
                            nodeDef.getNodeId());

                    // 旧版配置直接解析
                    nodeConfig = JSON.parseObject(nodeDef.getConfig());
                    node = createNodeFromLegacyConfig(nodeDef);
                } else {
                    throw new RuntimeException("节点配置无效: 必须指定 templateId 或 config");
                }

                nodeMap.put(nodeDef.getNodeId(), node);
                nodeConfigMap.put(nodeDef.getNodeId(), nodeConfig);

                log.info("节点创建成功: {} ({}) - 类型: {}",
                        nodeDef.getNodeName(), nodeDef.getNodeId(), nodeDef.getNodeType());
            }

            // 构建依赖关系
            Map<String, List<String>> dependencies = buildDependencies(schema.getEdges());

            // 构建DAG图
            WorkflowGraph dagGraph = WorkflowGraph.builder()
                    .dagId(schema.getDagId())
                    .nodes(nodeMap)
                    .nodeConfigs(nodeConfigMap) // 新增：保存节点配置
                    .edges(convert(schema.getEdges()))
                    .dependencies(dependencies)
                    .startNodeId(schema.getStartNodeId())
                    .build();

            log.info("DAG加载完成: dagId={}, 节点数={}", schema.getDagId(), nodeMap.size());

            return dagGraph;

        } catch (Exception e) {
            throw new RuntimeException("Failed to load DAG from JSON: " + e.getMessage(), e);
        }
    }

    private List<EdgeDefinitionEntity> convert(List<GraphJsonSchema.EdgeDefinition> edges) {
        List<EdgeDefinitionEntity> edgeDefinitionEntities = new ArrayList<>();
        for (GraphJsonSchema.EdgeDefinition edge : edges) {
            edgeDefinitionEntities.add(new EdgeDefinitionEntity(edge.getEdgeId(), edge.getSource(), edge.getTarget(),
                    edge.getCondition(), edge.getEdgeType().name()));
        }
        return edgeDefinitionEntities;
    }

    private Map<String, List<String>> buildDependencies(List<GraphJsonSchema.EdgeDefinition> edges) {
        Map<String, List<String>> dependencies = new HashMap<>();
        if (edges != null) {
            for (GraphJsonSchema.EdgeDefinition edge : edges) {
                dependencies.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>())
                        .add(edge.getSource());
            }
        }

        return dependencies;
    }

    /**
     * 从模板创建节点（新逻辑）
     */
    private NodeExecutor createNodeFromTemplate(GraphJsonSchema.NodeDefinition nodeDef,
            AiNodeTemplatePO template,
            JSONObject mergedConfig) {
        // 1. 构建临时 NodeDefinition（用于配置转换）
        GraphJsonSchema.NodeDefinition tempNodeDef = GraphJsonSchema.NodeDefinition.builder()
                .nodeId(nodeDef.getNodeId())
                .nodeType(nodeDef.getNodeType())
                .nodeName(nodeDef.getNodeName())
                .config(mergedConfig.toJSONString())
                .build();

        // 2. 解析配置
        PromptConfigResult promptConfigResult = promptConfigParseFactory.parseConfig(
                buildPromptConfigEntity(template, mergedConfig));
        ModelConfigResult modelConfigResult = modelConfigParseFactory.parseConfig(
                buildModelConfigEntity(template, mergedConfig));

        // 3. 创建节点
        return nodeExecutorFactory.createNodeExecutor(
                nodeDef.getNodeType(),
                nodeDef.getNodeId(),
                nodeDef.getNodeName(),
                template.getDescription(),
                modelConfigResult.getChatModel(),
                promptConfigResult.getPrompt());
    }

    private ModelConfigEntity buildModelConfigEntity( AiNodeTemplatePO template, JSONObject mergedConfig) {
        JSONObject modelConfig = new JSONObject();
        if(mergedConfig.containsKey("MODEL")){
            modelConfig.put("baseUrl", mergedConfig.getJSONObject("MODEL").getString("baseUrl"));
            modelConfig.put("apiKey", mergedConfig.getJSONObject("MODEL").getString("apiKey"));
            modelConfig.put("modelName", mergedConfig.getJSONObject("MODEL").getInteger("modelName"));
        }else{
            AiModelPO aiModelPO = aiModelRepository.getById(template.getModelId());
            AiApiPO aiApiPO = aiApiRepository.getById(aiModelPO.getApiId());
            modelConfig.put("baseUrl", aiApiPO.getBaseUrl());
            modelConfig.put("apiKey", aiApiPO.getApiKey());
            modelConfig.put("modelName", aiModelPO.getModelName());
        }

        return ModelConfigEntity.builder()
                .config(modelConfig.toJSONString())
                .build();

    }

    /**
     * 从旧版 config 创建节点（兼容逻辑）
     */
    private NodeExecutor createNodeFromLegacyConfig(GraphJsonSchema.NodeDefinition nodeDef) {
        // 解析节点配置
        McpConfigResult mcpConfigResult = mcpConfigParseFactory.parseConfig(
                ConfigConvert.convertMcp(nodeDef));
        PromptConfigResult promptConfigResult = promptConfigParseFactory.parseConfig(
                ConfigConvert.convertPrompt(nodeDef));
        ModelConfigResult modelConfigResult = modelConfigParseFactory.parseConfig(
                ConfigConvert.convertModel(nodeDef));

        // 使用工厂创建节点
        return nodeExecutorFactory.createNodeExecutor(
                nodeDef.getNodeType(),
                nodeDef.getNodeId(),
                nodeDef.getNodeName(),
                nodeDef.getNodeName(), // 描述(暂用 nodeName)
                modelConfigResult.getChatModel(),
                promptConfigResult.getPrompt());
    }

    /**
     * 加载节点模板
     */
    private AiNodeTemplatePO loadTemplate(String templateId) {
        LambdaQueryWrapper<AiNodeTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiNodeTemplatePO::getTemplateId, templateId);

        AiNodeTemplatePO template = nodeTemplateMapper.selectOne(wrapper);
        if (template == null) {
            throw new RuntimeException("模板不存在: templateId=" + templateId);
        }

        if (Boolean.TRUE.equals(template.getIsDeprecated())) {
            log.warn("使用已废弃的模板: {}", templateId);
        }

        return template;
    }

    /**
     * 合并用户配置到模板配置
     *
     * @param template   模板
     * @param userConfig 用户配置（JSON字符串，模块化结构）
     * @return 合并后的配置（JSONObject）
     */
    private JSONObject mergeUserConfigToJson(AiNodeTemplatePO template, String userConfig) {
        JSONObject mergedConfig = new JSONObject();

        // 解析用户配置
        if (StringUtils.hasText(userConfig)) {
            try {
                JSONObject userConfigJson = JSON.parseObject(userConfig);

                // 合并各个模块的配置
                if (userConfigJson.containsKey("MCP_TOOL")) {
                    mergedConfig.put("MCP_TOOL", userConfigJson.getJSONObject("MCP_TOOL"));
                }
                if (userConfigJson.containsKey("MODEL")) {
                    mergedConfig.put("MODEL", userConfigJson.getJSONObject("MODEL"));
                }
                if (userConfigJson.containsKey("HUMAN_INTERVENTION")) {
                    mergedConfig.put("HUMAN_INTERVENTION", userConfigJson.getJSONObject("HUMAN_INTERVENTION"));
                }
                // 其他模块...

            } catch (Exception e) {
                log.warn("解析 userConfig 失败: {}", e.getMessage());
            }
        }

        return mergedConfig;
    }

    /**
     * 构建 Prompt 配置实体
     */
    private com.zj.aiagent.domain.prompt.parse.entity.PromptConfigEntity buildPromptConfigEntity(
            AiNodeTemplatePO template, JSONObject mergedConfig) {

        JSONObject promptConfig = new JSONObject();

        // 使用模板的 systemPromptTemplate
        promptConfig.put("systemPrompt", template.getSystemPromptTemplate());

        // 合并用户的 userPrompt 配置
        if (mergedConfig.containsKey("userPrompt")) {
            JSONObject userPrompt = mergedConfig.getJSONObject("userPrompt");
            // 如果有自定义的 prompt 字段，可以覆盖
            if (userPrompt.containsKey("customPrompt")) {
                promptConfig.put("systemPrompt", userPrompt.getString("customPrompt"));
            }
            // 合并其他用户自定义字段
            promptConfig.putAll(userPrompt);
        }

        return com.zj.aiagent.domain.prompt.parse.entity.PromptConfigEntity.builder()
                .config(promptConfig.toJSONString())
                .build();
    }

}
