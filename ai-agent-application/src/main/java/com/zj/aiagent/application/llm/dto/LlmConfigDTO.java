package com.zj.aiagent.application.llm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LlmConfigDTO {
    private Long id;
    private String name;
    private String provider;
    private String baseUrl;
    private String model;
    private Boolean isDefault;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
