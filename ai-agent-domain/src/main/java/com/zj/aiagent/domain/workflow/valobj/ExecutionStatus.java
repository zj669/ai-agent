package com.zj.aiagent.domain.workflow.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 执行状态枚举
 * 节点和执行流的生命周期状态
 */
@Getter
@AllArgsConstructor
public enum ExecutionStatus {
    /**
     * 待执行
     */
    PENDING(0),

    /**
     * 执行中
     */
    RUNNING(1),

    /**
     * 执行成功
     */
    SUCCEEDED(2),

    /**
     * 执行失败
     */
    FAILED(3),

    /**
     * 已跳过（条件分支未命中）
     */
    SKIPPED(4),

    /**
     * 暂停中（等待人工审核）
     */
    PAUSED(5),

    /**
     * 已取消
     */
    CANCELLED(6),

    /**
     * 暂停中（等待人工审核）
     */
    PAUSED_FOR_REVIEW(10);

    private final int code;
}
