package com.zj.aiagent.domain.agent.dag.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DAG 执行实例 - DAG 聚合的一部分
 * 记录 DAG 的执行状态和运行时上下文
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DagExecutionInstance {

    /**
     * 实例ID
     */
    private Long id;

    /**
     * 关联的 Agent ID
     */
    private Long agentId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 当前节点ID
     */
    private String currentNodeId;

    /**
     * 运行状态: RUNNING, PAUSED, COMPLETED, FAILED
     */
    private String status;

    /**
     * 运行时上下文 JSON
     */
    private String runtimeContextJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 开始执行
     */
    public void start() {
        this.status = "RUNNING";
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 更新执行进度
     */
    public void updateProgress(String nodeId, String contextJson) {
        this.currentNodeId = nodeId;
        this.runtimeContextJson = contextJson;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 完成执行
     */
    public void complete() {
        this.status = "COMPLETED";
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 执行失败
     */
    public void fail() {
        this.status = "FAILED";
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 暂停执行
     */
    public void pause(String nodeId) {
        this.status = "PAUSED";
        this.currentNodeId = nodeId;
        this.updateTime = LocalDateTime.now();
    }
}
