package com.zj.aiagemt.service.agent.impl.execute.model;//package com.zj.domain.agent.service.execute.model;
//
//import com.zj.domain.agent.model.entity.ExecuteCommandEntity;
//import com.zj.domain.agent.service.armory.factory.DefaultAgentArmoryFactory;
//import com.zj.domain.agent.service.execute.AbstractExecuteStrategy;
//import com.zj.types.enums.AiAgentEnumVO;
//import com.zj.types.enums.AiClientTypeEnumVO;
//import com.zj.types.utills.SpringContextUtil;
//import jakarta.annotation.Resource;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.stereotype.Component;
//
//@Component
//public class ModelExecuteStrategy extends AbstractExecuteStrategy {
//    @Resource
//    private SpringContextUtil springContextUtil;
//    @Resource
//    private DefaultAgentArmoryFactory defaultAgentArmoryFactory;
//    @Override
//    public String doExecute(ExecuteCommandEntity executeCommandEntity, DynamicContext build) {
//        ChatClient client = springContextUtil.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(
//                build.getAiAgentClientFlowConfigVOMap().get(AiClientTypeEnumVO.DEFAULT.getCode()).getClientId()));
//        return client.prompt(executeCommandEntity.getUserMessage()).call().content();
//    }
//}
