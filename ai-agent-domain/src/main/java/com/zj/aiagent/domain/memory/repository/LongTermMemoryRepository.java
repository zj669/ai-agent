package com.zj.aiagent.domain.memory.repository;

import com.zj.aiagent.domain.memory.entity.Memory;

import java.util.List;

/**
 * 长期记忆存储接口
 * <p>
 * 定义长期记忆的持久化和检索技术接口，具体实现由基础设施层提供
 * （如向量数据库、Elasticsearch 等）
 */
public interface LongTermMemoryRepository {

    /**
     * 保存长期记忆
     *
     * @param executionId 执行ID
     * @param memory      记忆对象
     */
    void save(String executionId, Memory memory);

    /**
     * 检索相关的长期记忆
     *
     * @param executionId 执行ID
     * @param query       查询文本
     * @param topK        返回top K个相关记忆
     * @return 相关记忆列表（按相关性排序）
     */
    List<Memory> retrieve(String executionId, String query, int topK);

    /**
     * 清除长期记忆
     *
     * @param executionId 执行ID
     */
    void clear(String executionId);
}
