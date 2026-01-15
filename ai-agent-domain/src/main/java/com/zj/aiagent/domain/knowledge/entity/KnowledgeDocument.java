package com.zj.aiagent.domain.knowledge.entity;

import com.zj.aiagent.domain.knowledge.valobj.ChunkingConfig;
import com.zj.aiagent.domain.knowledge.valobj.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 知识文档聚合根
 * 管理单个文档的解析状态、分块进度、错误处理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {
    /**
     * 文档 ID（聚合根标识）
     */
    private String documentId;

    /**
     * 所属知识库 ID
     */
    private String datasetId;

    /**
     * 文件名
     */
    private String filename;

    /**
     * MinIO 存储路径
     */
    private String fileUrl;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（MIME type）
     * 例如：application/pdf, text/markdown, text/plain
     */
    private String contentType;

    /**
     * 文档处理状态
     */
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    /**
     * 总分块数
     */
    @Builder.Default
    private Integer totalChunks = 0;

    /**
     * 已处理分块数
     */
    @Builder.Default
    private Integer processedChunks = 0;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMessage;

    /**
     * 分块配置
     */
    @Builder.Default
    private ChunkingConfig chunkingConfig = ChunkingConfig.builder().build();

    /**
     * 上传时间
     */
    private Instant uploadedAt;

    /**
     * 完成时间
     */
    private Instant completedAt;

    // ========== 领域行为 ==========

    /**
     * 开始处理文档
     * 状态转换：PENDING → PROCESSING
     */
    public void startProcessing() {
        if (this.status != DocumentStatus.PENDING) {
            throw new IllegalStateException("只能对 PENDING 状态的文档开始处理");
        }
        this.status = DocumentStatus.PROCESSING;
    }

    /**
     * 更新处理进度
     * 
     * @param processedChunks 已处理的分块数
     */
    public void updateProgress(int processedChunks) {
        if (this.status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("只能更新 PROCESSING 状态文档的进度");
        }
        this.processedChunks = processedChunks;
    }

    /**
     * 设置总分块数（解析完成后确定）
     * 
     * @param totalChunks 总分块数
     */
    public void setTotalChunksCount(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    /**
     * 标记文档处理完成
     * 状态转换：PROCESSING → COMPLETED
     */
    public void markCompleted() {
        if (this.status != DocumentStatus.PROCESSING) {
            throw new IllegalStateException("只能标记 PROCESSING 状态的文档为完成");
        }
        this.status = DocumentStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.processedChunks = this.totalChunks; // 确保进度为 100%
    }

    /**
     * 标记文档处理失败
     * 状态转换：任意状态 → FAILED
     * 
     * @param errorMessage 错误信息
     */
    public void markFailed(String errorMessage) {
        this.status = DocumentStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    /**
     * 计算处理进度百分比
     * 
     * @return 0-100 的进度值
     */
    public int getProgressPercentage() {
        if (totalChunks == null || totalChunks == 0) {
            return 0;
        }
        return (int) ((processedChunks * 100.0) / totalChunks);
    }
}
