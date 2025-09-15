package com.zj.aiagemt.service.agent.execute.codereview.node;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.model.enums.AiClientTypeEnumVO;
import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;
import com.zj.aiagemt.service.agent.execute.codereview.factory.DefaultAutoCodeReviewExecuteStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class DistributeNode  extends AbstractAutoCodeReviewExecuteSupport{
    @Resource
    private SummaryNode summaryNode;
    @Override
    protected String doApply(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return router(autoCodeCommandEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<AutoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext, String> get(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return summaryNode;
    }

    @Override
    protected void multiThread(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        log.info("开始分发");
        List<String> diff = autoCodeCommandEntity.getDiff();
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < diff.size(); i += 10) {
            int end = Math.min(i + 10, diff.size());
            result.add(diff.subList(i, end));
        }
        AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO = dynamicContext.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.TASK_ANALYZER_CLIENT.getCode());

        List<CompletableFuture<String>> futures = new ArrayList<>();

        result.stream().map(subList -> {
            return CompletableFuture.supplyAsync(() -> {
                return chat(aiAgentClientFlowConfigVO, subList);
            });
        }).forEach(futures::add);
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join(); // 等待所有任务完成
        
        // 收集所有结果并添加到执行历史中
        StringBuilder executionHistory = dynamicContext.getExecutionHistory();
        if (executionHistory == null) {
            executionHistory = new StringBuilder();
            dynamicContext.setExecutionHistory(executionHistory);
        }
        
        for (CompletableFuture<String> future : futures) {
            String s = future.get();
            log.info("分发结果: {}", s);
            executionHistory.append(s);
        }
    }


    private String chat(AiAgentClientFlowConfigVO aiAgentClientFlowConfigVO ,List<String> diff){
        ChatClient chatClient = getChatClientByClientId(aiAgentClientFlowConfigVO.getClientId());

        String analysisPrompt = String.format(aiAgentClientFlowConfigVO.getStepPrompt(),
                JSON.toJSONString(diff));
        return chatClient
                .prompt(analysisPrompt)
                .call().content();
    }
}
