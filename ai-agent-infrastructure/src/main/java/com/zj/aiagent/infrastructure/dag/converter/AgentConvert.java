package com.zj.aiagent.infrastructure.dag.converter;

import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;

/**
 * Agent转换器
 * 负责领域实体和持久化对象之间的转换
 */
public class AgentConvert {

    /**
     * 将持久化对象转换为领域实体
     */
    public static AiAgent toDomain(AiAgentPO po) {
        if (po == null) {
            return null;
        }
        return AiAgent.builder()
                .userId(po.getUserId())
                .agentId(String.valueOf(po.getId()))
                .agentName(po.getAgentName())
                .description(po.getDescription())
                .graphJson(po.getGraphJson())
                .status(po.getStatus())
                .createTime(po.getCreateTime())
                .updateTime(po.getUpdateTime())
                .build();
    }

    /**
     * 将领域实体转换为持久化对象
     */
    public static AiAgentPO toPO(AiAgent agent) {
        if (agent == null) {
            return null;
        }
        return AiAgentPO.builder()
                .id(Long.valueOf(agent.getAgentId()))
                .userId(agent.getUserId())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .graphJson(agent.getGraphJson())
                .status(agent.getStatus())
                .createTime(agent.getCreateTime())
                .updateTime(agent.getUpdateTime())
                .build();
    }
}
