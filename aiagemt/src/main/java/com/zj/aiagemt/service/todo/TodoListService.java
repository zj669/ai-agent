package com.zj.aiagemt.service.todo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zj.aiagemt.Repository.base.TodoListMapper;
import com.zj.aiagemt.model.entity.TodoList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TodoListService {
    
    @Resource
    private TodoListMapper todoListMapper;
    
    /**
     * 根据对话ID获取待办事项列表
     * @param conversationId 对话ID
     * @return 待办事项列表
     */
    public List<TodoList> getTodoListByConversationId(String conversationId) {
        QueryWrapper<TodoList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        queryWrapper.orderByDesc("priority"); // 按优先级排序
        queryWrapper.orderByAsc("create_time");
        return todoListMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取未完成的待办事项列表
     * @param conversationId 对话ID
     * @return 未完成的待办事项列表
     */
    public List<TodoList> getUncompletedTodoList(String conversationId) {
        QueryWrapper<TodoList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        queryWrapper.eq("status", 0); // 0表示未完成
        queryWrapper.orderByDesc("priority"); // 按优先级排序
        queryWrapper.orderByAsc("create_time");
        return todoListMapper.selectList(queryWrapper);
    }
    
    /**
     * 获取进行中的待办事项列表
     * @param conversationId 对话ID
     * @return 进行中的待办事项列表
     */
    public List<TodoList> getInProgressTodoList(String conversationId) {
        QueryWrapper<TodoList> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("conversation_id", conversationId);
        queryWrapper.eq("status", 1); // 1表示进行中
        queryWrapper.orderByDesc("priority"); // 按优先级排序
        queryWrapper.orderByAsc("create_time");
        return todoListMapper.selectList(queryWrapper);
    }
    
    /**
     * 标记待办事项为进行中
     * @param todoId 待办事项ID
     * @return 是否更新成功
     */
    public boolean markTodoAsInProgress(Long todoId) {
        try {
            TodoList todo = todoListMapper.selectById(todoId);
            if (todo != null) {
                todo.setStatus(1); // 1表示进行中
                todo.setUpdateTime(LocalDateTime.now());
                todoListMapper.updateById(todo);
                log.info("标记待办事项为进行中: {}", todo.getTaskContent());
                return true;
            }
        } catch (Exception e) {
            log.error("标记待办事项为进行中时发生错误, todoId: {}", todoId, e);
        }
        return false;
    }
    
    /**
     * 标记待办事项为已完成
     * @param todoId 待办事项ID
     * @return 是否更新成功
     */
    public boolean markTodoAsCompleted(Long todoId) {
        try {
            TodoList todo = todoListMapper.selectById(todoId);
            if (todo != null) {
                todo.setStatus(2); // 2表示已完成
                todo.setUpdateTime(LocalDateTime.now());
                todoListMapper.updateById(todo);
                log.info("标记待办事项为已完成: {}", todo.getTaskContent());
                return true;
            }
        } catch (Exception e) {
            log.error("标记待办事项为已完成时发生错误, todoId: {}", todoId, e);
        }
        return false;
    }
}