package com.zj.aiagent.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Agent 执行日志持久化对象
 * <p>
 * 对应表: ai_agent_execution_log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentExecutionLogPO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联的运行实例ID
     */
    private Long instanceId;

    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 执行的节点ID
     */
    private String nodeId;

    /**
     * 节点类型 (PLAN/ACT/REFLECT/ROUTER/END等)
     */
    private String nodeType;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 执行状态 (RUNNING/SUCCESS/FAILED/SKIPPED)
     */
    private String executeStatus;

    /**
     * 节点输入数据(JSON格式)
     */
    private String inputData;

    /**
     * 节点输出数据(JSON格式)
     */
    private String outputData;

    /**
     * 错误信息(如果失败)
     */
    private String errorMessage;

    /**
     * 错误堆栈(如果失败)
     */
    private String errorStack;

    /**
     * 开始执行时间
     */
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时(毫秒)
     */
    private Long durationMs;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 使用的模型信息
     */
    private String modelInfo;

    /**
     * Token使用情况(JSON格式)
     */
    private String tokenUsage;

    /**
     * 其他元数据(JSON格式)
     */
    private String metadata;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;
}
