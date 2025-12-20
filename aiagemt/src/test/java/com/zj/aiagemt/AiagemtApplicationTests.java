package com.zj.aiagemt;

import com.zj.aiagemt.common.design.ruletree.StrategyHandler;
import com.zj.aiagemt.model.bo.ArmoryCommandEntity;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.service.agent.impl.armory.factory.DefaultAgentArmoryFactory;
import com.zj.aiagemt.service.agent.impl.armory.model.AgentArmoryVO;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class AiagemtApplicationTests {
	@Resource
	private DefaultAgentArmoryFactory defaultArmoryStrategyFactory;
	
	@Resource
	private SpringContextUtil springContextUtil;

	@Test
	void contextLoads() throws Exception {
	}
	
	@Test
	void test() {
		ChatClient bean = springContextUtil.getBean(AiAgentEnumVO.AI_CLIENT.getBeanName("3001"));
			String userInput = "你是谁，记住我叫zj99999";
			String content = bean.prompt().user(userInput).call().content();
			System.out.println("AI回答: " + content);
			String userInput1 = "我叫什么？";
			String content1 = bean.prompt().user(userInput1).call().content();
			System.out.println("AI回答: " + content1);
			String userInput2 = "我叫什么？";
			String content2 = bean.prompt().user(userInput2).call().content();
			System.out.println("AI回答: " + content2);
	}

}