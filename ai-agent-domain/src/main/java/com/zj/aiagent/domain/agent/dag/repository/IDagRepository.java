package com.zj.aiagent.domain.agent.dag.repository;

import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;

public interface IDagRepository {
    AiAgent selectAiAgentByAgentId(String agentId);
}
