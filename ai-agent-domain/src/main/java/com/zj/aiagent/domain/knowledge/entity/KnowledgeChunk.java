package com.zj.aiagent.domain.knowledge.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识片段实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunk {

    /**
     * 知识ID
     */
    private String id;

    /**
     * 知识内容
     */
    private String content;

    /**
     * 相似度评分
     */
    private Double score;

    /**
     * 来源
     */
    private String source;

    /**
     * 元数据
     */
    private java.util.Map<String, Object> metadata;

    /**
     * 向量嵌入(可选)
     */
    private float[] embedding;
}
