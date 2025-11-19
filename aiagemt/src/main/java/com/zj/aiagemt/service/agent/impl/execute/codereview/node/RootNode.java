package com.zj.aiagemt.service.agent.impl.execute.codereview.node;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.service.agent.IAgentRepository;
import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;
import com.zj.aiagemt.service.agent.impl.execute.codereview.factory.DefaultAutoCodeReviewExecuteStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component("autoCodeReviewRoot")
@Slf4j
public class RootNode extends AbstractAutoCodeReviewExecuteSupport{
    @Resource
    private DistributeNode distributeNode;
    @Resource
    protected IAgentRepository repository;
    @Override
    protected String doApply(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("开始执行代码审核");

        Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap = repository.queryAiAgentClientFlowConfig(autoCodeCommandEntity.getAiAgentId());

        // 客户端对话组
        dynamicContext.setAiAgentClientFlowConfigVOMap(aiAgentClientFlowConfigVOMap);
        // 上下文信息
        dynamicContext.setExecutionHistory(new StringBuilder());


        return router(autoCodeCommandEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<AutoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext, String> get(AutoCodeCommandEntity autoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return distributeNode;
    }
}
