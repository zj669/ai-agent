package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.valobj.Checkpoint;

import java.util.Optional;

/**
 * 检查点仓储接口（端口）
 * 用于持久化和恢复执行状态
 */
public interface CheckpointRepository {

    /**
     * 保存检查点
     * 
     * @param checkpoint 检查点
     */
    void save(Checkpoint checkpoint);

    /**
     * 获取最新检查点
     * 
     * @param executionId 执行ID
     * @return 最新的检查点
     */
    Optional<Checkpoint> findLatest(String executionId);

    /**
     * 获取暂停点
     * 
     * @param executionId 执行ID
     * @return 暂停点（如果存在）
     */
    Optional<Checkpoint> findPausePoint(String executionId);

    /**
     * 删除执行的所有检查点
     * 
     * @param executionId 执行ID
     */
    void deleteByExecutionId(String executionId);
}
