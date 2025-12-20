package com.zj.aiagemt.controller.client;

import com.zj.aiagemt.model.common.Response;
import com.zj.aiagemt.model.entity.TodoList;
import com.zj.aiagemt.service.todo.TodoListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/client/todo")
public class TodoListController {
    
    @Resource
    private TodoListService todoListService;
    
    /**
     * 根据对话ID获取待办事项列表
     * @param conversationId 对话ID
     * @return 待办事项列表
     */
    @GetMapping("/list/{conversationId}")
    public Response<List<TodoList>> getTodoList(@PathVariable String conversationId) {
        try {
            List<TodoList> todoList = todoListService.getTodoListByConversationId(conversationId);
            return Response.<List<TodoList>>builder()
                    .code(String.valueOf(200))
                    .info("获取待办事项列表成功")
                    .data(todoList)
                    .build();
        } catch (Exception e) {
            log.error("获取待办事项列表失败, conversationId: {}", conversationId, e);
            return Response.<List<TodoList>>builder()
                    .code(String.valueOf(500))
                    .info("获取待办事项列表失败: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }
}