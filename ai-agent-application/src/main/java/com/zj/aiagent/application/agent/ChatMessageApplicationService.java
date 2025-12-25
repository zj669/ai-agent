package com.zj.aiagent.application.agent;

import cn.hutool.core.util.IdUtil;
import com.zj.aiagent.application.agent.command.ChatCommand;
import com.zj.aiagent.domain.agent.chat.entity.ChatMessageEntity;
import com.zj.aiagent.domain.agent.chat.repository.IChatMessageRepository;
import com.zj.aiagent.domain.workflow.IWorkflowService;
import com.zj.aiagent.domain.workflow.entity.WorkflowGraph;
import com.zj.aiagent.infrastructure.listener.SSEWorkflowStateListener;
import com.zj.aiagent.infrastructure.parse.WorkflowGraphFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

/**
 * 聊天消息应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageApplicationService {

    private final IChatMessageRepository chatMessageRepository;
    private final WorkflowGraphFactory workflowGraphFactory;
    private final IWorkflowService workflowService;

    public void chat(ChatCommand command) {
        WorkflowGraph graph = workflowGraphFactory.loadDagByAgentId(command.getAgentId());
        String conversationId = command.getConversationId();
        ResponseBodyEmitter emitter = command.getEmitter();
        if (conversationId == null) {
            conversationId = String.valueOf(IdUtil.getSnowflake(1, 1).nextId());
        }
        workflowService.execute(graph, conversationId, new SSEWorkflowStateListener(emitter));
    }
    /**
     * 保存用户消息
     */
    public ChatMessageEntity saveUserMessage(String conversationId, Long agentId,
            Long userId, String content) {
        ChatMessageEntity message = ChatMessageEntity.builder()
                .conversationId(conversationId)
                .agentId(agentId)
                .userId(userId)
                .role(ChatMessageEntity.MessageRole.USER)
                .content(content)
                .timestamp(System.currentTimeMillis())
                .isError(false)
                .build();

        ChatMessageEntity saved = chatMessageRepository.save(message);
        log.info("保存用户消息: conversationId={}, agentId={}, userId={}",
                conversationId, agentId, userId);
        return saved;
    }

    /**
     * 保存 AI 回复消息
     */
    public ChatMessageEntity saveAssistantMessage(String conversationId, Long agentId,
            Long instanceId, String finalResponse,
            boolean isError, String errorMessage) {
        ChatMessageEntity message = ChatMessageEntity.builder()
                .conversationId(conversationId)
                .agentId(agentId)
                .instanceId(instanceId)
                .role(ChatMessageEntity.MessageRole.ASSISTANT)
                .finalResponse(finalResponse)
                .timestamp(System.currentTimeMillis())
                .isError(isError)
                .errorMessage(errorMessage)
                .build();

        ChatMessageEntity saved = chatMessageRepository.save(message);
        log.info("保存AI回复: conversationId={}, agentId={}, instanceId={}, isError={}",
                conversationId, agentId, instanceId, isError);
        return saved;
    }

    /**
     * 查询会话历史消息（包含节点执行详情）
     */
    public List<ChatMessageEntity> getConversationHistory(String conversationId) {
        log.info("查询会话历史: conversationId={}", conversationId);
        return chatMessageRepository.findByConversationId(conversationId);
    }

    /**
     * 统计会话消息数量
     */
    public int countConversationMessages(String conversationId) {
        return chatMessageRepository.countByConversationId(conversationId);
    }

    /**
     * 删除会话所有消息
     */
    public void deleteConversationMessages(String conversationId) {
        log.info("删除会话消息: conversationId={}", conversationId);
        chatMessageRepository.deleteByConversationId(conversationId);
    }
}
