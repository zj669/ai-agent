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
public class Step2PrecisionExecutorNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("\n⚡ 阶段2: 精准任务执行");

        // 从动态上下文中获取分析结果
        String analysisResult = dynamicContext.getValue("analysisResult");
        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            log.warn("⚠️ 分析结果为空，使用默认执行策略");
            analysisResult = "执行当前任务步骤";
        }

        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.PRECISION_EXECUTOR_CLIENT.getCode());

        String executionPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(), requestParameter.getUserMessage(), analysisResult);

        // 获取对话客户端
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String executionResult = chatClient
                .prompt(executionPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();

        assert executionResult != null;
        parseExecutionResult(dynamicContext, executionResult, requestParameter.getSessionId());

        // 将执行结果保存到动态上下文中，供下一步使用
        dynamicContext.setValue("executionResult", executionResult);

        // 更新执行历史
        String stepSummary = String.format("""
                === 第 %d 步执行记录 ===
                【分析阶段】%s
                【执行阶段】%s
                """, dynamicContext.getStep(), analysisResult, executionResult);

        dynamicContext.getExecutionHistory().append(stepSummary);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        return getBean("step3QualitySupervisorNode");
    }

    /**
     * 解析执行结果
     */
    private void parseExecutionResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String executionResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("\n⚡ === 第 {} 步执行结果 ===", step);

        // 发送最后一个section的内容
        sendExecutionSubResult(dynamicContext, executionResult, sessionId);
    }

    /**
     * 发送执行阶段细分结果到流式输出
     */
    private void sendExecutionSubResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                        String content, String sessionId) {
        // 抽取的通用判断逻辑
        if ( !content.isEmpty()) {
            sendSseResult(dynamicContext,  dynamicContext.getStep(), content, sessionId, false);
        }
    }

    @Override
    protected AiClientTypeEnumVO getType() {
        return AiClientTypeEnumVO.PRECISION_EXECUTOR_CLIENT;
    }
}
