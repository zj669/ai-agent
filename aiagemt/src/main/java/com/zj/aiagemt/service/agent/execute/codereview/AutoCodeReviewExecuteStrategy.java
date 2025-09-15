package com.zj.aiagemt.service.agent.execute.codereview;


import com.alibaba.fastjson.JSON;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.AutoAgentExecuteResultEntity;
import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.model.bo.ExecuteCommandEntity;
import com.zj.aiagemt.service.agent.execute.IExecuteStrategy;
import com.zj.aiagemt.service.agent.execute.auto.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.zj.aiagemt.service.agent.execute.codereview.factory.DefaultAutoCodeReviewExecuteStrategyFactory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * 自动执行策略
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/8/5 09:49
 */
@Slf4j
@Service
public class AutoCodeReviewExecuteStrategy {

    @Resource
    private DefaultAutoCodeReviewExecuteStrategyFactory defaultAutoCodeReviewExecuteStrategyFactory;

    public String execute(AutoCodeCommandEntity executeCommandEntity) throws Exception {
        StrategyHandler<AutoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext, String> autoCodeCommandEntityDynamicContextStringStrategyHandler = defaultAutoCodeReviewExecuteStrategyFactory.armoryStrategyHandler();
        String ans = autoCodeCommandEntityDynamicContextStringStrategyHandler.apply(executeCommandEntity, new DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext());
        log.info("审计结果:{}", ans);
        return ans;

    }

}
