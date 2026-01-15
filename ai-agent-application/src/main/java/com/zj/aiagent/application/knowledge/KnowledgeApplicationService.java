package com.zj.aiagent.application.knowledge;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeDataset;
import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import com.zj.aiagent.domain.knowledge.port.FileStorageService;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDatasetRepository;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 知识库应用服务
 * 职责：编排知识库和文档的 CRUD，协调领域对象、仓储和基础设施服务
 * 
 * 遵循 DDD 应用层规范：
 * - 仅做编排，不含业务逻辑（业务逻辑在 Domain 层）
 * - 管理事务边界 (@Transactional)
 * - 调用领域服务、仓储和基础设施服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeApplicationService {

    private final KnowledgeDatasetRepository datasetRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final AsyncDocumentProcessor asyncDocumentProcessor;

    @Value("${minio.bucket-name:knowledge-files}")
    private String bucketName;

    // ========== 知识库管理 ==========

    /**
     * 创建知识库
     * 
     * @param name        知识库名称
     * @param description 描述
     * @param userId      用户 ID
     * @param agentId     绑定的 Agent ID（可选）
     * @return 创建的知识库
     */
    @Transactional
    public KnowledgeDataset createDataset(String name, String description, Long userId, Long agentId) {
        log.info("创建知识库: name={}, userId={}, agentId={}", name, userId, agentId);

        // 构建领域对象
        KnowledgeDataset dataset = KnowledgeDataset.builder()
                .datasetId(UUID.randomUUID().toString())
                .name(name)
                .description(description)
                .userId(userId)
                .agentId(agentId)
                .documentCount(0)
                .totalChunks(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // 持久化
        return datasetRepository.save(dataset);
    }

    /**
     * 查询用户的所有知识库
     * 
     * @param userId 用户 ID
     * @return 知识库列表
     */
    public List<KnowledgeDataset> listDatasetsByUser(Long userId) {
        log.info("查询用户知识库列表: userId={}", userId);
        return datasetRepository.findByUserId(userId);
    }

    /**
     * 查询知识库详情
     * 
     * @param datasetId 知识库 ID
     * @return 知识库对象
     */
    public KnowledgeDataset getDataset(String datasetId) {
        return datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + datasetId));
    }

    /**
     * 删除知识库
     * 删除知识库及其所有文档（包括文件和向量）
     * 
     * @param datasetId 知识库 ID
     */
    @Transactional
    public void deleteDataset(String datasetId) {
        log.info("删除知识库: datasetId={}", datasetId);

        // 1. 查询知识库
        KnowledgeDataset dataset = getDataset(datasetId);

        // 2. 删除所有文档（分页查询防止OOM）
        Page<KnowledgeDocument> documentsPage;
        int pageNum = 0;
        do {
            documentsPage = documentRepository.findByDatasetId(
                    datasetId,
                    org.springframework.data.domain.PageRequest.of(pageNum++, 100));

            for (KnowledgeDocument document : documentsPage.getContent()) {
                deleteDocumentInternal(document);
            }
        } while (documentsPage.hasNext());

        // 3. 删除知识库记录
        datasetRepository.deleteById(datasetId);

        log.info("知识库删除完成: datasetId={}", datasetId);
    }

    // ========== 文档管理 ==========

    /**
     * 上传文档到知识库
     * 
     * @param datasetId      知识库 ID
     * @param file           上传的文件
     * @param chunkingConfig 分块配置（可选）
     * @return 创建的文档对象
     */
    @Transactional
    public KnowledgeDocument uploadDocument(
            String datasetId,
            MultipartFile file,
            ChunkingConfig chunkingConfig) {

        log.info("上传文档: datasetId={}, filename={}, size={}",
                datasetId, file.getOriginalFilename(), file.getSize());

        // 1. 验证知识库存在
        KnowledgeDataset dataset = getDataset(datasetId);

        try {
            // 2. 生成文档 ID 和存储路径
            String documentId = UUID.randomUUID().toString();
            String objectName = String.format("%s/%s/%s",
                    datasetId, documentId, file.getOriginalFilename());

            // 3. 上传文件到 MinIO
            String fileUrl = fileStorageService.upload(
                    bucketName,
                    objectName,
                    file.getInputStream(),
                    file.getSize());

            // 4. 构建文档聚合根
            KnowledgeDocument document = KnowledgeDocument.builder()
                    .documentId(documentId)
                    .datasetId(datasetId)
                    .filename(file.getOriginalFilename())
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .chunkingConfig(chunkingConfig != null ? chunkingConfig : ChunkingConfig.builder().build())
                    .uploadedAt(Instant.now())
                    .build();

            // 5. 保存文档记录（状态：PENDING）
            document = documentRepository.save(document);

            // 6. 更新知识库统计（调用领域行为）
            dataset.addDocument(document);
            datasetRepository.save(dataset);

            // 7. 触发异步处理（解析、分块、向量化）
            asyncDocumentProcessor.processDocumentAsync(document);

            log.info("文档上传成功，异步处理已触发: documentId={}", documentId);
            return document;

        } catch (Exception e) {
            log.error("文档上传失败: datasetId={}, filename={}",
                    datasetId, file.getOriginalFilename(), e);
            throw new RuntimeException("文档上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询知识库的文档列表（分页）
     * 
     * @param datasetId 知识库 ID
     * @param pageable  分页参数
     * @return 文档分页结果
     */
    public Page<KnowledgeDocument> listDocuments(String datasetId, Pageable pageable) {
        log.info("查询文档列表: datasetId={}, page={}", datasetId, pageable.getPageNumber());
        return documentRepository.findByDatasetId(datasetId, pageable);
    }

    /**
     * 查询文档详情
     * 
     * @param documentId 文档 ID
     * @return 文档对象
     */
    public KnowledgeDocument getDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));
    }

    /**
     * 删除文档
     * 删除文档记录、MinIO 文件和向量数据
     * 
     * @param documentId 文档 ID
     */
    @Transactional
    public void deleteDocument(String documentId) {
        log.info("删除文档: documentId={}", documentId);

        // 1. 查询文档
        KnowledgeDocument document = getDocument(documentId);

        // 2. 删除文档及相关资源
        deleteDocumentInternal(document);

        // 3. 更新知识库统计
        KnowledgeDataset dataset = getDataset(document.getDatasetId());
        dataset.removeDocument(document.getTotalChunks());
        datasetRepository.save(dataset);

        log.info("文档删除完成: documentId={}", documentId);
    }

    /**
     * 内部方法：删除文档及其关联资源
     * 
     * @param document 文档对象
     */
    private void deleteDocumentInternal(KnowledgeDocument document) {
        try {
            // 1. 删除向量数据（通过异步处理器）
            asyncDocumentProcessor.deleteDocumentVectors(document.getDocumentId());

            // 2. 删除 MinIO 文件
            String objectName = extractObjectName(document.getFileUrl());
            fileStorageService.delete(bucketName, objectName);

            // 3. 删除数据库记录
            documentRepository.deleteById(document.getDocumentId());

        } catch (Exception e) {
            log.error("删除文档资源失败: documentId={}", document.getDocumentId(), e);
            // 继续删除，避免部分删除导致的数据不一致
        }
    }

    /**
     * 从文件 URL 提取对象名称
     * 
     * @param fileUrl MinIO 文件 URL
     * @return 对象名称
     */
    private String extractObjectName(String fileUrl) {
        // URL 格式示例:
        // http://localhost:9000/knowledge-files/datasetId/documentId/filename.pdf
        // 提取 bucket 之后的路径
        int bucketIndex = fileUrl.indexOf(bucketName);
        if (bucketIndex == -1) {
            throw new IllegalArgumentException("无效的文件 URL: " + fileUrl);
        }
        return fileUrl.substring(bucketIndex + bucketName.length() + 1);
    }
}
