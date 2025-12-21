package com.zj.aiagent.domain.agent.dag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 节点执行日志 - DAG 聚合的一部分
 * 记录单个节点的执行详情
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NodeExecutionLog {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 关联的执行实例ID
     */
    private Long instanceId;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * 会话ID
     */
    private String conversationId;

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
     * 输入数据(JSON)
     */
    private String inputData;

    /**
     * 输出数据(JSON)
     */
    private String outputData;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStack;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
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
     * 模型信息
     */
    private String modelInfo;

    /**
     * Token 使用情况
     */
    private String tokenUsage;

    /**
     * 其他元数据
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 开始执行
     */
    public void start(String inputJson) {
        this.executeStatus = "RUNNING";
        this.startTime = LocalDateTime.now();
        this.inputData = inputJson;
        this.createTime = LocalDateTime.now();
    }

    /**
     * 执行成功
     */
    public void succeed(String outputJson) {
        this.executeStatus = "SUCCESS";
        this.endTime = LocalDateTime.now();
        this.outputData = outputJson;
        this.durationMs = calculateDuration();
    }

    /**
     * 执行失败
     */
    public void fail(String errorMsg, String stackTrace) {
        this.executeStatus = "FAILED";
        this.endTime = LocalDateTime.now();
        this.errorMessage = errorMsg;
        this.errorStack = stackTrace;
        this.durationMs = calculateDuration();
    }

    /**
     * 跳过执行
     */
    public void skip() {
        this.executeStatus = "SKIPPED";
        this.endTime = LocalDateTime.now();
        this.durationMs = 0L;
    }

    /**
     * 计算执行耗时
     */
    private Long calculateDuration() {
        if (startTime == null || endTime == null) {
            return null;
        }
        return ChronoUnit.MILLIS.between(startTime, endTime);
    }
}
