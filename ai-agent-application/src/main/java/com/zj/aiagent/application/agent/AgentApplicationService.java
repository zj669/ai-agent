package com.zj.aiagent.application.agent;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.application.agent.command.ChatCommand;
import com.zj.aiagent.application.agent.command.SaveAgentCommand;
import com.zj.aiagent.application.agent.query.GetUserAgentsQuery;
import com.zj.aiagent.domain.agent.config.repository.IAgentConfigRepository;
import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.repository.IDagRepository;
import com.zj.aiagent.domain.agent.dag.service.DagExecuteService;
import com.zj.aiagent.domain.agent.dag.service.DagLoaderService;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 应用服务
 *
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {

    private final AiAgentMapper aiAgentMapper;
    private final DagExecuteService dagExecuteService;
    private final DagLoaderService dagLoaderService;
    private final IDagRepository dagRepository;
    private final IAgentConfigRepository agentConfigRepository;

    public void chat(ChatCommand command) {
        log.info("执行对话, user: {}", command.getUserMessage());
        DagGraph dagGraph = dagLoaderService.loadDagByAgentId(command.getAgentId());
        String conversationId = command.getConversationId();
        if (conversationId == null) {
            conversationId = String.valueOf(IdUtil.getSnowflake(1, 1).nextId());
        }
        dagExecuteService.executeDag(dagGraph, conversationId, command.getUserMessage(), command.getEmitter(),
                command.getAgentId());
    }

    /**
     * 保存Agent配置
     *
     * @param command 保存命令
     * @return 保存后的agentId
     */
    public String saveAgent(SaveAgentCommand command) {
        log.info("保存Agent配置, agentId={}, agentName={}", command.getAgentId(), command.getAgentName());

        // 权限校验：如果是更新操作，需要验证用户权限
        if (command.getAgentId() != null && !command.getAgentId().isEmpty()) {
            AiAgent existingAgent = dagRepository.selectAiAgentByAgentIdAndUserId(command.getAgentId(),
                    command.getUserId());
            if (existingAgent == null) {
                throw new RuntimeException("Agent不存在或无权限修改");
            }
        }

        // 处理graphJson：转换modelId和promptId为完整配置
        String processedGraphJson = processGraphJson(command.getGraphJson());

        // 构建Agent实体
        AiAgent agent = AiAgent.builder()
                .agentId(command.getAgentId())
                .userId(command.getUserId())
                .agentName(command.getAgentName())
                .description(command.getDescription())
                .graphJson(processedGraphJson)
                .status(command.getStatus() != null ? command.getStatus() : 0) // 默认草稿状态
                .build();

        // 保存到数据库
        AiAgent savedAgent = dagRepository.saveAgent(agent);

        log.info("Agent保存成功, agentId={}", savedAgent.getAgentId());
        return savedAgent.getAgentId();
    }

    /**
     * 查询用户的 Agent 列表
     *
     * @param query 查询对象
     * @return Agent DTO 列表
     */
    public List<AgentDTO> getUserAgents(GetUserAgentsQuery query) {
        log.info("查询用户 Agent 列表, userId: {}", query.getUserId());

        // 查询用户的所有 Agent
        LambdaQueryWrapper<AiAgentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgentPO::getUserId, query.getUserId())
                .orderByDesc(AiAgentPO::getUpdateTime);

        List<AiAgentPO> agentList = aiAgentMapper.selectList(wrapper);

        // 转换为 DTO
        return agentList.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为 DTO
     */
    private AgentDTO toDTO(AiAgentPO po) {
        return AgentDTO.builder()
                .id(po.getId())
                .agentName(po.getAgentName())
                .description(po.getDescription())
                .status(po.getStatus())
                .statusDesc(getStatusDesc(po.getStatus()))
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    /**
     * 获取状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "已停用";
            default -> "未知";
        };
    }

    /**
     * 处理graphJson，转换modelId和promptId为完整配置
     */
    private String processGraphJson(String graphJson) {
        if (graphJson == null || graphJson.isEmpty()) {
            return graphJson;
        }

        try {
            // 解析JSON
            com.alibaba.fastjson.JSONObject graphObj = com.alibaba.fastjson.JSON.parseObject(graphJson);
            com.alibaba.fastjson.JSONArray nodes = graphObj.getJSONArray("nodes");

            if (nodes == null || nodes.isEmpty()) {
                return graphJson;
            }

            // 遍历所有节点
            for (int i = 0; i < nodes.size(); i++) {
                com.alibaba.fastjson.JSONObject node = nodes.getJSONObject(i);
                com.alibaba.fastjson.JSONObject config = node.getJSONObject("config");

                if (config == null) {
                    continue;
                }

                // 处理modelId
                String modelId = config.getString("modelId");
                if (modelId != null && !modelId.isEmpty()) {
                    java.util.Map<String, Object> modelConfig = agentConfigRepository
                            .findModelWithApiByModelId(modelId);
                    if (modelConfig != null) {
                        // 替换为完整的model配置
                        config.put("model", modelConfig);
                        config.remove("modelId");
                        log.debug("节点 {} 的modelId已转换为完整配置", node.getString("nodeId"));
                    } else {
                        log.warn("未找到modelId对应的配置: {}", modelId);
                    }
                }

                // 处理promptId
                String promptId = config.getString("promptId");
                if (promptId != null && !promptId.isEmpty()) {
                    com.zj.aiagent.domain.agent.config.entity.SystemPromptEntity promptEntity = agentConfigRepository
                            .findSystemPromptByPromptId(promptId);
                    if (promptEntity != null) {
                        // 替换为systemPrompt内容
                        config.put("systemPrompt", promptEntity.getPromptContent());
                        config.remove("promptId");
                        log.debug("节点 {} 的promptId已转换为systemPrompt", node.getString("nodeId"));
                    } else {
                        // promptId不存在，使用nodeType的默认prompt
                        log.warn("未找到promptId {}，尝试使用nodeType默认prompt", promptId);
                        String nodeType = node.getString("nodeType");
                        if (nodeType != null && !nodeType.isEmpty()) {
                            com.zj.aiagent.domain.agent.config.entity.NodeTemplateEntity templateEntity = agentConfigRepository
                                    .findNodeTemplateByNodeType(nodeType);
                            if (templateEntity != null && templateEntity.getDefaultSystemPrompt() != null) {
                                config.put("systemPrompt", templateEntity.getDefaultSystemPrompt());
                                config.remove("promptId");
                                log.info("节点 {} 使用nodeType {} 的默认prompt", node.getString("nodeId"), nodeType);
                            } else {
                                log.warn("nodeType {} 也没有默认prompt", nodeType);
                            }
                        }
                    }
                } else {
                    // 没有promptId，检查是否需要nodeType默认prompt
                    if (!config.containsKey("systemPrompt") ||
                            config.getString("systemPrompt") == null ||
                            config.getString("systemPrompt").isEmpty()) {
                        String nodeType = node.getString("nodeType");
                        if (nodeType != null && !nodeType.isEmpty()) {
                            com.zj.aiagent.domain.agent.config.entity.NodeTemplateEntity templateEntity = agentConfigRepository
                                    .findNodeTemplateByNodeType(nodeType);
                            if (templateEntity != null && templateEntity.getDefaultSystemPrompt() != null) {
                                config.put("systemPrompt", templateEntity.getDefaultSystemPrompt());
                                log.info("节点 {} 使用nodeType {} 的默认prompt", node.getString("nodeId"), nodeType);
                            }
                        }
                    }
                }
            }

            // 序列化回JSON字符串
            return graphObj.toJSONString();

        } catch (Exception e) {
            log.error("处理graphJson失败", e);
            throw new RuntimeException("graphJson格式错误: " + e.getMessage());
        }
    }

    /**
     * Agent DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentDTO {
        private Long id;
        private String agentName;
        private String description;
        private Integer status;
        private String statusDesc;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
