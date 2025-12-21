package com.zj.aiagent.infrastructure.dag.converter;

import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.user.entity.User;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;

public class AgentConvert {
    public static AiAgent toDomain(AiAgentPO po) {
        if (po == null) {
            return null;
        }
        return AiAgent.builder()
                .id(po.getId())
                .userId(po.getUserId())
                .agentName(po.getAgentName())
                .createTime(po.getCreateTime())
                .status(po.getStatus())
                .graphJson(po.getGraphJson())
                .updateTime(po.getUpdateTime())
                .description(po.getDescription())
                .build();
    }
}
