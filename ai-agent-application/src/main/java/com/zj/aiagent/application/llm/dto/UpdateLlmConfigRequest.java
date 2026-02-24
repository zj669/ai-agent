package com.zj.aiagent.application.llm.dto;

import lombok.Data;

@Data
public class UpdateLlmConfigRequest {
    private String name;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Boolean isDefault;
}
