package com.zj.aiagent.infrastructure.agent.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.agent.config.entity.*;
import com.zj.aiagent.domain.agent.config.repository.IAgentConfigRepository;
import com.zj.aiagent.infrastructure.persistence.entity.AiAdvisorPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiApiPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiModelPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;
import com.zj.aiagent.infrastructure.persistence.entity.AiSystemPromptPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiToolMcpPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAdvisorMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiApiMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiModelMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiNodeTemplateMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiSystemPromptMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiToolMcpMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 配置仓储实现
 *
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Repository
public class AgentConfigRepositoryImpl implements IAgentConfigRepository {

    @Resource
    private AiNodeTemplateMapper aiNodeTemplateMapper;

    @Resource
    private AiModelMapper aiModelMapper;

    @Resource
    private AiAdvisorMapper aiAdvisorMapper;

    @Resource
    private AiToolMcpMapper aiToolMcpMapper;

    @Resource
    private AiSystemPromptMapper aiSystemPromptMapper;

    @Resource
    private AiApiMapper aiApiMapper;

    @Override
    public List<NodeTemplateEntity> findAllNodeTemplates() {
        log.debug("查询所有节点模板");

        List<AiNodeTemplatePO> nodeTemplatePOList = aiNodeTemplateMapper.selectList(null);

        return nodeTemplatePOList.stream()
                .map(this::convertToNodeTemplateEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ModelEntity> findAllModels() {
        log.debug("查询所有启用的模型");

        LambdaQueryWrapper<AiModelPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModelPO::getStatus, 1); // 只查询启用状态的模型

        List<AiModelPO> modelPOList = aiModelMapper.selectList(queryWrapper);

        return modelPOList.stream()
                .map(this::convertToModelEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<AdvisorEntity> findAllAdvisors() {
        log.debug("查询所有启用的 Advisor");

        LambdaQueryWrapper<AiAdvisorPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiAdvisorPO::getStatus, 1); // 只查询启用状态的 Advisor
        queryWrapper.orderByAsc(AiAdvisorPO::getOrderNum); // 按顺序号排序

        List<AiAdvisorPO> advisorPOList = aiAdvisorMapper.selectList(queryWrapper);

        return advisorPOList.stream()
                .map(this::convertToAdvisorEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<McpToolEntity> findAllMcpTools() {
        log.debug("查询所有启用的 MCP 工具");

        LambdaQueryWrapper<AiToolMcpPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiToolMcpPO::getStatus, 1); // 只查询启用状态的 MCP 工具

        List<AiToolMcpPO> mcpToolPOList = aiToolMcpMapper.selectList(queryWrapper);

        return mcpToolPOList.stream()
                .map(this::convertToMcpToolEntity)
                .collect(Collectors.toList());
    }

    /**
     * 转换节点模板 PO 到实体
     */
    private NodeTemplateEntity convertToNodeTemplateEntity(AiNodeTemplatePO po) {
        return NodeTemplateEntity.builder()
                .id(po.getId())
                .nodeType(po.getNodeType())
                .nodeName(po.getNodeName())
                .description(po.getDescription())
                .icon(po.getIcon())
                .defaultSystemPrompt(po.getDefaultSystemPrompt())
                .configSchema(po.getConfigSchema())
                .build();
    }

    /**
     * 转换模型 PO 到实体
     */
    private ModelEntity convertToModelEntity(AiModelPO po) {
        return ModelEntity.builder()
                .id(po.getId())
                .modelId(po.getModelId())
                .modelName(po.getModelName())
                .modelType(po.getModelType())
                .apiId(po.getApiId())
                .status(po.getStatus())
                .build();
    }

    /**
     * 转换 Advisor PO 到实体
     */
    private AdvisorEntity convertToAdvisorEntity(AiAdvisorPO po) {
        return AdvisorEntity.builder()
                .id(po.getId())
                .advisorId(po.getAdvisorId())
                .advisorName(po.getAdvisorName())
                .advisorType(po.getAdvisorType())
                .orderNum(po.getOrderNum())
                .extParam(po.getExtParam())
                .status(po.getStatus())
                .build();
    }

    /**
     * 转换 MCP 工具 PO 到实体
     */
    private McpToolEntity convertToMcpToolEntity(AiToolMcpPO po) {
        return McpToolEntity.builder()
                .id(po.getId())
                .mcpId(po.getMcpId())
                .mcpName(po.getMcpName())
                .transportType(po.getTransportType())
                .transportConfig(po.getTransportConfig())
                .requestTimeout(po.getRequestTimeout())
                .status(po.getStatus())
                .build();
    }

    @Override
    public SystemPromptEntity findSystemPromptByPromptId(String promptId) {
        log.debug("根据promptId查询系统提示词: {}", promptId);

        LambdaQueryWrapper<AiSystemPromptPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiSystemPromptPO::getPromptId, promptId);
        queryWrapper.eq(AiSystemPromptPO::getStatus, 1); // 只查询启用状态的提示词

        AiSystemPromptPO promptPO = aiSystemPromptMapper.selectOne(queryWrapper);

        if (promptPO == null) {
            log.warn("未找到promptId对应的系统提示词: {}", promptId);
            return null;
        }

        return convertToSystemPromptEntity(promptPO);
    }

    /**
     * 转换系统提示词 PO 到实体
     */
    private SystemPromptEntity convertToSystemPromptEntity(AiSystemPromptPO po) {
        return SystemPromptEntity.builder()
                .promptId(po.getPromptId())
                .promptName(po.getPromptName())
                .promptContent(po.getPromptContent())
                .description(po.getDescription())
                .status(po.getStatus())
                .build();
    }

    @Override
    public Map<String, Object> findModelWithApiByModelId(String modelId) {
        log.debug("根据modelId查询model配置: {}", modelId);

        // 查询model
        LambdaQueryWrapper<AiModelPO> modelQuery = new LambdaQueryWrapper<>();
        modelQuery.eq(AiModelPO::getModelId, modelId);
        modelQuery.eq(AiModelPO::getStatus, 1);
        AiModelPO modelPO = aiModelMapper.selectOne(modelQuery);

        if (modelPO == null) {
            log.warn("未找到modelId对应的model配置: {}", modelId);
            return null;
        }

        // 查询关联的API配置
        LambdaQueryWrapper<AiApiPO> apiQuery = new LambdaQueryWrapper<>();
        apiQuery.eq(AiApiPO::getApiId, modelPO.getApiId());
        apiQuery.eq(AiApiPO::getStatus, 1);
        AiApiPO apiPO = aiApiMapper.selectOne(apiQuery);

        if (apiPO == null) {
            log.warn("未找到model关联的API配置: modelId={}, apiId={}", modelId, modelPO.getApiId());
            return null;
        }

        // 构建返回Map
        Map<String, Object> modelConfig = new HashMap<>();
        modelConfig.put("modelName", modelPO.getModelName());
        modelConfig.put("modelType", modelPO.getModelType());
        modelConfig.put("baseUrl", apiPO.getBaseUrl());
        modelConfig.put("apiKey", apiPO.getApiKey());
        modelConfig.put("completionsPath", apiPO.getCompletionsPath());
        modelConfig.put("embeddingsPath", apiPO.getEmbeddingsPath());

        log.debug("查询到model配置: {}", modelConfig);
        return modelConfig;
    }

    @Override
    public NodeTemplateEntity findNodeTemplateByNodeType(String nodeType) {
        log.debug("根据nodeType查询节点模板: {}", nodeType);

        LambdaQueryWrapper<AiNodeTemplatePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiNodeTemplatePO::getNodeType, nodeType);

        AiNodeTemplatePO templatePO = aiNodeTemplateMapper.selectOne(queryWrapper);

        if (templatePO == null) {
            log.warn("未找到nodeType对应的节点模板: {}", nodeType);
            return null;
        }

        return convertToNodeTemplateEntity(templatePO);
    }
}
