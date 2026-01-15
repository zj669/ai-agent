package com.zj.aiagent.domain.knowledge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 知识库聚合根
 * 管理知识库的生命周期、文档集合
 * 
 * 采用单 Collection + Metadata 过滤策略：
 * - 所有知识库共享 "agent_knowledge_base" Collection
 * - 通过 Metadata { "agentId": xxx, "datasetId": xxx } 进行隔离
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDataset {
    /**
     * 知识库 ID（聚合根标识）
     */
    private String datasetId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 所有者用户 ID
     */
    private Long userId;

    /**
     * 绑定的 Agent ID（可选）
     * 如果指定，则该知识库仅供该 Agent 使用
     */
    private Long agentId;

    /**
     * 文档数量统计
     */
    @Builder.Default
    private Integer documentCount = 0;

    /**
     * 总分块数统计
     */
    @Builder.Default
    private Integer totalChunks = 0;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    // ========== 领域行为 ==========

    /**
     * 添加文档到知识库
     * 更新统计信息
     */
    public void addDocument(KnowledgeDocument document) {
        this.documentCount++;
        this.updatedAt = Instant.now();
    }

    /**
     * 从知识库移除文档
     * 更新统计信息
     * 
     * @param chunkCount 要移除的分块数
     */
    public void removeDocument(int chunkCount) {
        this.documentCount--;
        this.totalChunks -= chunkCount;
        this.updatedAt = Instant.now();
    }

    /**
     * 构建向量检索的 Metadata Filter
     * 用于在统一的 Collection 中过滤出属于本知识库的向量
     * 
     * @return Metadata 过滤条件 Map
     */
    public Map<String, Object> buildMetadataFilter() {
        Map<String, Object> filter = new HashMap<>();
        filter.put("datasetId", this.datasetId);
        if (this.agentId != null) {
            filter.put("agentId", this.agentId);
        }
        return filter;
    }

    /**
     * 增加分块统计
     * 
     * @param chunkCount 新增的分块数
     */
    public void addChunks(int chunkCount) {
        this.totalChunks += chunkCount;
        this.updatedAt = Instant.now();
    }
}
