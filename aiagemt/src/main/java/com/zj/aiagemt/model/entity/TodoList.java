package com.zj.aiagemt.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 待办事项表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TodoList {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 对话ID
     */
    private String conversationId;

    /**
     * 任务内容
     */
    private String taskContent;

    /**
     * 任务状态(0:未完成, 1:进行中, 2:已完成)
     */
    private Integer status;

    /**
     * 优先级(1:低, 2:中, 3:高)
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}