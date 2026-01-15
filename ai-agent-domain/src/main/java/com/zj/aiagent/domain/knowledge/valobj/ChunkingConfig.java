package com.zj.aiagent.domain.knowledge.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档分块配置值对象
 * 定义文档如何被切分为 Chunks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkingConfig {
    /**
     * 分块大小（字符数）
     */
    @Builder.Default
    private Integer chunkSize = 500;

    /**
     * 重叠大小（字符数）
     */
    @Builder.Default
    private Integer chunkOverlap = 50;
}
