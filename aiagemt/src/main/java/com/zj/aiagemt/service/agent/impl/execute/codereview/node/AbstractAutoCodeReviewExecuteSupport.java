package com.zj.aiagemt.service.agent.impl.execute.codereview.node;

import com.zj.aiagemt.common.design.ruletree.AbstractMultiThreadStrategyRouter;
import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.service.agent.impl.execute.codereview.factory.DefaultAutoCodeReviewExecuteStrategyFactory;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractAutoCodeReviewExecuteSupport extends AbstractMultiThreadStrategyRouter<AutoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext, String> {
    @Resource
    protected ApplicationContext applicationContext;

    protected ChatClient getChatClientByClientId(String clientId) {
        return getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(clientId));
    }

    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }
    @Override
    protected void multiThread(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

}
