package com.zj.aiagent.application.llm.dto;

import lombok.Data;

@Data
public class CreateLlmConfigRequest {
    private String name;
    private String provider;
    private String baseUrl;
    private String apiKey;
    private String model;
}
