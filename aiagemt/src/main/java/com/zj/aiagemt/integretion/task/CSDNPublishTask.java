package com.zj.aiagemt.integretion.task;

import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.utils.JavaInterviewGenerator;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class CSDNPublishTask {
    @Resource
    private SpringContextUtil springContextUtil;

    @Scheduled(cron = "0 0 0/3 * * ?")
    public void run1() {
        JavaInterviewGenerator.InterviewConfig config = JavaInterviewGenerator.randomSelect();
        JavaInterviewGenerator.printConfigInfo(config);
        String userInput =JavaInterviewGenerator.generatePrompt(config);
        String beanName = AiAgentEnumVO.AI_CLIENT.getBeanName(String.valueOf(3002));
        ChatClient chatClient = springContextUtil.getBean(beanName);
        log.info("\n>>> QUESTION: {}", userInput);
        Flux<String> content = chatClient.prompt(userInput).stream().content();
        List<String> block = content.collectList().block();
        log.info("\n>>> ANSWER: {}", String.join("", block));
    }
}
