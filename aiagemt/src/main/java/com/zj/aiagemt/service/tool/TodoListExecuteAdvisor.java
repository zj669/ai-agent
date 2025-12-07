package com.zj.aiagemt.service.tool;

import com.zj.aiagemt.Repository.base.TodoListMapper;
import com.zj.aiagemt.model.entity.TodoList;
import com.zj.aiagemt.service.todo.TodoListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
public class TodoListExecuteAdvisor implements BaseAdvisor {
    
    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    
    private TodoListMapper todoListMapper;
    private TodoListService todoListService;
    
    public TodoListExecuteAdvisor(TodoListMapper todoListMapper, TodoListService todoListService) {
        this.todoListMapper = todoListMapper;
        this.todoListService = todoListService;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        try {
            // 获取对话ID
            String conversationId = (String) chatClientRequest.context().get(CHAT_MEMORY_CONVERSATION_ID_KEY);
            
            // 查找进行中的待办事项
            List<TodoList> inProgressTodos = todoListService.getInProgressTodoList(conversationId);
            
            // 如果存在进行中的任务，则修改提示词，将本次任务放进提示词内
            if (!inProgressTodos.isEmpty()) {
                StringBuilder taskContext = new StringBuilder();
                taskContext.append("您正在执行以下任务，请在回答中考虑这些任务的要求：\n");
                
                for (TodoList todo : inProgressTodos) {
                    taskContext.append("- ").append(todo.getTaskContent()).append("\n");
                }
                
                // 修改提示词，将任务上下文加入用户消息
                String originalUserMessage = chatClientRequest.prompt().getUserMessage().getText();
                String enhancedUserMessage = taskContext.toString() + "\n" + originalUserMessage;



                // 创建新的请求对象
                return chatClientRequest.mutate()
                        .prompt(chatClientRequest.prompt().augmentUserMessage(enhancedUserMessage))
                        .build();
            }
        } catch (Exception e) {
            log.error("处理待办事项上下文时发生错误", e);
        }
        
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        try {
            // 获取对话ID
            String conversationId = (String) chatClientResponse.context().get(CHAT_MEMORY_CONVERSATION_ID_KEY);
            
            // 标记进行中的待办事项为已完成
            List<TodoList> inProgressTodos = todoListService.getInProgressTodoList(conversationId);
            for (TodoList todo : inProgressTodos) {
                todoListService.markTodoAsCompleted(todo.getId());
            }
            
        } catch (Exception e) {
            log.error("更新待办事项状态时发生错误", e);
        }
        
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}