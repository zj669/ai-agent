package com.zj.aiagemt.service.agent.impl.execute.auto.step;


import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.AutoAgentExecuteResultEntity;
import com.zj.aiagemt.model.bo.ExecuteCommandEntity;
import com.zj.aiagemt.model.enums.AiClientTypeEnumVO;
import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;
import com.zj.aiagemt.service.agent.impl.execute.auto.factory.DefaultAutoAgentExecuteStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class Step1AnalyzerNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 获取配置信息
        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.TASK_ANALYZER_CLIENT.getCode());

        // 第一阶段：任务分析
        String analysisPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(),
                requestParameter.getUserMessage(),
                dynamicContext.getStep(),
                dynamicContext.getMaxStep(),
                !dynamicContext.getExecutionHistory().isEmpty() ? dynamicContext.getExecutionHistory().toString() : "[首次执行]",
                dynamicContext.getCurrentTask()
        );

        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String analysisResult = chatClient
                .prompt(analysisPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();

        assert analysisResult != null;
        parseAnalysisResult(dynamicContext, analysisResult, requestParameter.getSessionId());

        dynamicContext.setValue("analysisResult", analysisResult);

        if (analysisResult.contains("任务状态: COMPLETED") ||
                analysisResult.contains("完成度评估: 100%")) {
            dynamicContext.setCompleted(true);
            log.info("✅ 任务分析显示已完成！");
            return router(requestParameter, dynamicContext);
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        // 如果任务已完成或达到最大步数，进入总结阶段
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            return getBean("step4LogExecutionSummaryNode");
        }

        // 否则继续执行下一步
        return getBean("step2PrecisionExecutorNode");
    }

    private void parseAnalysisResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String analysisResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("\n📊 === 第 {} 步分析结果 ===", step);

        // 发送最后一个section的内容
        sendAnalysisSubResult(dynamicContext,step , analysisResult, sessionId);
    }

    /**
     * 发送分析阶段细分结果到流式输出
     */
    private void sendAnalysisSubResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                      Integer step , String content, String sessionId) {
        if (!content.isEmpty()) {
            sendSseResult(dynamicContext,step,content, sessionId, false);
        }
    }

    @Override
    protected AiClientTypeEnumVO getType() {
        return AiClientTypeEnumVO.TASK_ANALYZER_CLIENT;
    }
}
