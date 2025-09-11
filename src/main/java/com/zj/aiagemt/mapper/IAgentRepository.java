package com.zj.aiagemt.mapper;

import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;
import com.zj.aiagemt.service.agent.armory.factory.DefaultAgentArmoryFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


public interface IAgentRepository {
    void queryApiByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context);

    void queryModelByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context);

    void queryMcpByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context);

    void queryAdvisorByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context);

    void queryPromptByClientIdS(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context);

    void queryAiClientVOByClientIds(List<String> commandIdList, DefaultAgentArmoryFactory.DynamicContext context);


    Map<String, AiAgentClientFlowConfigVO> queryAiAgentClientFlowConfig(String aiAgentId);

    List<String> queryClientIdsByAgentId(String aiAgentId);
}
