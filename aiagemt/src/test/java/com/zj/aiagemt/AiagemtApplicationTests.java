package com.zj.aiagemt;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.service.agent.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.armory.model.AgentArmoryVO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AiagemtApplicationTests {
	@Resource
	private DefaultAgentArmoryFactory defaultArmoryStrategyFactory;

	@Test
	void contextLoads() throws Exception {
		List<String> commandIdList = List.of(new String[]{"3002"});
		StrategyHandler<ArmoryCommandEntity, DefaultAgentArmoryFactory.DynamicContext, AgentArmoryVO> armoryStrategyHandler =
				defaultArmoryStrategyFactory.strategyHandler();

		AgentArmoryVO result = armoryStrategyHandler.apply(
				ArmoryCommandEntity.builder()
						.commandType(AiAgentEnumVO.AI_CLIENT.getLoadDataStrategy())
						.commandIdList(commandIdList)
						.build(),
				new DefaultAgentArmoryFactory.DynamicContext());
	}

}
