package com.zj.aiagent.config;


import com.zj.aiagent.domain.agent.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagent.domain.agent.armory.model.AgentArmoryVO;
import com.zj.aiagent.domain.agent.armory.model.ArmoryCommandEntity;
import com.zj.aiagent.shared.design.ruletree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
//@ConditionalOnProperty(prefix = "spring.ai.agent.auto-config", name = "enabled", havingValue = "true")
public class AiAgentAutoConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private DefaultAgentArmoryFactory defaultArmoryStrategyFactory;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            log.info("AI Agent 自动装配开始，配置: {}", aiAgentAutoConfigProperties);
            // 检查配置是否有效
            if (!aiAgentAutoConfigProperties.isEnabled()) {
                log.info("AI Agent 自动装配未启用");
                return;
            }
            // 执行自动装配
            StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> armoryStrategyHandler =
                    defaultArmoryStrategyFactory.strategyHandler();

            AgentArmoryVO result = armoryStrategyHandler.apply(
                    ArmoryCommandEntity.builder()
                            .build(),
                    new DefaultAgentArmoryFactory.DynamicContext());

            log.info("AI Agent 自动装配完成，结果: {}", result);

        } catch (Exception e) {
            log.error("AI Agent 自动装配失败", e);
        }
    }

}