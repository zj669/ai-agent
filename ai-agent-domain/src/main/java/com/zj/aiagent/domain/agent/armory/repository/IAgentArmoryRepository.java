package com.zj.aiagent.domain.agent.armory.repository;


import com.zj.aiagent.domain.agent.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagent.domain.agent.dag.entity.AiAgent;

import java.util.List;


public interface IAgentArmoryRepository {
    List<AiAgent> queryAgentDtoList(Long userId);

    void queryMcps(DefaultAgentArmoryFactory.DynamicContext context);

//    void queryAdvisors(DefaultAgentArmoryFactory.DynamicContext context);
}
