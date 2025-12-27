package com.zj.aiagent.infrastructure.persistence.repository;

import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;

import java.util.List;

/**
 * Agent 仓储接口
 */
public interface IAiAgentRepository {

    /**
     * 根据ID查询Agent
     */
    AiAgentPO getById(Long id);

    /**
     * 查询用户的Agent列表
     */
    List<AiAgentPO> findByUserId(Long userId);

    /**
     * 保存或更新Agent
     */
    void saveOrUpdate(AiAgentPO agent);

    /**
     * 删除Agent
     */
    void deleteById(Long id);

    /**
     * 更新状态
     */
    void updateStatus(Long id, Integer status);
}
