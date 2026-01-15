package com.zj.aiagent.domain.knowledge.repository;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 知识文档仓储接口
 * 定义文档聚合根的持久化操作
 */
public interface KnowledgeDocumentRepository {
    /**
     * 保存文档（新增或更新）
     * 
     * @param document 文档聚合根
     * @return 保存后的文档
     */
    KnowledgeDocument save(KnowledgeDocument document);

    /**
     * 根据 ID 查询文档
     * 
     * @param documentId 文档 ID
     * @return Optional 包装的文档
     */
    Optional<KnowledgeDocument> findById(String documentId);

    /**
     * 分页查询知识库的文档列表
     * 
     * @param datasetId 知识库 ID
     * @param pageable  分页参数
     * @return 文档分页结果
     */
    Page<KnowledgeDocument> findByDatasetId(String datasetId, Pageable pageable);

    /**
     * 删除文档
     * 
     * @param documentId 文档 ID
     */
    void deleteById(String documentId);
}
