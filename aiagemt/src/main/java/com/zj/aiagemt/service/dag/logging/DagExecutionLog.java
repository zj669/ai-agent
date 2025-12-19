package com.zj.aiagemt.service.dag.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DAG执行日志
 * 记录DAG执行的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DagExecutionLog {

    /**
     * 日志ID
     */
    private String logId;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * DAG ID
     */
    private String dagId;

    /**
     * 节点ID（如果是节点日志）
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 日志级别 (INFO, WARN, ERROR)
     */
    private String level;

    /**
     * 日志类型 (DAG_START, DAG_END, NODE_START, NODE_END, NODE_ERROR)
     */
    private String logType;

    /**
     * 日志消息
     */
    private String message;

    /**
     * 详细信息（JSON格式）
     */
    private Map<String, Object> details;

    /**
     * 耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 异常信息
     */
    private String exceptionMessage;

    /**
     * 异常堆栈
     */
    private String exceptionStack;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
