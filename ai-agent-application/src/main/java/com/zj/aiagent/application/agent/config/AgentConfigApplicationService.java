package com.zj.aiagent.application.agent.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zj.aiagent.application.agent.config.dto.AdvisorDTO;
import com.zj.aiagent.application.agent.config.dto.McpToolDTO;
import com.zj.aiagent.application.agent.config.dto.ModelDTO;
import com.zj.aiagent.application.agent.config.dto.NodeTypeDTO;
import com.zj.aiagent.domain.agent.config.entity.AdvisorEntity;
import com.zj.aiagent.domain.agent.config.entity.McpToolEntity;
import com.zj.aiagent.domain.agent.config.entity.ModelEntity;
import com.zj.aiagent.domain.agent.config.entity.NodeTemplateEntity;
import com.zj.aiagent.domain.agent.config.repository.IAgentConfigRepository;
import com.zj.aiagent.domain.agent.dag.entity.NodeType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent 配置应用服务
 *
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
public class AgentConfigApplicationService {

    @Resource
    private IAgentConfigRepository agentConfigRepository;

    /**
     * 获取所有节点类型
     * 
     * 从数据库的 ai_node_template 表读取节点配置信息
     *
     * @return 节点类型列表
     */
    public List<NodeTypeDTO> getNodeTypes() {
        log.info("查询所有节点类型");

        // 从数据库查询节点模板
        List<NodeTemplateEntity> nodeTemplates = agentConfigRepository.findAllNodeTemplates();

        // 创建节点模板映射，方便根据节点类型查找
        Map<String, NodeTemplateEntity> templateMap = nodeTemplates.stream()
                .collect(Collectors.toMap(
                        NodeTemplateEntity::getNodeType,
                        entity -> entity,
                        (existing, replacement) -> existing // 如果有重复，保留第一个
                ));

        // 将枚举类型转换为 DTO，并从数据库获取配置信息
        return Arrays.stream(NodeType.values())
                .map(nodeType -> convertToNodeTypeDTO(nodeType, templateMap.get(nodeType.name())))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有模型
     *
     * @return 模型列表
     */
    public List<ModelDTO> getModels() {
        log.info("查询所有启用的模型");

        List<ModelEntity> modelEntities = agentConfigRepository.findAllModels();

        return modelEntities.stream()
                .map(this::convertToModelDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有 Advisor
     *
     * @return Advisor 列表
     */
    public List<AdvisorDTO> getAdvisors() {
        log.info("查询所有启用的 Advisor");

        List<AdvisorEntity> advisorEntities = agentConfigRepository.findAllAdvisors();

        return advisorEntities.stream()
                .map(this::convertToAdvisorDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有 MCP 工具
     *
     * @return MCP 工具列表
     */
    public List<McpToolDTO> getMcpTools() {
        log.info("查询所有启用的 MCP 工具");

        List<McpToolEntity> mcpToolEntities = agentConfigRepository.findAllMcpTools();

        return mcpToolEntities.stream()
                .map(this::convertToMcpToolDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换节点类型枚举到 DTO
     * 
     * @param nodeType 节点类型枚举
     * @param template 数据库中的节点模板（可能为 null）
     */
    private NodeTypeDTO convertToNodeTypeDTO(NodeType nodeType, NodeTemplateEntity template) {
        // 如果数据库中有配置，优先使用数据库配置，否则使用默认值
        String description = (template != null && StringUtils.hasText(template.getDescription()))
                ? template.getDescription()
                : getDefaultDescription(nodeType);

        String icon = (template != null && StringUtils.hasText(template.getIcon()))
                ? template.getIcon()
                : getDefaultIcon(nodeType);

        List<String> supportedConfigs = (template != null && StringUtils.hasText(template.getConfigSchema()))
                ? parseSupportedConfigs(template.getConfigSchema())
                : getDefaultSupportedConfigs(nodeType);

        return NodeTypeDTO.builder()
                .nodeType(nodeType.name())
                .nodeTypeValue(nodeType.getValue())
                .nodeName(nodeType.getLabel())
                .description(description)
                .icon(icon)
                .supportedConfigs(supportedConfigs)
                .build();
    }

    /**
     * 从 configSchema JSON 解析支持的配置项
     * 
     * configSchema 格式示例：
     * {
     * "supportedConfigs": ["MODEL", "ADVISOR", "MCP_TOOL", "SYSTEM_PROMPT"]
     * }
     */
    private List<String> parseSupportedConfigs(String configSchema) {
        try {
            if (!StringUtils.hasText(configSchema)) {
                return Collections.emptyList();
            }

            JSONObject json = JSON.parseObject(configSchema);
            if (json != null && json.containsKey("supportedConfigs")) {
                return json.getList("supportedConfigs", String.class);
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("解析 configSchema 失败: {}", configSchema, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取默认描述（如果数据库中没有配置）
     */
    private String getDefaultDescription(NodeType nodeType) {
        switch (nodeType) {
            case ACT_NODE:
                return "精准执行特定任务，不进行额外推理";
            case PLAN_NODE:
                return "分析用户任务，制定执行计划，评估任务复杂度";
            case REACT_NODE:
                return "Reasoning-Acting-Observing 循环，支持多轮推理";
            case ROUTER_NODE:
                return "根据条件选择下一个执行节点，支持AI评估和规则表达式";
            case HUMAN_NODE:
                return "等待人工介入，需要用户确认或输入";
            default:
                return "";
        }
    }

    /**
     * 获取默认图标（如果数据库中没有配置）
     */
    private String getDefaultIcon(NodeType nodeType) {
        switch (nodeType) {
            case ACT_NODE:
                return "icon-act";
            case PLAN_NODE:
                return "icon-plan";
            case REACT_NODE:
                return "icon-react";
            case ROUTER_NODE:
                return "icon-router";
            case HUMAN_NODE:
                return "icon-human";
            default:
                return "icon-default";
        }
    }

    /**
     * 获取默认支持的配置项（如果数据库中没有配置）
     */
    private List<String> getDefaultSupportedConfigs(NodeType nodeType) {
        switch (nodeType) {
            case ACT_NODE:
            case PLAN_NODE:
            case REACT_NODE:
                return Arrays.asList("MODEL", "ADVISOR", "MCP_TOOL", "SYSTEM_PROMPT", "USER_PROMPT", "TIMEOUT");
            case ROUTER_NODE:
                return Arrays.asList("MODEL", "ROUTING_STRATEGY", "CANDIDATE_NODES", "TIMEOUT");
            case HUMAN_NODE:
                return Arrays.asList("TIMEOUT", "PROMPT_MESSAGE");
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 转换模型实体到 DTO
     */
    private ModelDTO convertToModelDTO(ModelEntity entity) {
        return ModelDTO.builder()
                .id(entity.getId())
                .modelId(entity.getModelId())
                .modelName(entity.getModelName())
                .modelType(entity.getModelType())
                .apiId(entity.getApiId())
                .status(entity.getStatus())
                .build();
    }

    /**
     * 转换 Advisor 实体到 DTO
     */
    private AdvisorDTO convertToAdvisorDTO(AdvisorEntity entity) {
        return AdvisorDTO.builder()
                .id(entity.getId())
                .advisorId(entity.getAdvisorId())
                .advisorName(entity.getAdvisorName())
                .advisorType(entity.getAdvisorType())
                .orderNum(entity.getOrderNum())
                .extParam(entity.getExtParam())
                .status(entity.getStatus())
                .build();
    }

    /**
     * 转换 MCP 工具实体到 DTO
     */
    private McpToolDTO convertToMcpToolDTO(McpToolEntity entity) {
        return McpToolDTO.builder()
                .id(entity.getId())
                .mcpId(entity.getMcpId())
                .mcpName(entity.getMcpName())
                .transportType(entity.getTransportType())
                .transportConfig(entity.getTransportConfig())
                .requestTimeout(entity.getRequestTimeout())
                .status(entity.getStatus())
                .build();
    }
}
