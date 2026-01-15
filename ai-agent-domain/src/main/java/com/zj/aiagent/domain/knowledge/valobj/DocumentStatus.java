package com.zj.aiagent.domain.knowledge.valobj;

/**
 * 文档状态枚举
 * 用于跟踪文档的处理进度
 */
public enum DocumentStatus {
    /**
     * 已上传，等待处理
     */
    PENDING,

    /**
     * 正在解析向量化
     */
    PROCESSING,

    /**
     * 完成
     */
    COMPLETED,

    /**
     * 失败
     */
    FAILED
}
