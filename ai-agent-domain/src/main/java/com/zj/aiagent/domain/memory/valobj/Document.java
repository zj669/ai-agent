package com.zj.aiagent.domain.memory.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 领域层文档值对象
 *
 * 用于向量存储的文档表示，替代 Spring AI 的 Document
 * 保持 domain 层纯净，不依赖框架类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    /**
     * 文档唯一标识
     */
    private String id;

    /**
     * 文档内容
     */
    private String content;

    /**
     * 元数据（如 agent_id, dataset_id, timestamp）
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 向量嵌入（可选，由 infrastructure 层生成）
     */
    private List<Double> embedding;

    /**
     * 便捷构造方法：仅内容
     */
    public Document(String content) {
        this.content = content;
        this.metadata = new HashMap<>();
    }

    /**
     * 便捷构造方法：内容 + 元数据
     */
    public Document(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    /**
     * 获取文本内容（兼容方法）
     */
    public String getText() {
        return content;
    }
}
