package com.zj.aiagemt.service.agent.execute.codereview.node;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.model.enums.AiClientTypeEnumVO;
import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;
import com.zj.aiagemt.service.agent.execute.codereview.factory.DefaultAutoCodeReviewExecuteStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SummaryNode  extends AbstractAutoCodeReviewExecuteSupport{
    @Override
    protected String doApply(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        String history = dynamicContext.getExecutionHistory().toString();
        log.error("history: {}", history);
        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.SUMMARY_ASSISTANT.getCode());
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String summaryPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(),
                history);
        return chatClient
                .prompt(summaryPrompt)
                .call().content();
    }

    @Override
    public StrategyHandler<AutoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext, String> get(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
