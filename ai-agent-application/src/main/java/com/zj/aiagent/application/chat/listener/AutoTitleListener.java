package com.zj.aiagent.application.chat.listener;

import com.zj.aiagent.application.chat.ChatApplicationService;
import com.zj.aiagent.application.chat.event.MessageAppendedEvent;
import com.zj.aiagent.domain.chat.entity.Conversation;
import com.zj.aiagent.domain.chat.port.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 自动生成标题监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoTitleListener {

    private final ConversationRepository conversationRepository;
    // 实际项目中可能需要注入 AgentExecutionService 或 LLMClient

    @Async
    @EventListener
    public void onMessageAppended(MessageAppendedEvent event) {
        String conversationId = event.getMessage().getConversationId();

        // 简单逻辑：如果是第二条消息 (通常是一问一答后)，则生成标题
        // 这里只是示例，实际需要 count
        // 由于 repository.findMessages 是分页的，不太好直接 count
        // 我们可以简单地检查是否还是默认标题

        conversationRepository.findById(conversationId).ifPresent(conversation -> {
            if (conversation.getTitle().startsWith("New Chat")) {
                log.info("Generating title for conversation: {}", conversationId);

                // 模拟 LLM 调用耗时
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 实际逻辑：调用 LLM 生成标题
                // String newTitle = llmClient.generateTitle(firstUserMessage,
                // firstAssistantMessage);
                String newTitle = "Smart Chat " + conversationId.substring(0, 4); // Stub

                conversation.updateTitle(newTitle);
                conversationRepository.save(conversation);
                log.info("Updated title to: {}", newTitle);
            }
        });
    }
}
