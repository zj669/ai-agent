package com.zj.aiagent.domain.agent.dag.repository;

import com.zj.aiagent.domain.agent.dag.entity.AiAgent;
import com.zj.aiagent.domain.agent.dag.entity.DagGraph;

/**
 * DAG仓储接口
 *
 * @author zj
 */
public interface IDagRepository {

    /**
     * 根据agentId查询Agent
     *
     * @param agentId Agent ID
     * @return Agent实体
     */
    AiAgent selectAiAgentByAgentId(String agentId);

    /**
     * 保存Agent配置
     * 如果agentId已存在则更新，否则插入新记录
     *
     * @param agent Agent实体
     * @return 保存后的Agent实体（包含ID）
     */
    AiAgent saveAgent(AiAgent agent);

    /**
     * 根据agentId和userId查询Agent
     * 用于权限校验
     *
     * @param agentId Agent ID
     * @param userId  用户ID
     * @return Agent实体，不存在或userId不匹配则返回null
     */
    AiAgent selectAiAgentByAgentIdAndUserId(String agentId, Long userId);

    /**
     * 查询用户在指定Agent下的所有去重会话ID
     *
     * @param userId  用户ID
     * @param agentId Agent ID
     * @return 去重的会话ID列表
     */
    java.util.List<String> findDistinctConversationIds(Long userId, Long agentId);
}
