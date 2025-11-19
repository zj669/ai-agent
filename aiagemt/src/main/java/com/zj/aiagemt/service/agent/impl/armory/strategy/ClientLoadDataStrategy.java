package com.zj.aiagemt.service.agent.impl.armory.strategy;


import com.zj.aiagemt.service.agent.IAgentRepository;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service("aiClientLoadDataStrategy")
@Slf4j
public class ClientLoadDataStrategy implements ILoadDataStrategy{
    @Resource
    private IAgentRepository agentRepository;
    @Resource
    protected ThreadPoolExecutor threadPoolExecutor;
    @Override
    public void loadData(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
        log.info("开始异步加载数据");
        // api
        CompletableFuture<Void> futureApi = CompletableFuture.runAsync(() -> {
            agentRepository.queryApiByClientIdS(requestParams.getCommandIdList(), context);
        }, threadPoolExecutor);
        // model
        CompletableFuture<Void> futureModel = CompletableFuture.runAsync(() -> {
            agentRepository.queryModelByClientIdS(requestParams.getCommandIdList(), context);
        }, threadPoolExecutor);
        // mcp
        CompletableFuture<Void> futureMcp = CompletableFuture.runAsync(() -> {
            agentRepository.queryMcpByClientIdS(requestParams.getCommandIdList(), context);
        }, threadPoolExecutor);
        // advisor
        CompletableFuture<Void> futureAdvisor = CompletableFuture.runAsync(() -> {
            agentRepository.queryAdvisorByClientIdS(requestParams.getCommandIdList(), context);
        }, threadPoolExecutor);
        // prompt
        CompletableFuture<Void> futurePrompt = CompletableFuture.runAsync(() -> {
            agentRepository.queryPromptByClientIdS(requestParams.getCommandIdList(), context);
        }, threadPoolExecutor);

        CompletableFuture<Void> futureClient = CompletableFuture.runAsync(() -> {
            agentRepository.queryAiClientVOByClientIds(requestParams.getCommandIdList(), context);
        }, threadPoolExecutor);


        CompletableFuture<Void> future = CompletableFuture.allOf(futureApi, futureModel, futureMcp, futureAdvisor, futurePrompt, futureClient);
        future.join();
        log.info("结束异步加载数据");
    }
}
