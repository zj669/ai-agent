package com.zj.aiagent.domain.llm.entity;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 供应商配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmProviderConfig {
    private Long id;
    private Long userId;
    private String name;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
    @Builder.Default
    private Boolean isDefault = false;
    @Builder.Default
    private Integer status = 1;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
