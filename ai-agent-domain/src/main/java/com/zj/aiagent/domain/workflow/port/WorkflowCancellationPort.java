package com.zj.aiagent.domain.workflow.port;

/**
 * 工作流取消端口
 * 
 * Domain 层定义的接口，用于管理工作流的取消状态
 * Infrastructure 层负责实现
 * 
 * 职责：
 * - 标记工作流为已取消
 * - 检查工作流是否已取消
 */
public interface WorkflowCancellationPort {
    
    /**
     * 标记工作流为已取消
     * 
     * @param executionId 执行ID
     */
    void markAsCancelled(String executionId);
    
    /**
     * 检查工作流是否已取消
     * 
     * @param executionId 执行ID
     * @return true-已取消, false-未取消
     */
    boolean isCancelled(String executionId);
}
