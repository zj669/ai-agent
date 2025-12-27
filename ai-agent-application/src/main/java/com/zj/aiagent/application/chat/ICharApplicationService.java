package com.zj.aiagent.application.chat;

import com.zj.aiagent.application.chat.command.ChatCommand;
import com.zj.aiagent.domain.memory.dto.ChatHistoryDTO;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

public interface ICharApplicationService {
    void chat(ChatCommand command);

    List<String> queryHistoryId(Long userId, String agentId);

    /**
     * 查询历史消息
     *
     * @param userId         用户ID
     * @param agentId        Agent ID
     * @param conversationId 会话ID
     * @return 历史消息列表
     */
    List<ChatHistoryDTO> queryHistory(Long userId, String agentId, String conversationId);

    /**
     * 获取执行快照
     *
     * @param userId         用户ID
     * @param agentId        Agent ID
     * @param conversationId 会话ID
     * @return 执行快照，如果不存在返回 null
     */
    com.zj.aiagent.domain.workflow.entity.ExecutionContextSnapshot getSnapshot(Long userId, String agentId,
            String conversationId);

    /**
     * 更新快照
     *
     * @param userId         用户ID
     * @param agentId        Agent ID
     * @param conversationId 会话ID
     * @param nodeId         节点ID
     * @param stateData      更新后的状态数据
     */
    void updateSnapshot(Long userId, String agentId, String conversationId, String nodeId,
            java.util.Map<String, Object> stateData);

    /**
     * 人工审核
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     * @param nodeId         节点ID
     * @param approved       是否批准
     * @param agentId        Agent ID
     * @param emitter        SSE 发射器（审批通过后用于推送恢复执行的状态）
     */
    void review(Long userId, String conversationId, String nodeId, Boolean approved, String agentId,
            ResponseBodyEmitter emitter);

    /**
     * 取消执行
     *
     * @param command 取消命令
     */
    void cancelExecution(com.zj.aiagent.application.agent.command.CancelExecutionCommand command);
}
