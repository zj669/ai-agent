package com.zj.aiagent.domain.workflow.interfaces;

import com.zj.aiagent.domain.workflow.base.WorkflowState;

/**
 * 检查点接口
 *
 * - 自动保存每个节点执行后的状态
 * - 支持从任意节点恢复执行
 */
public interface Checkpointer {

    /**
     * 保存检查点
     * 
     * @param executionId 执行ID
     * @param nodeId      当前节点ID
     * @param state       当前状态
     */
    void save(String executionId, String nodeId, WorkflowState state);

    /**
     * 加载最新检查点
     * 
     * @param executionId 执行ID
     * @return 最新的状态，如果不存在返回 null
     */
    WorkflowState load(String executionId);

    /**
     * 加载指定节点的检查点
     * 
     * @param executionId 执行ID
     * @param nodeId      节点ID
     * @return 该节点执行后的状态
     */
    WorkflowState loadAt(String executionId, String nodeId);

    /**
     * 获取最后执行的节点ID
     * 
     * @param executionId 执行ID
     * @return 最后执行的节点ID
     */
    String getLastNodeId(String executionId);

    /**
     * 清除检查点
     * 
     * @param executionId 执行ID
     */
    void clear(String executionId);
}
