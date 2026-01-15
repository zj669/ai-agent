package com.zj.aiagent.interfaces.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 知识库管理 API 数据传输对象
 */
public class KnowledgeDTO {

    // ========== 知识库相关 ==========

    /**
     * 创建知识库请求
     */
    @Data
    public static class DatasetCreateReq {
        @NotBlank(message = "知识库名称不能为空")
        private String name;

        private String description;

        /** 可选：绑定的 Agent ID */
        private Long agentId;
    }

    /**
     * 知识库响应
     */
    @Data
    public static class DatasetResp {
        private String datasetId;
        private String name;
        private String description;
        private Long userId;
        private Long agentId;
        private Integer documentCount;
        private Integer totalChunks;
        private String createdAt;
        private String updatedAt;
    }

    // ========== 文档相关 ==========

    /**
     * 文档上传请求（分块配置）
     * 注意：文件通过 MultipartFile 传递，此对象仅包含配置参数
     */
    @Data
    public static class DocumentUploadReq {
        @NotBlank(message = "知识库 ID 不能为空")
        private String datasetId;

        /** 分块大小（默认 500 tokens） */
        @Positive(message = "分块大小必须为正整数")
        private Integer chunkSize = 500;

        /** 分块重叠（默认 50 tokens） */
        @Positive(message = "分块重叠必须为正整数")
        private Integer chunkOverlap = 50;
    }

    /**
     * 文档列表查询请求
     */
    @Data
    public static class DocumentListReq {
        @NotBlank(message = "知识库 ID 不能为空")
        private String datasetId;

        private Integer page = 0;
        private Integer size = 20;
    }

    /**
     * 文档响应
     */
    @Data
    public static class DocumentResp {
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
        private String uploadedAt;
        private String completedAt;
    }

    // ========== 检索相关 ==========

    /**
     * 知识检索请求
     */
    @Data
    public static class SearchReq {
        @NotBlank(message = "知识库 ID 不能为空")
        private String datasetId;

        @NotBlank(message = "查询内容不能为空")
        private String query;

        @Positive(message = "topK 必须为正整数")
        private Integer topK = 5;
    }

    /**
     * 知识检索响应
     */
    @Data
    public static class SearchResp {
        private String content;
        private Double score;
        private String documentId;
        private String filename;
        private Integer chunkIndex;
    }
}
