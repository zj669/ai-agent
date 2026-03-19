package com.zj.aiagent.application.knowledge;

import com.zj.aiagent.domain.knowledge.entity.KnowledgeDocument;
import com.zj.aiagent.domain.knowledge.port.DocumentReaderPort;
import com.zj.aiagent.domain.knowledge.port.FileStorageService;
import com.zj.aiagent.domain.knowledge.port.TextSplitterPort;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDatasetRepository;
import com.zj.aiagent.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.memory.port.VectorStore;
import com.zj.aiagent.domain.memory.valobj.Document;
import java.io.InputStream;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 异步文档处理器
 * 处理流程：MinIO 下载 → 文档解析 → 文本分块 → 批量向量化存储
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncDocumentProcessor {

    private final FileStorageService fileStorageService;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeDatasetRepository datasetRepository;
    private final DocumentReaderPort documentReaderPort;
    private final TextSplitterPort textSplitterPort;
    private final VectorStore vectorStore;

    @Value("${minio.bucket-name:knowledge-files}")
    private String bucketName;

    private static final int BATCH_SIZE = 20;

    @Async
    public void processDocumentAsync(KnowledgeDocument document) {
        log.info(
            "开始异步处理文档: documentId={}, filename={}",
            document.getDocumentId(),
            document.getFilename()
        );

        try {
            // 1. 更新状态为 PROCESSING
            document.startProcessing();
            documentRepository.save(document);

            // 2. 从 MinIO 下载文件
            String objectName = extractObjectName(document.getFileUrl());
            InputStream fileStream = fileStorageService.download(
                bucketName,
                objectName
            );

            // 3. 解析文档为文本
            List<String> parsedTexts = documentReaderPort.readDocument(
                fileStream,
                document.getFilename()
            );
            log.info(
                "文档解析完成: documentId={}, parsedCount={}",
                document.getDocumentId(),
                parsedTexts.size()
            );

            // 4. 文本分块
            ChunkingConfig chunkingConfig =
                document.getChunkingConfig() != null
                    ? document.getChunkingConfig().normalized()
                    : ChunkingConfig.fixedDefault();
            List<String> chunks = textSplitterPort.split(
                parsedTexts,
                chunkingConfig
            );

            document.setTotalChunksCount(chunks.size());
            documentRepository.save(document);
            log.info(
                "文档分块完成: documentId={}, strategy={}, totalChunks={}",
                document.getDocumentId(),
                chunkingConfig.getStrategy(),
                chunks.size()
            );

            // 5. 预查询 dataset 获取 agentId（避免循环内查询）
            Long agentId = datasetRepository
                .findById(document.getDatasetId())
                .map(ds -> ds.getAgentId())
                .orElse(null);

            // 6. 批量向量化存储
            List<Document> batch = new ArrayList<>();
            int processedCount = 0;

            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("document_id", document.getDocumentId());
                metadata.put("dataset_id", document.getDatasetId());
                metadata.put("filename", document.getFilename());
                metadata.put("chunk_index", i);
                metadata.put(
                    "chunk_strategy",
                    chunkingConfig.getStrategy().name()
                );
                if (agentId != null) {
                    metadata.put("agent_id", agentId);
                }

                batch.add(
                    Document.builder()
                        .id(UUID.randomUUID().toString())
                        .content(chunks.get(i))
                        .metadata(metadata)
                        .build()
                );

                // 达到批次大小或最后一批时写入
                if (batch.size() >= BATCH_SIZE || i == chunks.size() - 1) {
                    vectorStore.addDocuments(batch);
                    processedCount += batch.size();
                    batch.clear();

                    document.updateProgress(processedCount);
                    documentRepository.save(document);
                    log.info(
                        "文档处理进度: documentId={}, progress={}/{}",
                        document.getDocumentId(),
                        processedCount,
                        chunks.size()
                    );
                }
            }

            // 7. 标记完成
            document.markCompleted();
            documentRepository.save(document);

            // 8. 更新知识库统计
            datasetRepository
                .findById(document.getDatasetId())
                .ifPresent(dataset -> {
                    dataset.addChunks(chunks.size());
                    datasetRepository.save(dataset);
                });

            log.info(
                "文档处理完成: documentId={}, totalChunks={}",
                document.getDocumentId(),
                chunks.size()
            );
        } catch (Exception e) {
            log.error(
                "文档处理失败: documentId={}",
                document.getDocumentId(),
                e
            );
            document.markFailed(e.getMessage());
            documentRepository.save(document);
        }
    }

    /**
     * 删除文档的向量数据
     */
    public void deleteDocumentVectors(String documentId) {
        try {
            log.info("删除文档向量: documentId={}", documentId);
            Map<String, Object> filter = new HashMap<>();
            filter.put("document_id", documentId);
            vectorStore.deleteByMetadata(filter);
            log.info("文档向量删除完成: documentId={}", documentId);
        } catch (Exception e) {
            log.error("删除文档向量失败: documentId={}", documentId, e);
            throw new RuntimeException("删除向量失败: " + e.getMessage(), e);
        }
    }

    private String extractObjectName(String fileUrl) {
        int bucketIndex = fileUrl.indexOf(bucketName);
        if (bucketIndex == -1) {
            throw new IllegalArgumentException("无效的文件 URL: " + fileUrl);
        }
        return fileUrl.substring(bucketIndex + bucketName.length() + 1);
    }
}
