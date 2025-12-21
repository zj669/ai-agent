package com.zj.aiagent.infrastructure.dag.repository;

import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.domain.agent.dag.repository.IDagRepository;
import com.zj.aiagent.infrastructure.dag.converter.AgentConvert;
import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class DagRepository implements IDagRepository {
    @Resource
    private AiAgentMapper aiAgentMapper;

    @Override
    public AiAgent selectAiAgentByAgentId(String agentId) {
        if(agentId == null){
            return null;
        }
        AiAgentPO aiAgentPO = aiAgentMapper.selectById(agentId);
        return AgentConvert.toDomain(aiAgentPO);
    }
}
