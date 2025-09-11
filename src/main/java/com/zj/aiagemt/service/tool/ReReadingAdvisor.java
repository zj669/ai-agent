package com.zj.aiagemt.service.tool;

import java.util.HashMap;
import java.util.Map;
 
import reactor.core.publisher.Flux;
 
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
 
public class ReReadingAdvisor implements BaseAdvisor {
 
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
        advisedUserParams.put("re2_input_query", chatClientRequest.userText());
 
        return ChatClientRequest.from(chatClientRequest)
        .userText("""
                  {re2_input_query}
                  Read the question again: {re2_input_query}
                  """)
        .userParams(advisedUserParams)
        .build();
    }
 
    @Override   
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse; // 不修改响应
    }
}