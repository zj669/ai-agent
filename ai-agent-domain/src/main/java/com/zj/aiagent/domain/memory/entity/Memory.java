package com.zj.aiagent.domain.memory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 长期记忆实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Memory {

    /**
     * 记忆ID
     */
    private String id;

    /**
     * 记忆内容
     */
    private String content;

    /**
     * 记忆类型: fact(事实), preference(偏好), event(事件)
     */
    private String type;

    /**
     * 重要性评分 (0-1)
     */
    private Double importance;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;

    /**
     * 元数据
     */
    private java.util.Map<String, Object> metadata;
}
