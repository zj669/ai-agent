package com.zj.aiagemt.service.agent.armory.strategy;


import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.service.agent.armory.factory.DefaultAgentArmoryFactory;

public interface ILoadDataStrategy {
    void loadData(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context);
}
