package com.zj.aiagent.domain.knowledge.valobj;

import java.util.Locale;

/**
 * 文档分块策略
 */
public enum ChunkingStrategy {
    FIXED,
    SEMANTIC;

    public static ChunkingStrategy fromValue(String value) {
        if (value == null || value.isBlank()) {
            return FIXED;
        }

        try {
            return ChunkingStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("不支持的分块策略: " + value, ex);
        }
    }
}
