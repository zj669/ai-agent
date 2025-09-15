package com.zj.aiagemt.service.agent.execute.codereview.factory;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.AutoCodeCommandEntity;
import com.zj.aiagemt.model.vo.AiAgentClientFlowConfigVO;

import com.zj.aiagemt.service.agent.execute.codereview.node.RootNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
@Component
public class DefaultAutoCodeReviewExecuteStrategyFactory {
    private final RootNode autoCodeReviewRoot;

    public DefaultAutoCodeReviewExecuteStrategyFactory(RootNode autoCodeReviewRoot) {
        this.autoCodeReviewRoot = autoCodeReviewRoot;
    }

    public StrategyHandler<AutoCodeCommandEntity, DefaultAutoCodeReviewExecuteStrategyFactory.DynamicContext, String> armoryStrategyHandler(){
        return autoCodeReviewRoot;
    }



    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {




        private StringBuilder executionHistory;


        private Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap;

        private Map<String, Object> dataObjects = new HashMap<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }
    }
}
