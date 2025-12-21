package com.zj.aiagent.infrastructure.agent.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.domain.agent.config.entity.AdvisorEntity;
import com.zj.aiagent.domain.agent.config.entity.McpToolEntity;
import com.zj.aiagent.domain.agent.config.entity.ModelEntity;
import com.zj.aiagent.domain.agent.config.repository.IAgentConfigRepository;
import com.zj.aiagent.infrastructure.persistence.entity.AiAdvisorPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiModelPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiToolMcpPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAdvisorMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiModelMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiToolMcpMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    private AiModelMapper aiModelMapper;

    @Resource
    private AiAdvisorMapper aiAdvisorMapper;

    @Resource
    private AiToolMcpMapper aiToolMcpMapper;

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
}
