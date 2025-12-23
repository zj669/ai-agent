package com.zj.aiagent.application.agent.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zj.aiagent.application.agent.config.dto.AdvisorDTO;
import com.zj.aiagent.application.agent.config.dto.McpToolDTO;
import com.zj.aiagent.application.agent.config.dto.ModelDTO;
import com.zj.aiagent.application.agent.config.dto.NodeTypeDTO;
import com.zj.aiagent.domain.agent.config.entity.*;
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
            // case HUMAN_NODE:
            // return "等待人工介入，需要用户确认或输入";
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
            // case HUMAN_NODE:
            // return "icon-human";
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
                return Arrays.asList("MODEL", "ADVISOR", "MCP_TOOL", "SYSTEM_PROMPT", "USER_PROMPT", "TIMEOUT",
                        "HUMAN_INTERVENTION");
            case ROUTER_NODE:
                return Arrays.asList("MODEL", "ROUTING_STRATEGY", "CANDIDATE_NODES", "TIMEOUT", "HUMAN_INTERVENTION");
            // case HUMAN_NODE:
            // return Arrays.asList("TIMEOUT", "PROMPT_MESSAGE");
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

    /**
     * 获取配置项定义
     * 
     * 返回每个配置项类型（MODEL、ADVISOR、MCP_TOOL、SYSTEM_PROMPT等）的可选值列表
     *
     * @param configType 配置项类型（可选），如MODEL、ADVISOR、MCP_TOOL、SYSTEM_PROMPT
     * @return 配置项定义列表
     */
    public List<ConfigDefinitionDTO> getConfigDefinitions(String configType) {
        log.info("查询配置项定义，配置类型: {}", configType);

        List<ConfigDefinitionDTO> result = new ArrayList<>();

        if (configType == null || configType.isEmpty()) {
            // 返回所有配置项类型的值
            result.add(buildModelConfigDefinition());
            result.add(buildAdvisorConfigDefinition());
            result.add(buildMcpToolConfigDefinition());
            result.add(buildSystemPromptConfigDefinition());
        } else {
            // 返回指定配置项类型的值
            switch (configType.toUpperCase()) {
                case "MODEL":
                    result.add(buildModelConfigDefinition());
                    break;
                case "ADVISOR":
                    result.add(buildAdvisorConfigDefinition());
                    break;
                case "MCP_TOOL":
                    result.add(buildMcpToolConfigDefinition());
                    break;
                case "SYSTEM_PROMPT":
                    result.add(buildSystemPromptConfigDefinition());
                    break;
                case "HUMAN_INTERVENTION":
                    result.add(buildHumanInterventionConfigDefinition());
                    break;
                default:
                    log.warn("未知的配置类型: {}", configType);
            }
        }

        return result;
    }

    private ConfigDefinitionDTO buildHumanInterventionConfigDefinition() {
        return ConfigDefinitionDTO.builder()
                .configType("HUMAN_INTERVENTION")
                .configName("人工介入")
                .options(new ArrayList<>())
                .build();
    }

    private ConfigDefinitionDTO buildModelConfigDefinition() {
        List<ModelEntity> models = agentConfigRepository.findAllModels();
        List<ConfigDefinitionDTO.ConfigOption> options = models.stream()
                .map(model -> ConfigDefinitionDTO.ConfigOption.builder()
                        .id(model.getModelId())
                        .name(model.getModelName())
                        .type(model.getModelType())
                        .build())
                .collect(Collectors.toList());

        return ConfigDefinitionDTO.builder()
                .configType("MODEL")
                .configName("模型配置")
                .options(options)
                .build();
    }

    private ConfigDefinitionDTO buildAdvisorConfigDefinition() {
        List<AdvisorEntity> advisors = agentConfigRepository.findAllAdvisors();
        List<ConfigDefinitionDTO.ConfigOption> options = advisors.stream()
                .map(advisor -> ConfigDefinitionDTO.ConfigOption.builder()
                        .id(advisor.getAdvisorId())
                        .name(advisor.getAdvisorName())
                        .type(advisor.getAdvisorType())
                        .build())
                .collect(Collectors.toList());

        return ConfigDefinitionDTO.builder()
                .configType("ADVISOR")
                .configName("Advisor配置")
                .options(options)
                .build();
    }

    private ConfigDefinitionDTO buildMcpToolConfigDefinition() {
        List<McpToolEntity> mcpTools = agentConfigRepository.findAllMcpTools();
        List<ConfigDefinitionDTO.ConfigOption> options = mcpTools.stream()
                .map(tool -> ConfigDefinitionDTO.ConfigOption.builder()
                        .id(tool.getMcpId())
                        .name(tool.getMcpName())
                        .type(tool.getTransportType())
                        .build())
                .collect(Collectors.toList());

        return ConfigDefinitionDTO.builder()
                .configType("MCP_TOOL")
                .configName("MCP工具配置")
                .options(options)
                .build();
    }

    private ConfigDefinitionDTO buildSystemPromptConfigDefinition() {
        List<com.zj.aiagent.domain.agent.config.entity.SystemPromptEntity> prompts = agentConfigRepository
                .findAllSystemPrompts();

        List<ConfigDefinitionDTO.ConfigOption> options = prompts.stream()
                .map(prompt -> ConfigDefinitionDTO.ConfigOption.builder()
                        .id(prompt.getPromptId())
                        .name(prompt.getPromptName())
                        .type("system_prompt")
                        .build())
                .collect(Collectors.toList());

        return ConfigDefinitionDTO.builder()
                .configType("SYSTEM_PROMPT")
                .configName("系统提示词配置")
                .options(options)
                .build();
    }

    /**
     * 获取配置字段属性定义
     * 
     * 返回指定配置类型的可自定义字段列表
     * 
     * @param configType 配置类型（MODEL、ADVISOR、MCP_TOOL、USER_PROMPT、TIMEOUT等）
     * @return 配置字段属性列表
     */
    public List<ConfigFieldDefinitionDTO> getConfigFieldDefinitions(String configType) {
        log.info("查询配置字段属性定义，配置类型: {}", configType);

        if (configType == null || configType.isEmpty()) {
            throw new IllegalArgumentException("配置类型不能为空");
        }

        List<ConfigFieldDefinitionDTO> result = new ArrayList<>();

        switch (configType.toUpperCase()) {
            case "MODEL":
                result = buildModelFieldDefinitions();
                break;
            case "ADVISOR":
                result = buildAdvisorFieldDefinitions();
                break;
            case "MCP_TOOL":
                result = buildMcpToolFieldDefinitions();
                break;
            case "MEMORY":
                result = buildMemoryFieldDefinitions();
                break;
            case "USER_PROMPT":
                result.add(ConfigFieldDefinitionDTO.builder()
                        .fieldName("userPrompt")
                        .fieldLabel("用户提示词")
                        .fieldType("textarea")
                        .required(false)
                        .description("自定义的用户提示词内容")
                        .build());
                break;
            case "TIMEOUT":
                result.add(ConfigFieldDefinitionDTO.builder()
                        .fieldName("timeout")
                        .fieldLabel("超时时间")
                        .fieldType("number")
                        .required(false)
                        .description("节点执行超时时间(毫秒)")
                        .build());
                break;
            case "HUMAN_INTERVENTION":
                result = buildHumanInterventionFieldDefinitions();
                break;
            default:
                log.warn("未知的配置类型: {}", configType);
                throw new IllegalArgumentException("未知的配置类型: " + configType);
        }

        return result;

    }

    /**
     * 构建人工介入配置的字段定义
     * 从数据库读取配置字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildHumanInterventionFieldDefinitions() {
        // 从数据库查询配置字段定义
        List<ConfigFieldDefinitionEntity> entities = agentConfigRepository
                .findConfigFieldDefinitions("HUMAN_INTERVENTION");

        // 转换为 DTO
        return entities.stream()
                .map(entity -> {
                    ConfigFieldDefinitionDTO.ConfigFieldDefinitionDTOBuilder builder = ConfigFieldDefinitionDTO
                            .builder()
                            .fieldName(entity.getFieldName())
                            .fieldLabel(entity.getFieldLabel())
                            .fieldType(entity.getFieldType())
                            .required(entity.getRequired())
                            .description(entity.getDescription());

                    // 处理默认值
                    if (entity.getDefaultValue() != null && !entity.getDefaultValue().isEmpty()) {
                        // 根据字段类型转换默认值
                        Object defaultValue = convertDefaultValue(entity.getFieldType(), entity.getDefaultValue());
                        builder.defaultValue(defaultValue);
                    }

                    // 处理选项列表
                    if (entity.getOptions() != null && !entity.getOptions().isEmpty()) {
                        List<String> options = entity.getParsedOptions();
                        builder.options(options);
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据字段类型转换默认值
     */
    private Object convertDefaultValue(String fieldType, String defaultValueStr) {
        if (defaultValueStr == null || defaultValueStr.isEmpty()) {
            return null;
        }

        try {
            switch (fieldType.toLowerCase()) {
                case "boolean":
                    return Boolean.parseBoolean(defaultValueStr);
                case "number":
                    return Double.parseDouble(defaultValueStr);
                default:
                    return defaultValueStr;
            }
        } catch (Exception e) {
            log.warn("转换默认值失败: fieldType={}, value={}", fieldType, defaultValueStr, e);
            return defaultValueStr;
        }
    }

    /**
     * 构建模型配置的字段定义
     * 从数据库读取配置字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildModelFieldDefinitions() {
        return buildFieldDefinitionsFromDatabase("MODEL");
    }

    /**
     * 构建Advisor配置的字段定义
     * 从数据库读取配置字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildAdvisorFieldDefinitions() {
        return buildFieldDefinitionsFromDatabase("ADVISOR");
    }

    /**
     * 构建MCP工具配置的字段定义
     * 从数据库读取配置字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildMcpToolFieldDefinitions() {
        return buildFieldDefinitionsFromDatabase("MCP_TOOL");
    }

    /**
     * 构建Memory配置的字段定义
     * 从数据库读取配置字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildMemoryFieldDefinitions() {
        return buildFieldDefinitionsFromDatabase("MEMORY");
    }

    /**
     * 通用方法：从数据库读取配置字段定义并转换为DTO
     * 
     * @param configType 配置类型
     * @return 配置字段定义DTO列表
     */
    private List<ConfigFieldDefinitionDTO> buildFieldDefinitionsFromDatabase(String configType) {
        // 从数据库查询配置字段定义
        List<ConfigFieldDefinitionEntity> entities = agentConfigRepository.findConfigFieldDefinitions(configType);

        // 转换为 DTO
        return entities.stream()
                .map(entity -> {
                    ConfigFieldDefinitionDTO.ConfigFieldDefinitionDTOBuilder builder = ConfigFieldDefinitionDTO
                            .builder()
                            .fieldName(entity.getFieldName())
                            .fieldLabel(entity.getFieldLabel())
                            .fieldType(entity.getFieldType())
                            .required(entity.getRequired())
                            .description(entity.getDescription());

                    // 处理默认值
                    if (entity.getDefaultValue() != null && !entity.getDefaultValue().isEmpty()) {
                        // 根据字段类型转换默认值
                        Object defaultValue = convertDefaultValue(entity.getFieldType(), entity.getDefaultValue());
                        builder.defaultValue(defaultValue);
                    }

                    // 处理选项列表
                    if (entity.getOptions() != null && !entity.getOptions().isEmpty()) {
                        List<String> options = entity.getParsedOptions();
                        builder.options(options);
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConfigDefinitionDTO {
        private String configType;
        private String configName;
        private List<ConfigOption> options;

        @lombok.Data
        @lombok.Builder
        @lombok.NoArgsConstructor
        @lombok.AllArgsConstructor
        public static class ConfigOption {
            private String id;
            private String name;
            private String type;
            private Map<String, Object> extra;
        }
    }

    /**
     * 配置字段属性定义DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ConfigFieldDefinitionDTO {
        /**
         * 字段名称
         */
        private String fieldName;

        /**
         * 字段标签（显示名称）
         */
        private String fieldLabel;

        /**
         * 字段类型（text, number, boolean, select, textarea, json, password等）
         */
        private String fieldType;

        /**
         * 是否必填
         */
        private Boolean required;

        /**
         * 字段描述
         */
        private String description;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 可选项（针对select类型）
         */
        private List<String> options;
    }
}
