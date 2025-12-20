package com.zj.aiagemt.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.zj.aiagemt.repository.base.AiAgentMapper;
import com.zj.aiagemt.repository.base.AiAgentTaskScheduleMapper;
import com.zj.aiagemt.repository.base.AiAdvisorMapper;
import com.zj.aiagemt.repository.base.AiApiMapper;
import com.zj.aiagemt.repository.base.AiModelMapper;
import com.zj.aiagemt.repository.base.AiRagOrderMapper;
import com.zj.aiagemt.repository.base.AiSystemPromptMapper;
import com.zj.aiagemt.repository.base.AiToolMcpMapper;
import com.zj.aiagemt.model.dto.AgentInfoDTO;
import com.zj.aiagemt.model.entity.*;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.model.vo.*;
import com.zj.aiagemt.service.agent.IAgentRepository;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
@Primary
public class AgentRepository implements IAgentRepository {
    @Resource
    private AiAdvisorMapper aiAdvisorMapper;
    @Resource
    private AiToolMcpMapper aiToolMcpMapper;
    @Resource
    private SpringContextUtil springContextUtil;

    @Override
    public List<AiAgent> queryAgentDtoList() {
        return List.of();
    }

    @Override
    public void queryMcps(DefaultAgentArmoryFactory.DynamicContext context) {

        List<AiClientToolMcpVO> result = new ArrayList<>();
        List<AiToolMcp> toolMcps = aiToolMcpMapper.selectList(new LambdaQueryWrapper<>());
        for (AiToolMcp toolMcp : toolMcps) {
            if (toolMcp != null && toolMcp.getStatus() == 1) {
                // 4. 转换为VO对象
                AiClientToolMcpVO mcpVO = AiClientToolMcpVO.builder()
                        .mcpId(toolMcp.getMcpId())
                        .mcpName(toolMcp.getMcpName())
                        .transportType(toolMcp.getTransportType())
                        .transportConfig(toolMcp.getTransportConfig())
                        .requestTimeout(toolMcp.getRequestTimeout())
                        .build();

                String transportConfig = toolMcp.getTransportConfig();
                String transportType = toolMcp.getTransportType();

                try {
                    if ("sse".equals(transportType)) {
                        // 解析SSE配置
                        ObjectMapper objectMapper = new ObjectMapper();
                        AiClientToolMcpVO.TransportConfigSse transportConfigSse = objectMapper.readValue(transportConfig, AiClientToolMcpVO.TransportConfigSse.class);
                        mcpVO.setTransportConfigSse(transportConfigSse);
                    } else if ("stdio".equals(transportType)) {
                        // 解析STDIO配置
                        Map<String, AiClientToolMcpVO.TransportConfigStdio.Stdio> stdio = JSON.parseObject(transportConfig,
                                new TypeReference<>() {
                                });

                        AiClientToolMcpVO.TransportConfigStdio transportConfigStdio = new AiClientToolMcpVO.TransportConfigStdio();
                        transportConfigStdio.setStdio(stdio);

                        mcpVO.setTransportConfigStdio(transportConfigStdio);
                    }
                } catch (Exception e) {
                    log.error("解析传输配置失败: {}", e.getMessage(), e);
                }
                result.add(mcpVO);
            }
        }
        context.setValue(AiAgentEnumVO.AI_CLIENT_TOOL_MCP.getDataName(), result);
    }

    @Override
    public void queryAdvisors(DefaultAgentArmoryFactory.DynamicContext context) {
        List<AiClientAdvisorVO> result = new ArrayList<>();
        List<AiAdvisor> aiAdvisors = aiAdvisorMapper.selectList(new LambdaQueryWrapper<>());
        for (AiAdvisor aiClientAdvisor : aiAdvisors) {
            // 3. 解析extParam中的配置
            AiClientAdvisorVO.ChatMemory chatMemory = null;
            AiClientAdvisorVO.RagAnswer ragAnswer = null;
            AiClientAdvisorVO.VectorStoreRetriever vectorStoreRetriever = null;

            String extParam = aiClientAdvisor.getExtParam();
            if (extParam != null && !extParam.trim().isEmpty()) {
                try {
                    if ("ChatMemory".equals(aiClientAdvisor.getAdvisorType())) {
                        // 解析chatMemory配置
                        chatMemory = JSON.parseObject(extParam, AiClientAdvisorVO.ChatMemory.class);
                    } else if ("RagAnswer".equals(aiClientAdvisor.getAdvisorType())) {
                        // 解析ragAnswer配置
                        ragAnswer = JSON.parseObject(extParam, AiClientAdvisorVO.RagAnswer.class);
                    } else if ("VectorStoreRetrieverMemoryAdvisor".equals(aiClientAdvisor.getAdvisorType())) {
                        // 解析vectorStoreRetriever配置
                        vectorStoreRetriever = JSON.parseObject(extParam, AiClientAdvisorVO.VectorStoreRetriever.class);
                    }
                } catch (Exception e) {
                    // 解析失败时忽略，使用默认值null
                }
            }

            // 4. 构建AiClientAdvisorVO对象
            AiClientAdvisorVO advisorVO = AiClientAdvisorVO.builder()
                    .advisorId(aiClientAdvisor.getAdvisorId())
                    .advisorName(aiClientAdvisor.getAdvisorName())
                    .advisorType(aiClientAdvisor.getAdvisorType())
                    .orderNum(aiClientAdvisor.getOrderNum())
                    .chatMemory(chatMemory)
                    .ragAnswer(ragAnswer)
                    .vectorStoreRetriever(vectorStoreRetriever)
                    .springContextUtil(springContextUtil)
                    .build();

            result.add(advisorVO);
        }

        context.setValue(AiAgentEnumVO.AI_CLIENT_ADVISOR.getDataName(), result);
    }
}
