package com.zj.aiagent.domain.agent.dag.repository;

import com.zj.aiagent.domain.agent.dag.entity.DagExecutionInstance;

/**
 * DAG 执行实例仓储接口
 */
public interface IDagExecutionRepository {

    /**
     * 保存执行实例
     *
     * @param instance 执行实例
     * @return 保存后的实例(包含生成的ID)
     */
    DagExecutionInstance save(DagExecutionInstance instance);

    /**
     * 更新执行实例
     *
     * @param instance 执行实例
     */
    void update(DagExecutionInstance instance);

    /**
     * 根据ID查询执行实例
     *
     * @param id 实例ID
     * @return 执行实例,不存在返回 null
     */
    DagExecutionInstance findById(Long id);

    /**
     * 根据会话ID查询最新的执行实例
     *
     * @param conversationId 会话ID
     * @return 执行实例,不存在返回 null
     */
    DagExecutionInstance findByConversationId(String conversationId);
}
