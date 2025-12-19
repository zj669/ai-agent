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
public class Step3QualitySupervisorNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 第三阶段：质量监督
        log.info("\n🔍 阶段3: 质量监督检查");

        // 从动态上下文中获取执行结果
        String executionResult = dynamicContext.getValue("executionResult");
        if (executionResult == null || executionResult.trim().isEmpty()) {
            log.warn("⚠️ 执行结果为空，跳过质量监督");
            return "质量监督跳过";
        }

        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.QUALITY_SUPERVISOR_CLIENT.getCode());

        String supervisionPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(), requestParameter.getUserMessage(), executionResult);

        // 获取对话客户端
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String supervisionResult = chatClient
                .prompt(supervisionPrompt)
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 1024))
                .call().content();

        assert supervisionResult != null;
        parseSupervisionResult(dynamicContext, supervisionResult, requestParameter.getSessionId());

        // 将监督结果保存到动态上下文中
        dynamicContext.setValue("supervisionResult", supervisionResult);

        // 根据监督结果决定是否需要重新执行
        if (supervisionResult.contains("是否通过: FAIL")) {
            log.info("❌ 质量检查未通过，需要重新执行");
            dynamicContext.setCurrentTask("根据质量监督的建议重新执行任务");
        } else if (supervisionResult.contains("是否通过: OPTIMIZE")) {
            log.info("🔧 质量检查建议优化，继续改进");
            dynamicContext.setCurrentTask("根据质量监督的建议优化执行结果");
        } else {
            log.info("✅ 质量检查通过");
            dynamicContext.setCompleted(true);
        }

        // 更新执行历史
        String stepSummary = String.format("""
                === 第 %d 步完整记录 ===
                【分析阶段】%s
                【执行阶段】%s
                【监督阶段】%s
                """, dynamicContext.getStep(),
                dynamicContext.getValue("analysisResult"),
                executionResult,
                supervisionResult);

        dynamicContext.getExecutionHistory().append(stepSummary);

        // 增加步骤计数
        dynamicContext.setStep(dynamicContext.getStep() + 1);

        // 如果任务已完成或达到最大步数，进入总结阶段
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            return router(requestParameter, dynamicContext);
        }

        // 否则继续下一轮执行，返回到Step1AnalyzerNode
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        // 如果任务已完成或达到最大步数，进入总结阶段
        if (dynamicContext.isCompleted() || dynamicContext.getStep() > dynamicContext.getMaxStep()) {
            return getBean("step4LogExecutionSummaryNode");
        }

        // 否则返回到Step1AnalyzerNode进行下一轮分析
        return getBean("step1AnalyzerNode");
    }

    /**
     * 解析监督结果
     */
    private void parseSupervisionResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String supervisionResult, String sessionId) {
        int step = dynamicContext.getStep();
        log.info("\n🔍 === 第 {} 步监督结果 ===", step);

        // 发送完整的监督结果
        sendSupervisionResult(dynamicContext, supervisionResult, sessionId);
    }

    /**
     * 发送监督结果到流式输出
     */
    private void sendSupervisionResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                       String supervisionResult, String sessionId) {
        sendSseResult(dynamicContext, dynamicContext.getStep(), supervisionResult, sessionId, false);
    }

    @Override
    protected AiClientTypeEnumVO getType() {
        return AiClientTypeEnumVO.QUALITY_SUPERVISOR_CLIENT;
    }
}
