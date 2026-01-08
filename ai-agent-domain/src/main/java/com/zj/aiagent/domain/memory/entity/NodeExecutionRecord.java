package com.zj.aiagent.domain.memory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 节点执行记录（领域实体）
 * <p>
 * 表示工作流中单个节点的执行详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionRecord {

    /**
     * 记录ID
     */
    private Long id;

    /**
     * 执行实例ID
     */
    private Long instanceId;

    private String agentId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 执行状态: RUNNING, SUCCESS, FAILED, SKIPPED
     */
    private String executeStatus;

    /**
     * 节点输入数据（JSON格式）
     */
    private String inputData;

    /**
     * 节点输出数据（JSON格式）
     */
    private String outputData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 使用的模型信息
     */
    private String modelInfo;

    /**
     * Token使用情况（JSON格式）
     */
    private String tokenUsage;
}
