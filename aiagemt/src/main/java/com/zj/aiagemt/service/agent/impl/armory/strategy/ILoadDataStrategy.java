package com.zj.aiagemt.service.agent.impl.armory.strategy;


import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;

public interface ILoadDataStrategy {
    void loadData(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context);
}
