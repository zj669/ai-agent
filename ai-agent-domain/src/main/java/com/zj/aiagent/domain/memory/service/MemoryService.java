package com.zj.aiagent.domain.memory.service;

import com.zj.aiagent.domain.memory.MemoryProvider;
import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.Memory;
import com.zj.aiagent.domain.memory.repository.ChatHistoryRepository;
import com.zj.aiagent.domain.memory.repository.LongTermMemoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Memory 领域服务
 * <p>
 * 实现记忆管理的业务逻辑，依赖技术接口（Repository）
 */
@Slf4j
@Service
@AllArgsConstructor
public class MemoryService implements MemoryProvider {

    private final ChatHistoryRepository chatHistoryRepository;
    private final LongTermMemoryRepository longTermMemoryRepository;

    @Override
    public List<ChatMessage> loadChatHistory(String executionId, int maxMessages) {
        log.debug("[{}] 加载对话历史，最多 {} 条", executionId, maxMessages);
        return chatHistoryRepository.load(executionId, maxMessages);
    }

    @Override
    public void saveChatMessage(String executionId, ChatMessage message) {
        log.debug("[{}] 保存对话消息: {} - {}",
                executionId,
                message.getRole(),
                message.getContent().substring(0, Math.min(50, message.getContent().length())));
        chatHistoryRepository.save(executionId, message);
    }

    @Override
    public List<Memory> loadLongTermMemory(String executionId, String query) {
        log.debug("[{}] 检索长期记忆: {}", executionId, query);
        // 默认返回 top 10
        return longTermMemoryRepository.retrieve(executionId, query, 10);
    }

    @Override
    public void saveLongTermMemory(String executionId, Memory memory) {
        log.debug("[{}] 保存长期记忆: {}", executionId, memory.getType());
        longTermMemoryRepository.save(executionId, memory);
    }

    @Override
    public void clearChatHistory(String executionId) {
        log.info("[{}] 清除对话历史", executionId);
        chatHistoryRepository.clear(executionId);
    }
}
