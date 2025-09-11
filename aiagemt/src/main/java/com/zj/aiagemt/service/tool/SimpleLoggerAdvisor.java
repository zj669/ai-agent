package com.zj.aiagemt.service.tool;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;

public class SimpleLoggerAdvisor implements BaseAdvisor {
 
    private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerAdvisor.class);
 
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
        logger.debug("发送请求前的操作: {}", JSON.toJSONString(chatClientRequest));
        return chatClientRequest; // 不修改请求
    }
 
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        logger.debug("响应后的操作: {}", JSON.toJSONString(chatClientResponse));
        return chatClientResponse; // 不修改响应
    }
}