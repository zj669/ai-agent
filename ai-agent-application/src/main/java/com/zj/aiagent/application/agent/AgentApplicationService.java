package com.zj.aiagent.application.agent;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zj.aiagent.application.agent.command.ChatCommand;
import com.zj.aiagent.application.agent.query.GetUserAgentsQuery;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
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

    public void chat(ChatCommand command) {
        log.info("执行对话, user: {}", command.getUserMessage());
        DagGraph dagGraph = dagLoaderService.loadDagByAgentId(command.getAgentId());
        String conversationId = command.getConversationId();
        if(conversationId == null){
            conversationId = String.valueOf(IdUtil.getSnowflake(1, 1).nextId());
        }
        dagExecuteService.executeDag(dagGraph, conversationId, command.getUserMessage(), command.getEmitter());
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
                .agentId(po.getAgentId())
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
     * Agent DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentDTO {
        private Long id;
        private String agentId;
        private String agentName;
        private String description;
        private Integer status;
        private String statusDesc;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
