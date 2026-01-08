package com.zj.aiagent.application.agent;

import com.zj.aiagent.infrastructure.persistence.entity.AiAgentPO;

import java.util.List;

/**
 * Agent 应用服务接口
 *
 * @author zj
 * @since 2025-12-27
 */
public interface IAgentApplicationService {

    /**
     * 获取用户的 Agent 列表
     *
     * @param userId 用户ID
     * @return Agent列表（PO对象）
     */
    List<AiAgentPO> getUserAgentList(Long userId);

    /**
     * 获取 Agent 详情
     *
     * @param userId  用户ID
     * @param agentId Agent ID
     * @return Agent详情（PO对象）
     */
    AiAgentPO getAgentDetail(Long userId, Long agentId);

    /**
     * 保存或更新 Agent
     *
     * @param userId      用户ID
     * @param agentId     Agent ID (可为空，表示创建)
     * @param agentName   Agent名称
     * @param description 描述
     * @param graphJson   DAG配置JSON
     * @param status      状态
     * @return Agent ID
     */
    String saveAgent(Long userId, String agentId, String agentName,
            String description, String graphJson, Integer status);

    /**
     * 删除 Agent
     *
     * @param userId  用户ID
     * @param agentId Agent ID
     */
    void deleteAgent(Long userId, Long agentId);

    /**
     * 发布 Agent
     *
     * @param userId  用户ID
     * @param agentId Agent ID
     */
    void publishAgent(Long userId, Long agentId);
}
