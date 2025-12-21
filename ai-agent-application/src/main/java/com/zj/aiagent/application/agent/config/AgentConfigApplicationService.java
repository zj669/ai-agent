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
                default:
                    log.warn("未知的配置类型: {}", configType);
            }
        }

        return result;
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
            default:
                log.warn("未知的配置类型: {}", configType);
                throw new IllegalArgumentException("未知的配置类型: " + configType);
        }

        return result;
    }

    /**
     * 构建模型配置的字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildModelFieldDefinitions() {
        List<ConfigFieldDefinitionDTO> fields = new ArrayList<>();

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("baseUrl")
                .fieldLabel("API基础URL")
                .fieldType("text")
                .required(false)
                .description("模型API的基础URL地址")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("apiKey")
                .fieldLabel("API密钥")
                .fieldType("password")
                .required(false)
                .description("访问模型API的密钥")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("modelName")
                .fieldLabel("模型名称")
                .fieldType("text")
                .required(false)
                .description("使用的模型名称")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("temperature")
                .fieldLabel("温度参数")
                .fieldType("number")
                .required(false)
                .description("控制输出随机性，范围0.0-2.0")
                .defaultValue(0.7)
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("maxTokens")
                .fieldLabel("最大Token数")
                .fieldType("number")
                .required(false)
                .description("生成文本的最大token数量")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("topP")
                .fieldLabel("Top P参数")
                .fieldType("number")
                .required(false)
                .description("核采样参数，范围0.0-1.0")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("frequencyPenalty")
                .fieldLabel("频率惩罚")
                .fieldType("number")
                .required(false)
                .description("降低重复内容的惩罚系数")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("presencePenalty")
                .fieldLabel("存在惩罚")
                .fieldType("number")
                .required(false)
                .description("鼓励生成新内容的惩罚系数")
                .build());

        return fields;
    }

    /**
     * 构建Advisor配置的字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildAdvisorFieldDefinitions() {
        List<ConfigFieldDefinitionDTO> fields = new ArrayList<>();

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("advisorId")
                .fieldLabel("Advisor ID")
                .fieldType("text")
                .required(true)
                .description("Advisor的唯一标识")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("advisorType")
                .fieldLabel("Advisor类型")
                .fieldType("select")
                .required(true)
                .description("Advisor类型：MEMORY, RAG, TOOL, CUSTOM")
                .options(Arrays.asList("MEMORY", "RAG", "TOOL", "CUSTOM"))
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("config")
                .fieldLabel("配置参数")
                .fieldType("json")
                .required(false)
                .description("Advisor的配置参数，JSON格式")
                .build());

        return fields;
    }

    /**
     * 构建MCP工具配置的字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildMcpToolFieldDefinitions() {
        List<ConfigFieldDefinitionDTO> fields = new ArrayList<>();

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("mcpId")
                .fieldLabel("MCP工具ID")
                .fieldType("text")
                .required(true)
                .description("MCP工具的唯一标识")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("mcpName")
                .fieldLabel("MCP工具名称")
                .fieldType("text")
                .required(true)
                .description("MCP工具的名称")
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("mcpType")
                .fieldLabel("MCP工具类型")
                .fieldType("select")
                .required(true)
                .description("MCP工具类型")
                .options(Arrays.asList("FILE_SYSTEM", "CODE_EXECUTOR", "WEB_SEARCH", "DATABASE", "API_CLIENT"))
                .build());

        return fields;
    }

    /**
     * 构建Memory配置的字段定义
     */
    private List<ConfigFieldDefinitionDTO> buildMemoryFieldDefinitions() {
        List<ConfigFieldDefinitionDTO> fields = new ArrayList<>();

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("enabled")
                .fieldLabel("启用记忆")
                .fieldType("boolean")
                .required(false)
                .description("是否启用记忆功能")
                .defaultValue(false)
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("type")
                .fieldLabel("记忆类型")
                .fieldType("select")
                .required(false)
                .description("记忆的类型")
                .options(Arrays.asList("VECTOR_STORE", "CHAT_MEMORY", "SUMMARY_MEMORY", "BUFFER_MEMORY"))
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("retrieveSize")
                .fieldLabel("检索大小")
                .fieldType("number")
                .required(false)
                .description("从记忆中检索的条目数量")
                .defaultValue(10)
                .build());

        fields.add(ConfigFieldDefinitionDTO.builder()
                .fieldName("conversationId")
                .fieldLabel("会话ID")
                .fieldType("text")
                .required(false)
                .description("会话ID，支持占位符${conversationId}")
                .build());

        return fields;
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
