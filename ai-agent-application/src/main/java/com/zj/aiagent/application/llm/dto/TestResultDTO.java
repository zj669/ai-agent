package com.zj.aiagent.application.llm.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TestResultDTO {
    private boolean ok;
    private Long latencyMs;
    private String error;
}
