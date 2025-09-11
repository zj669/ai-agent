package com.zj.aiagemt.service.agent.armory.node;


import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.model.vo.AiClientModelVO;
import com.zj.aiagemt.service.agent.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.armory.model.AgentArmoryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ModelNode extends  AgentAromorSupport{
    @Resource
    private McpNode mcpNode;
    @Override
    protected String beanName(String beanId) {
        return AiAgentEnumVO.AI_CLIENT_MODEL.getBeanName(beanId);
    }

    @Override
    protected String dataName() {
        return AiAgentEnumVO.AI_CLIENT_MODEL.getDataName();
    }

    @Override
    protected AgentArmoryVO doApply(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) throws Exception {
        log.info("Ai Agent 构建节点，客户端,ModelNode");
        List<AiClientModelVO> value = context.getValue(dataName());
        for (AiClientModelVO model : value) {
            OpenAiChatModel chatModel = OpenAiChatModel.builder()
                    .openAiApi(getBean(AiAgentEnumVO.AI_CLIENT_API.getBeanName(model.getApiId())))
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(model.getModelName())
                            .build()
                    )
                    .build();
            registerBean(beanName(model.getModelId()), OpenAiChatModel.class,  chatModel);
        }
        return router(requestParams, context);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> get(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        return mcpNode;
    }
}
