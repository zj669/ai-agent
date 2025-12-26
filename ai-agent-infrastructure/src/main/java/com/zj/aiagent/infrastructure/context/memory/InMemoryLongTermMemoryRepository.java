package com.zj.aiagent.infrastructure.context.memory;

import com.zj.aiagent.domain.memory.entity.Memory;
import com.zj.aiagent.domain.memory.repository.LongTermMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LongTermMemory Repository 内存实现
 * <p>
 * 基于内存存储的临时实现，生产环境应替换为向量数据库
 */
@Slf4j
@Repository
public class InMemoryLongTermMemoryRepository implements LongTermMemoryRepository {

    // 临时存储: executionId -> 记忆列表
    private final Map<String, List<Memory>> memoryStore = new ConcurrentHashMap<>();

    @Override
    public void save(String executionId, Memory memory) {
        memoryStore.computeIfAbsent(executionId, k -> new ArrayList<>()).add(memory);
        log.debug("[{}] [InMemory] 保存长期记忆: {}", executionId, memory.getType());
    }

    @Override
    public List<Memory> retrieve(String executionId, String query, int topK) {
        // TODO: 实现向量检索逻辑
        // 当前简单返回所有记忆
        List<Memory> allMemories = memoryStore.getOrDefault(executionId, new ArrayList<>());
        int limit = Math.min(topK, allMemories.size());

        log.debug("[{}] [InMemory] 检索长期记忆: query={}, 返回 {} 条",
                executionId, query, limit);

        return new ArrayList<>(allMemories.subList(Math.max(0, allMemories.size() - limit), allMemories.size()));
    }

    @Override
    public void clear(String executionId) {
        memoryStore.remove(executionId);
        log.info("[{}] [InMemory] 清除长期记忆", executionId);
    }
}
