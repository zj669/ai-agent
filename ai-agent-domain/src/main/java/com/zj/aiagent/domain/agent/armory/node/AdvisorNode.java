//package com.zj.aiagent.domain.agent.armory.node;
//
//import com.alibaba.fastjson2.JSON;
//import com.zj.aiagent.domain.agent.armory.factory.DefaultAgentArmoryFactory;
//import com.zj.aiagent.domain.agent.armory.model.AgentArmoryVO;
//import com.zj.aiagent.domain.agent.armory.model.ArmoryCommandEntity;
//import com.zj.aiagent.shared.model.enums.AiAgentEnumVO;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.ai.chat.client.advisor.api.Advisor;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@Slf4j
//public class AdvisorNode extends AgentAromorSupport{
//    @Resource
//    private VectorStore vectorStore;
//
//    @Resource
//    private EndNode endNode;
//
//    @Override
//    protected String beanName(String beanId) {
//        return AiAgentEnumVO.AI_ADVISOR.getBeanName(beanId);
//    }
//
//    @Override
//    protected String dataName() {
//        return AiAgentEnumVO.AI_ADVISOR.getDataName();
//    }
//
//    @Override
//    protected AgentArmoryVO doApply(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) throws Exception {
//        log.info("Ai Agent 构建节点，Advisor 顾问角色{}", JSON.toJSONString(requestParams));
//
//        List<AiClientAdvisorVO> aiClientAdvisorList = context.getValue(dataName());
//
//        if (aiClientAdvisorList == null || aiClientAdvisorList.isEmpty()) {
//            log.warn("没有需要被初始化的 ai client advisor");
//            return router(requestParams, context);
//        }
//
//        for (AiClientAdvisorVO aiClientAdvisorVO : aiClientAdvisorList) {
//            // 构建顾问访问对象
//            Advisor advisor = createAdvisor(aiClientAdvisorVO);
//            if(advisor == null){
//                log.error("Advisor 构建失败，Advisor ID: {}", aiClientAdvisorVO.getAdvisorId());
//                continue;
//            }
//            // 注册Bean对象
//            registerBean(beanName(aiClientAdvisorVO.getAdvisorId()), Advisor.class, advisor);
//        }
//
//        return router(requestParams, context);
//
//    }
//
//    @Override
//    public StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> get(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
//        return endNode;
//    }
//
//    private Advisor createAdvisor(AiClientAdvisorVO aiClientAdvisorVO) {
//        String advisorType = aiClientAdvisorVO.getAdvisorType();
//        AiClientAdvisorTypeEnumVO advisorTypeEnum = AiClientAdvisorTypeEnumVO.getByCode(advisorType);
//        if (advisorTypeEnum == null) {
//            return  null;
//        }
//        return advisorTypeEnum.createAdvisor(aiClientAdvisorVO, vectorStore);
//    }
//}
