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
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChunkingConfig {

    /**
     * 分块策略
     */
    @Builder.Default
    private ChunkingStrategy strategy = ChunkingStrategy.FIXED;

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

    /**
     * 语义分块的最大块大小
     */
    @Builder.Default
    private Integer maxChunkSize = 800;

    /**
     * 语义分块的最小块大小
     */
    @Builder.Default
    private Integer minChunkSize = 200;

    /**
     * 语义相似度阈值，取值范围 0-1
     */
    @Builder.Default
    private Double similarityThreshold = 0.75d;

    /**
     * 是否合并过小片段
     */
    @Builder.Default
    private Boolean mergeSmallChunks = Boolean.TRUE;

    public static ChunkingConfig fixedDefault() {
        return ChunkingConfig.builder()
            .strategy(ChunkingStrategy.FIXED)
            .chunkSize(500)
            .chunkOverlap(50)
            .build();
    }

    public static ChunkingConfig semanticDefault() {
        return ChunkingConfig.builder()
            .strategy(ChunkingStrategy.SEMANTIC)
            .maxChunkSize(800)
            .minChunkSize(200)
            .similarityThreshold(0.75d)
            .mergeSmallChunks(Boolean.TRUE)
            .chunkOverlap(0)
            .build();
    }

    public ChunkingConfig normalized() {
        ChunkingStrategy normalizedStrategy =
            strategy != null ? strategy : ChunkingStrategy.FIXED;
        ChunkingConfig.ChunkingConfigBuilder builder =
            this.toBuilder().strategy(normalizedStrategy);

        if (normalizedStrategy == ChunkingStrategy.SEMANTIC) {
            builder.maxChunkSize(maxChunkSize != null ? maxChunkSize : 800);
            builder.minChunkSize(minChunkSize != null ? minChunkSize : 200);
            builder.similarityThreshold(
                similarityThreshold != null ? similarityThreshold : 0.75d
            );
            builder.mergeSmallChunks(
                mergeSmallChunks != null ? mergeSmallChunks : Boolean.TRUE
            );
            builder.chunkOverlap(chunkOverlap != null ? chunkOverlap : 0);
        } else {
            builder.chunkSize(chunkSize != null ? chunkSize : 500);
            builder.chunkOverlap(chunkOverlap != null ? chunkOverlap : 50);
        }

        return builder.build();
    }

    public void validate() {
        ChunkingConfig normalized = normalized();
        if (normalized.getStrategy() == ChunkingStrategy.SEMANTIC) {
            validatePositive(
                normalized.getMinChunkSize(),
                "最小分块大小必须为正整数"
            );
            validatePositive(
                normalized.getMaxChunkSize(),
                "最大分块大小必须为正整数"
            );
            validateNonNegative(
                normalized.getChunkOverlap(),
                "语义分块重叠大小不能为负数"
            );

            if (normalized.getMaxChunkSize() < normalized.getMinChunkSize()) {
                throw new IllegalArgumentException(
                    "最大分块大小不能小于最小分块大小"
                );
            }

            Double threshold = normalized.getSimilarityThreshold();
            if (threshold == null || threshold <= 0 || threshold > 1) {
                throw new IllegalArgumentException(
                    "语义相似度阈值必须在 (0, 1] 范围内"
                );
            }
        } else {
            validatePositive(normalized.getChunkSize(), "分块大小必须为正整数");
            validateNonNegative(
                normalized.getChunkOverlap(),
                "分块重叠必须大于等于 0"
            );

            if (normalized.getChunkOverlap() >= normalized.getChunkSize()) {
                throw new IllegalArgumentException("分块重叠必须小于分块大小");
            }
        }
    }

    private static void validatePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateNonNegative(Integer value, String message) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
