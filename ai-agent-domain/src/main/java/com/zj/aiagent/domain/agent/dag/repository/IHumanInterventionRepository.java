package com.zj.aiagent.domain.agent.dag.repository;

import com.zj.aiagent.domain.agent.dag.context.ExecutionContextSnapshot;
import com.zj.aiagent.domain.agent.dag.context.HumanInterventionRequest;

/**
 * 人工介入仓储接口（领域层）
 * 基础设施层负责实现
 *
 * @author zj
 * @since 2025-12-23
 */
public interface IHumanInterventionRepository {

    /**
     * 保存暂停状态
     *
     * @param request 人工介入请求
     */
    void savePauseState(HumanInterventionRequest request);

    /**
     * 保存审核结果
     *
     * @param conversationId 会话ID
     * @param nodeId         节点ID
     * @param approved       是否批准
     * @param comments       审核备注
     * @param modifiedOutput 修改后的输出
     */
    void saveReviewResult(String conversationId, String nodeId,
            boolean approved, String comments, String modifiedOutput);

    /**
     * 加载状态（优先 Redis）
     *
     * @param conversationId 会话ID
     * @param nodeId         节点ID
     * @return 人工介入请求，如果不存在返回 null
     */
    HumanInterventionRequest loadState(String conversationId, String nodeId);

    /**
     * 清理已完成的审核状态
     *
     * @param conversationId 会话ID
     * @param nodeId         节点ID
     */
    void cleanupCompletedState(String conversationId, String nodeId);

    /**
     * 查询暂停状态（根据 conversationId 查找，无需 nodeId）
     *
     * @param conversationId 会话ID
     * @return 人工介入请求，如果不存在返回 null
     */
    HumanInterventionRequest findPausedState(String conversationId);

    /**
     * 加载完整上下文（所有节点执行结果）
     *
     * @param conversationId 会话ID
     * @return 节点执行结果Map，key为nodeId，value为执行结果
     */
    java.util.Map<String, Object> loadFullContext(String conversationId);

    /**
     * 更新上下文中的指定字段
     *
     * @param conversationId 会话ID
     * @param modifications  需要修改的字段，key为nodeId，value为新的执行结果
     */
    void updateContext(String conversationId, java.util.Map<String, Object> modifications);

    /**
     * 保存完整执行上下文快照
     *
     * @param snapshot 快照对象
     */
    void saveContextSnapshot(ExecutionContextSnapshot snapshot);

    /**
     * 加载完整执行上下文快照
     *
     * @param conversationId 会话ID
     * @return 快照对象，如果不存在返回 null
     */
    ExecutionContextSnapshot loadContextSnapshot(String conversationId);

    /**
     * 更新快照中的可编辑字段
     *
     * @param conversationId 会话ID
     * @param modifications  需要修改的字段Map
     */
    void updateSnapshotEditableFields(String conversationId, java.util.Map<String, Object> modifications);

    /**
     * 删除执行上下文快照
     *
     * @param conversationId 会话ID
     */
    void deleteContextSnapshot(String conversationId);
}
