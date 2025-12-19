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
public class Step4LogExecutionSummaryNode extends AbstractExecuteSupport {

    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        log.info("\n📊 === 执行第 {} 步 ===", dynamicContext.getStep());

        // 第四阶段：执行总结
        log.info("\n📊 阶段4: 执行总结分析");

        // 记录执行总结
        logExecutionSummary(dynamicContext.getMaxStep(), dynamicContext.getExecutionHistory(), dynamicContext.isCompleted());

        // 生成最终总结报告（无论任务是否完成都需要生成）
        generateFinalReport(requestParameter, dynamicContext);

        log.info("\n🏁 === 动态多轮执行结束 ====");

        return "ai agent execution summary completed!";
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        // 总结节点是最后一个节点，返回null表示执行结束
        return defaultStrategyHandler;
    }

    /**
     * 记录执行总结
     */
    private void logExecutionSummary(int maxSteps, StringBuilder executionHistory, boolean isCompleted) {
        log.info("\n📊 === 动态多轮执行总结 ====");

        int actualSteps = Math.min(maxSteps, executionHistory.toString().split("=== 第").length - 1);
        log.info("📈 总执行步数: {} 步", actualSteps);

        if (isCompleted) {
            log.info("✅ 任务完成状态: 已完成");
        } else {
            log.info("⏸️ 任务完成状态: 未完成（达到最大步数限制）");
        }

        // 计算执行效率
        double efficiency = isCompleted ? 100.0 : (double) actualSteps / maxSteps * 100;
        log.info("📊 执行效率: {}%", efficiency);
    }

    /**
     * 生成最终总结报告
     */
    private void generateFinalReport(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) {
        try {
            boolean isCompleted = dynamicContext.isCompleted();
            log.info("\n--- 生成{}任务的最终答案 ---", isCompleted ? "已完成" : "未完成");

            AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.RESPONSE_ASSISTANT.getCode());

            String summaryPrompt = getSummaryPrompt(aiAgentClientFlowConfigVO, requestParameter, dynamicContext, isCompleted);

            // 获取对话客户端 - 使用任务分析客户端进行总结
            ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

            String summaryResult = chatClient
                    .prompt(summaryPrompt)
                    .advisors(a -> a
                            .param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId() + "-summary")
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                    .call().content();

            assert summaryResult != null;
            logFinalReport(dynamicContext, summaryResult, requestParameter.getSessionId());

            // 将总结结果保存到动态上下文中
            dynamicContext.setValue("finalSummary", summaryResult);

        } catch (Exception e) {
            log.error("生成最终总结报告时出现异常: {}", e.getMessage(), e);
        }
    }

    private static String getSummaryPrompt(AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO, ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, boolean isCompleted) {
        String summaryPrompt;
        if (isCompleted) {
            summaryPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(),
                    requestParameter.getUserMessage(),
                    dynamicContext.getExecutionHistory().toString());
        } else {
            summaryPrompt = String.format("""
                    虽然任务未完全执行完成，但请基于已有的执行过程，尽力回答用户的原始问题：
                    
                    **用户原始问题:** %s
                    
                    **已执行的过程和获得的信息:**
                    %s
                    
                    **要求:**
                    1. 基于已有信息，尽力回答用户的原始问题
                    2. 如果信息不足，说明哪些部分无法完成并给出原因
                    3. 提供已能确定的部分答案
                    4. 给出完成剩余部分的具体建议
                    5. 以MD语法的表格形式，优化展示结果数据
                    
                    请基于现有信息给出用户问题的答案：
                    """,
                    requestParameter.getUserMessage(),
                    dynamicContext.getExecutionHistory().toString());
        }
        return summaryPrompt;
    }

    /**
     * 输出最终总结报告
     */
    private void logFinalReport(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String summaryResult, String sessionId) {
        boolean isCompleted = dynamicContext.isCompleted();
        log.info("\n📋 === {}任务最终总结报告 ===", isCompleted ? "已完成" : "未完成");

        // 发送完整的总结结果
        sendSummaryResult(dynamicContext, summaryResult, sessionId);

        // 发送完成标识
        sendCompleteResult(dynamicContext, sessionId);
    }

    /**
     * 发送总结结果到流式输出
     */
    private void sendSummaryResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                                   String summaryResult, String sessionId) {
        sendSseResult(dynamicContext, dynamicContext.getStep(), summaryResult, sessionId, false);
    }
    /**
     * 发送完成标识到流式输出
     */
    private void sendCompleteResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext, String sessionId) {
        AutoAgentExecuteResultEntity result = AutoAgentExecuteResultEntity.createCompleteResult(sessionId);
        sendSseResult(dynamicContext, null,"执行完成", sessionId , true);
        log.info("✅ 已发送完成标识");
    }

    /**
     * 检测总结部分标识
     */
    private String detectSummarySection(String content) {
        if (content.contains("已完成的工作") || content.contains("完成的工作") || content.contains("工作内容和成果")) {
            return "completed_work";
        } else if (content.contains("未完成的原因") || content.contains("未完成原因")) {
            return "incomplete_reasons";
        } else if (content.contains("关键因素") || content.contains("完成的关键因素")) {
            return "key_factors";
        } else if (content.contains("执行效率") || content.contains("执行效率和质量")) {
            return "efficiency_quality";
        } else if (content.contains("完成剩余任务的建议") || content.contains("建议") || content.contains("优化建议") || content.contains("经验总结")) {
            return "suggestions";
        } else if (content.contains("整体执行效果") || content.contains("评估")) {
            return "evaluation";
        }
        return null;
    }

    @Override
    protected AiClientTypeEnumVO getType() {
        return AiClientTypeEnumVO.SUMMARY_ASSISTANT;
    }
}
