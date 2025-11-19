package com.zj.aiagemt.service.agent.impl.armory.node;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;


import org.springframework.stereotype.Component;

@Component
public class EndNode extends AgentAromorSupport{
    @Override
    protected String beanName(String beanId) {
        return "";
    }

    @Override
    protected String dataName() {
        return "";
    }

    @Override
    protected AgentArmoryVO doApply(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        return AgentArmoryVO.builder()
                .dynamicContext( context)
                .build();
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> get(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        return getDefaultStrategyHandler();
    }
}
