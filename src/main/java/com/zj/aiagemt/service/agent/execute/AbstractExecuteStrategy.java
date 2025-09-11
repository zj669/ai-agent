package com.zj.aiagemt.service.agent.execute;//package com.zj.domain.agent.service.execute;
//
//import com.zj.domain.agent.adpater.repository.IAgentRepository;
//import com.zj.domain.agent.model.entity.ArmoryCommandEntity;
//import com.zj.domain.agent.model.entity.ExecuteCommandEntity;
//import com.zj.domain.agent.model.vo.AiAgentClientFlowConfigVO;
//import com.zj.domain.agent.service.armory.factory.DefaultAgentArmoryFactory;
//import com.zj.domain.agent.service.armory.model.AgentArmoryVO;
//import com.zj.types.common.design.tree.handler.StrategyHandler;
//import com.zj.types.enums.AiAgentEnumVO;
//import jakarta.annotation.Resource;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//@Component
//public abstract class AbstractExecuteStrategy implements IExecuteStrategy{
//    @Resource
//    private DefaultAgentArmoryFactory agentArmoryFactory;
//    @Resource
//    private IAgentRepository agentRepository;
//    @Override
//    public String execute(ExecuteCommandEntity executeCommandEntity) {
//        StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> armoryCommandEntityDynamicContextAgentArmoryVOStrategyHandler = agentArmoryFactory.strategyHandler();
//        List<String> commandIdList  = agentRepository.queryClientIdsByAgentId(executeCommandEntity.getAiAgentId());
//        ArmoryCommandEntity armoryCommandEntity = ArmoryCommandEntity.builder()
//                .commandType(AiAgentEnumVO.AI_CLIENT.getLoadDataStrategy())
//                .commandIdList(commandIdList)
//                .build();
//        DefaultAgentArmoryFactory.DynamicContext dynamicContext = new DefaultAgentArmoryFactory.DynamicContext();
//        armoryCommandEntityDynamicContextAgentArmoryVOStrategyHandler.apply(armoryCommandEntity, dynamicContext);
//
//        Map<String, AiAgentClientFlowConfigVO> stringAiAgentClientFlowConfigVOMap = agentRepository.queryAiAgentClientFlowConfig(executeCommandEntity.getAiAgentId());
//        DynamicContext build = DynamicContext.builder().aiAgentClientFlowConfigVOMap(stringAiAgentClientFlowConfigVOMap).build();
//        return doExecute(executeCommandEntity, build);
//    }
//
//    protected abstract String doExecute(ExecuteCommandEntity executeCommandEntity,DynamicContext build);
//
//    @Data
//    @Builder
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class DynamicContext {
//
//        // 任务执行步骤
//        private int step = 1;
//
//        // 最大任务步骤
//        private int maxStep = 1;
//
//        private StringBuilder executionHistory;
//
//        private String currentTask;
//
//        boolean isCompleted = false;
//
//        private Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap;
//
//        private Map<String, Object> dataObjects = new HashMap<>();
//
//        public <T> void setValue(String key, T value) {
//            dataObjects.put(key, value);
//        }
//
//        public <T> T getValue(String key) {
//            return (T) dataObjects.get(key);
//        }
//    }
//}
