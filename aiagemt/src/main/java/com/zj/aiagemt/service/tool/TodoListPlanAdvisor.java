package com.zj.aiagemt.service.tool;

import com.alibaba.fastjson2.JSON;
import com.zj.aiagemt.Repository.base.TodoListMapper;
import com.zj.aiagemt.model.entity.TodoList;
import com.zj.aiagemt.service.todo.TodoListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class TodoListPlanAdvisor implements BaseAdvisor {

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    
    private TodoListMapper todoListMapper;
    private TodoListService todoListService;
    
    public TodoListPlanAdvisor(TodoListMapper todoListMapper, TodoListService todoListService) {
        this.todoListMapper = todoListMapper;
        this.todoListService = todoListService;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        try {
            // 定义Few-shot提示词模板
            String fewShotTemplate = "你是一个任务规划助手，请根据用户的消息来规划生成任务。\n\n" +
                    "请按照以下格式输出任务列表，每个任务占一行：\n" +
                    "1. 任务内容1\n" +
                    "2. 任务内容2\n" +
                    "3. 任务内容3\n\n" +
                    "示例：\n" +
                    "用户消息：我需要准备一个技术分享会\n" +
                    "输出：\n" +
                    " 太棒了！技术分享是个人和团队成长的重要方式。准备一个出色的技术分享需要系统性的规划。这里为你提供一个从零到一的全流程指南，你可以根据你的时间和主题进行调整。" +
                    "=== 任务规划开始 === \n" +
                    "1. 确定分享主题和内容大纲\n" +
                    "2. 制作演示文稿\n" +
                    "3. 准备示例代码\n" +
                    "4. 练习演讲并录制视频\n\n" +
                    "现在请根据以下用户消息生成任务列表：\n" +
                    "用户消息：";
            
            // 修改提示词，使用Few-shot方法
            String originalUserMessage = chatClientRequest.prompt().getUserMessage().getText();
            String enhancedUserMessage = fewShotTemplate + originalUserMessage + "\n\n" +
                    "请严格按照指定格式输出，只输出任务列表，不要包含其他内容：\n" +
                    "=== 任务规划开始 ===";

            // 创建新的请求对象
            return chatClientRequest.mutate()
                    .prompt(chatClientRequest.prompt().augmentSystemMessage(enhancedUserMessage))
                    .build();
        } catch (Exception e) {
            log.error("增强提示词时发生错误", e);
        }
        
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        try {
            // 获取AI响应内容
            String responseContent = chatClientResponse.chatResponse().getResult().getOutput().getText();
            
            // 提取任务规划部分
            String taskContent = extractTaskPlanning(responseContent);
            
            // 隐藏规划的任务部分（移除分隔符之间的内容）
            String filteredContent = filterTaskPlanning(responseContent);
            
            // 更新响应内容
            ChatClientResponse filteredResponse = ChatClientResponse.builder()
                    .context(chatClientResponse.context())
                    .chatResponse(ChatResponse.builder()
                            .from(chatClientResponse.chatResponse())
                            .generations(List.of(new Generation(new AssistantMessage(filteredContent))))
                            .build())
                    .build();
            
            // 解析待办事项列表
            List<String> todoItems = parseTodoList(taskContent);
            
            // 获取对话ID
            Map<String, Object> context = chatClientResponse.context();
            String conversationId = (String) context.get(CHAT_MEMORY_CONVERSATION_ID_KEY);
            
            // 保存待办事项到数据库
            saveTodoItems(todoItems, conversationId);
            
            return filteredResponse;
            
        } catch (Exception e) {
            log.error("处理待办事项时发生错误", e);
        }
        
        return chatClientResponse;
    }

    /**
     * 提取任务规划部分
     * @param responseContent 原始响应内容
     * @return 任务内容
     */
    private String extractTaskPlanning(String responseContent) {
        // 查找分隔符位置并提取之间的内容
        String startMarker = "=== 任务规划开始 ===";
        int startIndex = responseContent.indexOf(startMarker);
        
        if (startIndex != -1) {
            // 返回分隔符之后的内容
            return responseContent.substring(startIndex + startMarker.length()).trim();
        }
        
        return responseContent;
    }

    /**
     * 隐藏规划的任务部分
     * @param responseContent 原始响应内容
     * @return 过滤后的内容
     */
    private String filterTaskPlanning(String responseContent) {
        // 查找分隔符位置并移除之间的内容
        String startMarker = "=== 任务规划开始 ===";
        int startIndex = responseContent.indexOf(startMarker);
        
        if (startIndex != -1) {
            // 返回分隔符之前的内容
            return responseContent.substring(0, startIndex).trim();
        }
        
        return responseContent;
    }

    /**
     * 解析AI生成的待办事项列表
     * @param responseContent AI响应内容
     * @return 待办事项列表
     */
    private List<String> parseTodoList(String responseContent) {
        // 按行分割并过滤任务编号
        String[] lines = responseContent.split("\n");
        List<String> todoItems = new ArrayList<String>();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                // 移除行首的数字编号和点号（如"1. "）
                if (trimmedLine.matches("^\\d+\\.\\s.*")) {
                    String task = trimmedLine.replaceFirst("^\\d+\\.\\s+", "");
                    todoItems.add(task);
                } else {
                    todoItems.add(trimmedLine);
                }
            }
        }
        
        return todoItems;
    }

    /**
     * 保存待办事项到数据库
     * @param todoItems 待办事项列表
     * @param conversationId 对话ID
     */
    private void saveTodoItems(List<String> todoItems, String conversationId) {
        for (String item : todoItems) {
            if (item != null && !item.trim().isEmpty()) {
                // 默认优先级为中等(2)
                int priority = 2;
                
                // 尝试从任务内容中提取优先级信息
                if (item.toLowerCase().contains("high") || item.toLowerCase().contains("重要") || item.toLowerCase().contains("紧急")) {
                    priority = 3; // 高优先级
                } else if (item.toLowerCase().contains("low") || item.toLowerCase().contains("低")) {
                    priority = 1; // 低优先级
                }
                
                TodoList todo = TodoList.builder()
                        .conversationId(conversationId)
                        .taskContent(item.trim())
                        .status(0) // 0表示未完成
                        .priority(priority) // 设置优先级
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                
                try {
                    todoListMapper.insert(todo);
                    log.info("成功保存待办事项: {}", item.trim());
                } catch (Exception e) {
                    log.error("保存待办事项失败: {}", item.trim(), e);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}