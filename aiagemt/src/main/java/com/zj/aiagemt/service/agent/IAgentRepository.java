package com.zj.aiagemt.service.agent;

import com.zj.aiagemt.model.dto.AgentInfoDTO;
import com.zj.aiagemt.model.entity.AiAgent;
import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;

import java.util.List;
import java.util.Map;


public interface IAgentRepository {
    List<AiAgent> queryAgentDtoList(Long userId);

    void queryMcps(DefaultAgentArmoryFactory.DynamicContext context);

    void queryAdvisors(DefaultAgentArmoryFactory.DynamicContext context);
}
