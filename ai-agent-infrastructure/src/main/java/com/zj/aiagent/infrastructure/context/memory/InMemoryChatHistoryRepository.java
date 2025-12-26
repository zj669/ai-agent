package com.zj.aiagent.infrastructure.context.memory;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatHistory Repository 内存实现
 * <p>
 * 基于内存存储的临时实现，生产环境应替换为 Redis 或数据库
 */
@Slf4j
@Repository
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    // 临时存储: executionId -> 消息列表
    private final Map<String, List<ChatMessage>> chatHistoryStore = new ConcurrentHashMap<>();

    @Override
    public void save(String executionId, ChatMessage message) {
        chatHistoryStore.computeIfAbsent(executionId, k -> new ArrayList<>()).add(message);
        log.debug("[{}] [InMemory] 保存消息: {} - {}",
                executionId,
                message.getRole(),
                message.getContent().substring(0, Math.min(50, message.getContent().length())));
    }

    @Override
    public List<ChatMessage> load(String executionId, int maxMessages) {
        List<ChatMessage> history = chatHistoryStore.getOrDefault(executionId, new ArrayList<>());

        // 返回最新的 maxMessages 条
        int fromIndex = Math.max(0, history.size() - maxMessages);
        List<ChatMessage> result = new ArrayList<>(history.subList(fromIndex, history.size()));

        log.debug("[{}] [InMemory] 加载对话历史: {} 条", executionId, result.size());
        return result;
    }

    @Override
    public void clear(String executionId) {
        chatHistoryStore.remove(executionId);
        log.info("[{}] [InMemory] 清除对话历史", executionId);
    }
}
