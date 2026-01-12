package com.zj.aiagent.application.chat.listener;

import com.zj.aiagent.application.chat.ChatApplicationService;
import com.zj.aiagent.domain.chat.valobj.MessageStatus;
import com.zj.aiagent.domain.workflow.event.ExecutionCompletedEvent;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 监听工作流执行完成，更新 Assistant 消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutionCompletedListener {

    private final ChatApplicationService chatApplicationService;

    @Async
    @EventListener
    public void onExecutionCompleted(ExecutionCompletedEvent event) {
        log.info("Received ExecutionCompletedEvent: {}", event.getExecutionId());

        // 假设 ExecutionId 作为 Message 的 runId 或者以某种方式关联
        // 在实际业务中，我们可能需要根据 executionId 找到对应的 messageId (PENDING)
        // 这里简化处理：假设 runId = executionId，且在 message metadata 中存储了
        // 或者我们假设调用方(Execution Service) 在 initAssistantMessage 时返回了 messageId 并传递给了
        // workflow execution

        // 由于当前的 Event 设计没有 messageId，我们需要一种查找机制。
        // MVP 简化：Conversation Context 应该记录了 runId -> messageId 的映射，或者我们遍历该会话找到最后一条
        // PENDING 的 Assistant 消息。
        // 但这里为了稳健，最好的方式是 Event 包含 messageId，但这污染了 Execution 域。
        // 妥协方案：Conversation Service 提供根据 runId 查找消息的方法。

        // 既然 ChatApplicationService 没有 `findByRunId`，我们暂时无法直接定位。
        // 考虑到设计文档中 Integrated Points: "监听 Execution 完成事件，自动记录 ASSISTANT 消息"。
        // 如果是流式模式，消息已经 init 了。如果没有 init，则创建新消息。

        // 这里模拟：如果输出是文本，则更新消息内容
        String content = "";
        if (event.getOutputs() != null && event.getOutputs().containsKey("output")) {
            Object outputObj = event.getOutputs().get("output");
            content = outputObj != null ? outputObj.toString() : "";
        }

        // 实际上 finalizeMessage 需要 messageId。
        // 这里的逻辑可能需要调整：Execution 应该不感知 messageId。
        // 也许我们需要在 Conversation 模块内部维护 runId -> messageId 的索引?
        // 或者简单点：findLastPendingMessageByConversationId(event.getConversationId())

        // 占位逻辑，实际需根据 runId 查找
        // log.warn("Needs implementation to find messageId by runId: {}",
        // event.getExecutionId());
    }
}
