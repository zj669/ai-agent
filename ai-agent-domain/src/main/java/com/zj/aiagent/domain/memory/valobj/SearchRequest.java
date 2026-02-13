package com.zj.aiagent.domain.memory.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 领域层搜索请求值对象
 *
 * 用于向量存储的搜索请求，替代 Spring AI 的 SearchRequest
 * 保持 domain 层纯净，不依赖框架类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    /**
     * 查询文本
     */
    private String query;

    /**
     * 返回结果数量
     */
    @Builder.Default
    private int topK = 5;

    /**
     * 过滤表达式（如 "agent_id == 123"）
     */
    private String filterExpression;

    /**
     * 相似度阈值（可选，0.0-1.0）
     */
    private Double similarityThreshold;

    /**
     * 便捷构造方法：仅查询文本
     */
    public SearchRequest(String query) {
        this.query = query;
        this.topK = 5;
    }

    /**
     * 便捷构造方法：查询文本 + topK
     */
    public SearchRequest(String query, int topK) {
        this.query = query;
        this.topK = topK;
    }
}
