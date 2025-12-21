//package com.zj.aiagent.domain.agent.armory.node;
//
//
//import com.zj.aiagent.domain.agent.armory.factory.DefaultAgentArmoryFactory;
//import com.zj.aiagent.domain.agent.armory.model.AgentArmoryVO;
//import com.zj.aiagent.domain.agent.armory.model.ArmoryCommandEntity;
//import com.zj.aiagent.shared.design.ruletree.AbstractMultiThreadStrategyRouter;
//import com.zj.aiagent.shared.utils.SpringContextUtil;
//import jakarta.annotation.Resource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//
//@Component
//public abstract class AgentAromorSupport extends AbstractMultiThreadStrategyRouter<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> {
//    private final Logger log = LoggerFactory.getLogger(AgentAromorSupport.class);
//    @Resource
//    protected SpringContextUtil springContextUtil ;
//
//    @Override
//    protected void multiThread(ArmoryCommandEntity requestParams, DefaultAgentArmoryFactory.DynamicContext context) {
//
//    }
//
//    /**
//     * 通用的Bean注册方法
//     *
//     * @param beanName  Bean名称
//     * @param beanClass Bean类型
//     * @param <T>       Bean类型
//     */
//    protected synchronized <T> void registerBean(String beanName, Class<T> beanClass, T beanInstance) {
//       springContextUtil.registerBean(beanName, beanClass, beanInstance);
//    }
//
//    protected <T> T getBean(String beanName) {
//        return springContextUtil.getBean(beanName);
//    }
//
//    protected abstract String beanName(String beanId);
//
//
//    protected abstract String dataName();
//}
