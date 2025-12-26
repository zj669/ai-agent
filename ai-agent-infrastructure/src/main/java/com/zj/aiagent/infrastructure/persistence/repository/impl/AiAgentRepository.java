package com.zj.aiagent.infrastructure.persistence.repository.impl;

import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;
import com.zj.aiagent.infrastructure.persistence.mapper.AiAgentMapper;
import com.zj.aiagent.infrastructure.persistence.repository.IAiAgentRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

@Repository
public class AiAgentRepository implements IAiAgentRepository {
    @Resource
    private AiAgentMapper aiAgentMapper;
    @Override
    public AiAgentPO getById(String agentId) {
        if(agentId != null){
            return aiAgentMapper.selectById(agentId);
        }
        return null;
    }
}
