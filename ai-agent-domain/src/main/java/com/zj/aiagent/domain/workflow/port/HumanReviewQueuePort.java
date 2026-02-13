package com.zj.aiagent.domain.workflow.port;

import java.util.Set;

/**
 * 人工审核队列端口
 *
 * Domain 层定义的接口，用于管理待人工审核的工作流执行队列
 * Infrastructure 层负责实现
 *
 * 职责：
 * - 添加执行到待审核队列
 * - 从待审核队列移除执行
 * - 检查执行是否在待审核队列中
 * - 获取所有待审核的执行ID
 */
public interface HumanReviewQueuePort {

    /**
     * 添加到待审核队列
     *
     * @param executionId 执行ID
     */
    void addToPendingQueue(String executionId);

    /**
     * 从待审核队列移除
     *
     * @param executionId 执行ID
     */
    void removeFromPendingQueue(String executionId);

    /**
     * 检查是否在待审核队列中
     *
     * @param executionId 执行ID
     * @return true-在队列中, false-不在队列中
     */
    boolean isInPendingQueue(String executionId);

    /**
     * 获取所有待审核的执行ID
     *
     * @return 待审核的执行ID集合
     */
    Set<String> getPendingExecutionIds();
}
