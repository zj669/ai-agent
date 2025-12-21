package com.zj.aiagent.domain.agent.armory.node;


import com.zj.aiagent.domain.agent.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagent.domain.agent.armory.model.AgentArmoryVO;
import com.zj.aiagent.domain.agent.armory.model.ArmoryCommandEntity;
import com.zj.aiagent.shared.design.ruletree.StrategyHandler;
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
