package com.zj.aiagent.application.chat;

import com.zj.aiagent.application.chat.command.ChatCommand;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

public interface ICharApplicationService {
    void chat(ChatCommand command);

    List<String> queryHistoryId(Long userId, String agentId);

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
}
