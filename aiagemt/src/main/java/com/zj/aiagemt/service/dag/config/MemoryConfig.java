package com.zj.aiagemt.service.dag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 记忆配置类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryConfig {

    /**
     * 是否启用记忆
     */
    private Boolean enabled;

    /**
     * 记忆类型
     */
    private MemoryType type;

    /**
     * 检索大小
     */
    private Integer retrieveSize;

    /**
     * 会话ID (支持占位符${conversationId})
     */
    private String conversationId;

    /**
     * 记忆类型枚举
     */
    public enum MemoryType {
        /**
         * 向量存储记忆
         */
        VECTOR_STORE,

        /**
         * 对话记忆
         */
        CHAT_MEMORY,

        /**
         * 摘要记忆
         */
        SUMMARY_MEMORY,

        /**
         * 缓冲记忆
         */
        BUFFER_MEMORY
    }
}
