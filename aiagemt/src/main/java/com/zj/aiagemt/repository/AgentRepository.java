package com.zj.aiagemt.repository;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.zj.aiagemt.repository.base.AiAgentMapper;
import com.zj.aiagemt.repository.base.AiAdvisorMapper;
import com.zj.aiagemt.repository.base.AiToolMcpMapper;
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
    private AiAgentMapper aiAgentMapper;
    @Resource
    private AiToolMcpMapper aiToolMcpMapper;
    @Resource
    private SpringContextUtil springContextUtil;

    @Override
    public List<AiAgent> queryAgentDtoList(Long userId) {
        if(userId == null){
            return List.of();
        }
        return aiAgentMapper.selectList(new LambdaQueryWrapper<AiAgent>().eq(AiAgent::getUserId, userId));
    }

    @Override
    public void queryMcps(DefaultAgentArmoryFactory.DynamicContext context) {

        List<AiClientToolMcpVO> result = new ArrayList<>();
        List<AiToolMcp> toolMcps = aiToolMcpMapper.selectList(new LambdaQueryWrapper<AiToolMcp>().eq(AiToolMcp::getStatus, 1));
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
                        // 验证JSON格式是否正确
                        if (isValidJson(transportConfig)) {
                            AiClientToolMcpVO.TransportConfigSse transportConfigSse = objectMapper.readValue(transportConfig, AiClientToolMcpVO.TransportConfigSse.class);
                            mcpVO.setTransportConfigSse(transportConfigSse);
                        } else {
                            log.warn("SSE传输配置JSON格式无效，MCP ID: {}, 配置内容: {}", toolMcp.getMcpId(), transportConfig);
                        }
                    } else if ("stdio".equals(transportType)) {
                        // 解析STDIO配置
                        // 验证JSON格式是否正确
                        if (isValidJson(transportConfig)) {
                            Map<String, AiClientToolMcpVO.TransportConfigStdio.Stdio> stdio = JSON.parseObject(transportConfig,
                                    new TypeReference<>() {
                                    });

                            AiClientToolMcpVO.TransportConfigStdio transportConfigStdio = new AiClientToolMcpVO.TransportConfigStdio();
                            transportConfigStdio.setStdio(stdio);

                            mcpVO.setTransportConfigStdio(transportConfigStdio);
                        } else {
                            log.warn("STDIO传输配置JSON格式无效，MCP ID: {}, 配置内容: {}", toolMcp.getMcpId(), transportConfig);
                        }
                    }
                } catch (Exception e) {
                    log.error("解析传输配置失败，MCP ID: {}, 配置内容: {}, 错误信息: {}", 
                             toolMcp.getMcpId(), transportConfig, e.getMessage(), e);
                }
                result.add(mcpVO);
            }
        }
        context.setValue(AiAgentEnumVO.AI_TOOL_MCP.getDataName(), result);
    }

    /**
     * 验证字符串是否为有效的JSON格式
     * @param jsonStr 待验证的JSON字符串
     * @return 是否为有效JSON
     */
    private boolean isValidJson(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return false;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.readTree(jsonStr);
            return true;
        } catch (Exception e) {
            return false;
        }
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
                    log.warn("解析Advisor扩展参数失败，Advisor ID: {}, 参数内容: {}, 错误信息: {}", 
                            aiClientAdvisor.getAdvisorId(), extParam, e.getMessage());
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

        context.setValue(AiAgentEnumVO.AI_ADVISOR.getDataName(), result);
    }
}