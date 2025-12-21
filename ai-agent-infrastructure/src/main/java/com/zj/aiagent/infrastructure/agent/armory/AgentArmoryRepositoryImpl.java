package com.zj.aiagent.infrastructure.agent.armory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.agent.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagent.domain.agent.armory.model.AiToolMcpVO;
import com.zj.aiagent.domain.agent.armory.repository.IAgentArmoryRepository;
import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiToolMcpPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentMapper;
import com.zj.aiagent.infrastructure.persistence.mapper.AiToolMcpMapper;
import com.zj.aiagent.shared.model.enums.AiAgentEnumVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent武器库仓储实现
 */
@Repository
@Slf4j
public class AgentArmoryRepositoryImpl implements IAgentArmoryRepository {

    @Resource
    private AiAgentMapper aiAgentMapper;

    @Resource
    private AiToolMcpMapper aiToolMcpMapper;

    /**
     * 查询用户的Agent列表
     */
    @Override
    public List<AiAgent> queryAgentDtoList(Long userId) {
        if (userId == null) {
            return List.of();
        }

        // 查询持久层对象
        List<AiAgentPO> agentPOList = aiAgentMapper.selectList(
                new LambdaQueryWrapper<AiAgentPO>()
                        .eq(AiAgentPO::getUserId, userId));

        // 转换为领域实体
        return agentPOList.stream()
                .map(this::convertToEntity)
                .toList();
    }

    /**
     * PO转换为领域实体
     */
    private AiAgent convertToEntity(AiAgentPO po) {
        return AiAgent.builder()
                .userId(po.getUserId())
                .agentName(po.getAgentName())
                .description(po.getDescription())
                .graphJson(po.getGraphJson())
                .status(po.getStatus())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    /**
     * 查询MCP工具配置
     */
    @Override
    public void queryMcps(DefaultAgentArmoryFactory.DynamicContext context) {
        List<AiToolMcpVO> result = new ArrayList<>();

        // 查询启用状态的MCP工具
        List<AiToolMcpPO> toolMcpList = aiToolMcpMapper.selectList(
                new LambdaQueryWrapper<AiToolMcpPO>()
                        .eq(AiToolMcpPO::getStatus, 1));

        for (AiToolMcpPO toolMcpPO : toolMcpList) {
            if (toolMcpPO == null || toolMcpPO.getStatus() != 1) {
                continue;
            }

            // 转换为VO对象
            AiToolMcpVO mcpVO = AiToolMcpVO.builder()
                    .mcpId(toolMcpPO.getMcpId())
                    .mcpName(toolMcpPO.getMcpName())
                    .transportType(toolMcpPO.getTransportType())
                    .transportConfig(toolMcpPO.getTransportConfig())
                    .requestTimeout(toolMcpPO.getRequestTimeout())
                    .build();

            String transportConfig = toolMcpPO.getTransportConfig();
            String transportType = toolMcpPO.getTransportType();

            try {
                if ("sse".equals(transportType)) {
                    // 解析SSE配置
                    if (isValidJson(transportConfig)) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        AiToolMcpVO.TransportConfigSse transportConfigSse = objectMapper.readValue(transportConfig,
                                AiToolMcpVO.TransportConfigSse.class);
                        mcpVO.setTransportConfigSse(transportConfigSse);
                    } else {
                        log.warn("SSE传输配置JSON格式无效，MCP ID: {}, 配置内容: {}",
                                toolMcpPO.getMcpId(), transportConfig);
                    }
                } else if ("stdio".equals(transportType)) {
                    // 解析STDIO配置
                    if (isValidJson(transportConfig)) {
                        Map<String, AiToolMcpVO.TransportConfigStdio.Stdio> stdio = JSON.parseObject(transportConfig,
                                new TypeReference<Map<String, AiToolMcpVO.TransportConfigStdio.Stdio>>() {
                                });

                        AiToolMcpVO.TransportConfigStdio transportConfigStdio = new AiToolMcpVO.TransportConfigStdio();
                        transportConfigStdio.setStdio(stdio);
                        mcpVO.setTransportConfigStdio(transportConfigStdio);
                    } else {
                        log.warn("STDIO传输配置JSON格式无效，MCP ID: {}, 配置内容: {}",
                                toolMcpPO.getMcpId(), transportConfig);
                    }
                }
            } catch (Exception e) {
                log.error("解析传输配置失败，MCP ID: {}, 配置内容: {}, 错误信息: {}",
                        toolMcpPO.getMcpId(), transportConfig, e.getMessage(), e);
            }

            result.add(mcpVO);
        }

        // 将结果设置到上下文
        context.setValue(AiAgentEnumVO.AI_TOOL_MCP.getDataName(), result);
    }

    /**
     * 验证JSON格式是否有效
     */
    private boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            JSON.parse(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
