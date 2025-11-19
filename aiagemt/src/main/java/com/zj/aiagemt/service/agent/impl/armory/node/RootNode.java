package com.zj.aiagemt.service.agent.impl.armory.node;


import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;
import com.zj.aiagemt.service.agent.impl.armory.strategy.ILoadDataStrategy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class RootNode extends AgentAromorSupport{
    @Resource
    private ApiNode apiNode;
    @Resource
    private Map<String, ILoadDataStrategy> loadDataStrategyMap;

    @Override
    protected AgentArmoryVO doApply(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) throws Exception {
        return router(requestParams, context);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> get(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        return apiNode;
    }

    @Override
    protected void multiThread(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        // 策略加载数据
        String commandType = requestParams.getCommandType();
        log.info("开始加载数据 commandType{}", commandType);
        ILoadDataStrategy loadDataStrategy = loadDataStrategyMap.get(commandType);
        if (loadDataStrategy == null) {
            log.error("未找到对应的策略");
        }
        loadDataStrategy.loadData(requestParams, context);
    }

    @Override
    protected String beanName(String beanId) {
        return "";
    }

    @Override
    protected String dataName() {
        return "";
    }
}
