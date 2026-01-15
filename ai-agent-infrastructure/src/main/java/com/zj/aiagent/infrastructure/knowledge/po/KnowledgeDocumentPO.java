package com.zj.aiagent.infrastructure.knowledge.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识文档 PO (Persistent Object)
 * 对应数据库表: knowledge_document
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocumentPO {

    @TableId(type = IdType.INPUT)
    private String documentId;

    private String datasetId;
    private String filename;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED
    private Integer totalChunks;
    private Integer processedChunks;
    private String errorMessage;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private LocalDateTime uploadedAt;
    private LocalDateTime completedAt;
}
