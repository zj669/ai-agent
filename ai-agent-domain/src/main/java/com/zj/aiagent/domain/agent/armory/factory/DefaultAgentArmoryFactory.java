//package com.zj.aiagent.domain.agent.armory.factory;
//
//
//import com.zj.aiagent.domain.agent.armory.model.AgentArmoryVO;
//import com.zj.aiagent.domain.agent.armory.model.ArmoryCommandEntity;
//import com.zj.aiagent.domain.agent.armory.node.RootNode;
//import com.zj.aiagent.shared.design.ruletree.StrategyHandler;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Component
//public class DefaultAgentArmoryFactory {
//    private final RootNode rootNode;
//
//    public DefaultAgentArmoryFactory(RootNode rootNode) {
//        this.rootNode = rootNode;
//    }
//
//    public StrategyHandler<ArmoryCommandEntity, DynamicContext, AgentArmoryVO> strategyHandler() {
//        return rootNode;
//    }
//
//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    @Builder
//    public static class DynamicContext{
//        private Map<String, Object> dataObjects = new HashMap<>();
//
//        public <T> void setValue(String key, T value) {
//            dataObjects.put(key, value);
//        }
//
//        public <T> T getValue(String key) {
//            return (T) dataObjects.get(key);
//        }
//
//    }
//}
