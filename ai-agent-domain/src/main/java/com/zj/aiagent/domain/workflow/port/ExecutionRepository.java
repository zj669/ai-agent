package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.entity.Execution;

import java.util.Optional;

/**
 * 执行仓储接口（端口）
 * 用于持久化和查询执行聚合根
 */
public interface ExecutionRepository {

    /**
     * 保存执行
     * 
     * @param execution 执行聚合根
     */
    void save(Execution execution);

    /**
     * 根据ID查找执行
     * 
     * @param executionId 执行ID
     * @return 执行聚合根
     */
    Optional<Execution> findById(String executionId);

    /**
     * 更新执行（带乐观锁）
     * 
     * @param execution 执行聚合根
     * @throws org.springframework.dao.OptimisticLockingFailureException 版本冲突时抛出
     */
    void update(Execution execution);

    /**
     * 删除执行
     * 
     * @param executionId 执行ID
     */
    void delete(String executionId);

    /**
     * 根据会话ID查询执行历史
     */
    java.util.List<Execution> findByConversationId(String conversationId);
}
