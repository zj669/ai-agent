package com.zj.aiagent.domain.workflow.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 条件分支定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    /**
     * 分支ID（用于路由匹配）
     */
    private String branchId;

    /**
     * 分支标签（UI 展示）
     */
    private String label;

    /**
     * 分支描述（用于 LLM 语义路由）
     */
    private String description;

    /**
     * SpEL 条件表达式（仅 EXPRESSION 模式使用）
     */
    private String condition;

    /**
     * 该分支连接的下游节点ID列表
     */
    private String[] nextNodeIds;
}
