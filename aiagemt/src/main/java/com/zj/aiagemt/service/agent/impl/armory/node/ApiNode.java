package com.zj.aiagemt.service.agent.impl.armory.node;


import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.model.vo.AiClientApiVO;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
public class ApiNode extends AgentAromorSupport {
    @Resource
    private ModelNode modelNode;
    @Resource(name = "webClientBuilder1")
    private WebClient.Builder webClientBuilder;
    @Resource(name = "restClientBuilder1")
    private RestClient.Builder restClientBuilder;
    @Override
    protected AgentArmoryVO doApply(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) throws Exception {
        log.info("Ai Agent 构建节点，客户端,ApiNode");
        List<AiClientApiVO> value = context.getValue(dataName());
        for (AiClientApiVO aiClientApiVO : value) {
            OpenAiApi build = OpenAiApi.builder()
                    .baseUrl(aiClientApiVO.getBaseUrl())
                    .apiKey(aiClientApiVO.getApiKey())
                    .webClientBuilder(webClientBuilder)
                    .restClientBuilder(restClientBuilder)
                    .build();
            registerBean(beanName(aiClientApiVO.getApiId()), OpenAiApi.class,  build);
        }
        return router(requestParams, context);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> get(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        return modelNode;
    }

    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_API.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_API.getDataName();
    }
}
