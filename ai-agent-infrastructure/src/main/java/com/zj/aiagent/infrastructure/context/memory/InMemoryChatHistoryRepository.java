package com.zj.aiagent.infrastructure.context.memory;

import com.zj.aiagent.domain.memory.entity.ChatMessage;
import com.zj.aiagent.domain.memory.entity.NodeExecutionRecord;
import com.zj.aiagent.domain.memory.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
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
@ConditionalOnProperty(name = "memory.storage", havingValue = "memory", matchIfMissing = false)
public class InMemoryChatHistoryRepository implements ChatHistoryRepository {

    // 临时存储: executionId -> 消息列表
    private final Map<String, List<ChatMessage>> chatHistoryStore = new ConcurrentHashMap<>();

    @Override
    public void save(String executionId, ChatMessage message) {
        chatHistoryStore.computeIfAbsent(executionId, k -> new ArrayList<>()).add(message);
        log.debug("[{}] [InMemory] 保存消息: {} - {}",
                executionId,
                message.getRole(),
                message.getContent() != null && message.getContent().length() > 50
                        ? message.getContent().substring(0, 50)
                        : message.getContent());
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

    @Override
    public List<ChatMessage> loadWithNodeExecutions(String conversationId, int maxMessages) {
        // 内存实现：暂时返回普通消息，不包含节点详情
        log.warn("[{}] [InMemory] loadWithNodeExecutions 内存实现暂不支持节点详情", conversationId);
        return load(conversationId, maxMessages);
    }

    @Override
    public void saveNodeExecution(String conversationId, Long instanceId, NodeExecutionRecord record) {
        // 内存实现：暂不保存节点执行记录
        log.warn("[{}] [InMemory] saveNodeExecution 内存实现暂不支持", conversationId);
    }

    @Override
    public List<NodeExecutionRecord> loadNodeExecutions(Long instanceId) {
        // 内存实现：暂时返回空列表
        log.warn("[InMemory] loadNodeExecutions 内存实现暂不支持");
        return Collections.emptyList();
    }

    @Override
    public List<String> queryConversationIds(Long userId, String agentId) {
        // 内存实现：从 chatHistoryStore 的键中提取
        log.warn("[InMemory] queryConversationIds 内存实现返回所有键");
        return new ArrayList<>(chatHistoryStore.keySet());
    }
}
