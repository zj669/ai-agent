package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;

public interface IAiAgentRepository {
    AiAgentPO getById(String agentId);
}
