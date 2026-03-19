package com.zj.aiagent.domain.knowledge.valobj;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ChunkingConfig 测试")
class ChunkingConfigTest {

    @Test
    @DisplayName("默认空策略应归一化为 FIXED")
    void shouldNormalizeToFixedByDefault() {
        ChunkingConfig config = ChunkingConfig.builder().build().normalized();

        assertEquals(ChunkingStrategy.FIXED, config.getStrategy());
        assertEquals(500, config.getChunkSize());
        assertEquals(50, config.getChunkOverlap());
    }

    @Test
    @DisplayName("语义分块默认配置应通过校验")
    void shouldValidateSemanticDefault() {
        assertDoesNotThrow(() -> ChunkingConfig.semanticDefault().validate());
    }

    @Test
    @DisplayName("固定分块 overlap 不能大于等于 chunkSize")
    void shouldRejectInvalidFixedConfig() {
        ChunkingConfig config = ChunkingConfig.builder()
                .strategy(ChunkingStrategy.FIXED)
                .chunkSize(100)
                .chunkOverlap(100)
                .build();

        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    @DisplayName("语义分块 threshold 必须在有效范围内")
    void shouldRejectInvalidSemanticThreshold() {
        ChunkingConfig config = ChunkingConfig.builder()
                .strategy(ChunkingStrategy.SEMANTIC)
                .minChunkSize(100)
                .maxChunkSize(300)
                .similarityThreshold(1.2d)
                .build();

        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
