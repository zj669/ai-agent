package com.zj.aiagemt.service.tool;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.zj.aiagemt.model.enums.AiAgentEnumVO;
import com.zj.aiagemt.utils.SpringContextUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;
 
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
 
public class ReReadingAdvisor implements BaseAdvisor {
    @Resource
    private SpringContextUtil springContextUtil;
 
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
 
    @Override
    public int getOrder() {
        return 0; // 默认顺序，可以根据需要调整
    }
 
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> advisedUserParams = new HashMap<>(chatClientRequest.context());
        advisedUserParams.put("input_query", chatClientRequest.prompt().getUserMessage().getText());

        String beanName = AiAgentEnumVO.AI_CLIENT.getBeanName(String.valueOf(3002));
        ChatClient chatClient = springContextUtil.getBean(beanName);
        String content = chatClient
                .prompt(Prompt.builder()
                        .messages(chatClientRequest.prompt().getUserMessage(), new AssistantMessage(JSON.toJSONString(chatClientRequest.context())))
                        .build())
                .call().content();

        advisedUserParams.put("re2_input_query", content);
        return ChatClientRequest.builder()
                .prompt(Prompt.builder().messages(new UserMessage(content == null?chatClientRequest.prompt().getUserMessage().getText():content), new AssistantMessage(JSON.toJSONString(advisedUserParams))).build())
                .context(advisedUserParams)
                .build();
    }
 
    @Override   
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse; // 不修改响应
    }
}