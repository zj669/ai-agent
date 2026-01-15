package com.zj.aiagent.domain.knowledge.repository;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeDataset;

import java.util.List;
import java.util.Optional;

/**
 * 知识库仓储接口
 * 定义知识库聚合根的持久化操作
 */
public interface KnowledgeDatasetRepository {
    /**
     * 保存知识库（新增或更新）
     * 
     * @param dataset 知识库聚合根
     * @return 保存后的知识库
     */
    KnowledgeDataset save(KnowledgeDataset dataset);

    /**
     * 根据 ID 查询知识库
     * 
     * @param datasetId 知识库 ID
     * @return Optional 包装的知识库
     */
    Optional<KnowledgeDataset> findById(String datasetId);

    /**
     * 查询用户的所有知识库
     * 
     * @param userId 用户 ID
     * @return 知识库列表
     */
    List<KnowledgeDataset> findByUserId(Long userId);

    /**
     * 删除知识库
     * 
     * @param datasetId 知识库 ID
     */
    void deleteById(String datasetId);
}
