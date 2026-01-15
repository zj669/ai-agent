package com.zj.aiagent.application.knowledge;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import com.zj.aiagent.domain.knowledge.port.FileStorageService;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDatasetRepository;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.zj.aiagent.infrastructure.knowledge.SpringAIDocumentReaderAdapter;
import com.zj.aiagent.infrastructure.knowledge.SpringAITextSplitterAdapter;
import com.zj.aiagent.domain.memory.port.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异步文档处理器
 * 职责：异步处理文档的解析、分块、向量化和存储
 * 
 * 使用 @Async 注解实现异步处理，避免阻塞上传 API
 * 处理流程：
 * 1. 从 MinIO 下载文件
 * 2. 使用 Spring AI TikaDocumentReader 解析文档
 * 3. 使用 TokenTextSplitter 分块
 * 4. 为每个分块生成 Embedding 并存储到 Milvus
 * 5. 更新文档处理状态和进度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncDocumentProcessor {

    private final FileStorageService fileStorageService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDatasetRepository datasetRepository;
    private final SpringAIDocumentReaderAdapter documentReaderAdapter;
    private final SpringAITextSplitterAdapter textSplitterAdapter;
    private final VectorStore vectorStore;

    @Value("${minio.bucket-name:knowledge-files}")
    private String bucketName;

    /**
     * 异步处理文档
     * 
     * @param document 待处理的文档
     */
    @Async
    public void processDocumentAsync(KnowledgeDocument document) {
        log.info("开始异步处理文档: documentId={}, filename={}",
                document.getDocumentId(), document.getFilename());

        try {
            // 1. 更新状态为 PROCESSING
            document.startProcessing();
            documentRepository.save(document);

            // 2. 从 MinIO 下载文件
            String objectName = extractObjectName(document.getFileUrl());
            InputStream fileStream = fileStorageService.download(bucketName, objectName);

            // 3. 使用 Spring AI TikaDocumentReader 解析文档
            InputStreamResource resource = new InputStreamResource(fileStream);
            List<Document> parsedDocuments = documentReaderAdapter.readDocuments(resource);

            log.info("文档解析完成: documentId={}, parsedCount={}",
                    document.getDocumentId(), parsedDocuments.size());

            // 4. 使用 TokenTextSplitter 分块
            int chunkSize = document.getChunkingConfig().getChunkSize();
            int overlap = document.getChunkingConfig().getChunkOverlap();
            List<Document> chunks = textSplitterAdapter.split(parsedDocuments, chunkSize, overlap);

            // 设置总分块数
            document.setTotalChunksCount(chunks.size());
            documentRepository.save(document);

            log.info("文档分块完成: documentId={}, totalChunks={}",
                    document.getDocumentId(), chunks.size());

            // 5. 为每个分块生成向量并存储
            int processedCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);

                // 构建 Metadata（包含 datasetId, documentId, agentId 用于隔离）
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("documentId", document.getDocumentId());
                metadata.put("datasetId", document.getDatasetId());
                metadata.put("filename", document.getFilename());
                metadata.put("chunkIndex", i);

                // 如果知识库绑定了 Agent，添加 agentId
                datasetRepository.findById(document.getDatasetId()).ifPresent(dataset -> {
                    if (dataset.getAgentId() != null) {
                        metadata.put("agentId", dataset.getAgentId());
                    }
                });

                // 创建新的 Document 对象（包含更新后的 metadata）
                Document enrichedChunk = new Document(chunk.getText(), metadata);

                // 存储向量（使用 null agentId，因为已在 metadata 中）
                vectorStore.store(null, enrichedChunk.getText(), enrichedChunk.getMetadata());

                // 更新进度
                processedCount++;
                if (processedCount % 10 == 0 || processedCount == chunks.size()) {
                    document.updateProgress(processedCount);
                    documentRepository.save(document);
                    log.info("文档处理进度: documentId={}, progress={}/{}",
                            document.getDocumentId(), processedCount, chunks.size());
                }
            }

            // 6. 标记完成
            document.markCompleted();
            documentRepository.save(document);

            // 7. 更新知识库统计
            datasetRepository.findById(document.getDatasetId()).ifPresent(dataset -> {
                dataset.addChunks(chunks.size());
                datasetRepository.save(dataset);
            });

            log.info("文档处理完成: documentId={}, totalChunks={}",
                    document.getDocumentId(), chunks.size());

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}", document.getDocumentId(), e);

            // 标记失败
            document.markFailed(e.getMessage());
            documentRepository.save(document);
        }
    }

    /**
     * 删除文档的向量数据
     * 
     * @param documentId 文档 ID
     */
    public void deleteDocumentVectors(String documentId) {
        try {
            log.info("删除文档向量: documentId={}", documentId);

            // 使用 Metadata Filter 删除所有属于该文档的向量
            Map<String, Object> filter = new HashMap<>();
            filter.put("documentId", documentId);

            vectorStore.deleteByMetadata(filter);

            log.info("文档向量删除完成: documentId={}", documentId);

        } catch (Exception e) {
            log.error("删除文档向量失败: documentId={}", documentId, e);
            throw new RuntimeException("删除向量失败: " + e.getMessage(), e);
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
