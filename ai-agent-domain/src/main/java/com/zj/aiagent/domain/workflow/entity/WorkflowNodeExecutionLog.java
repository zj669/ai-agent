package com.zj.aiagent.domain.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流节点执行日志 (Audit Log)
 * 用于记录每个节点的完整执行详情，包括输入输出和渲染模式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNodeExecutionLog {

    private Long id;
    private String executionId;
    private String nodeId;
    private String nodeName;
    private String nodeType;

    /**
     * 渲染模式: HIDDEN, THOUGHT, MESSAGE
     */
    private String renderMode;

    /**
     * 执行状态: 0:Running, 1:Success, 2:Failed
     */
    private Integer status;

    /**
     * 完整输入 (JSON)
     */
    private Map<String, Object> inputs;

    /**
     * 完整输出 (JSON)
     */
    private Map<String, Object> outputs;

    private String errorMessage;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
