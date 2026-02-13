package com.zj.aiagent.interfaces.workflow.dto;

import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

public class HumanReviewDTO {

    @Data
    public static class PendingReviewDTO {
        private String executionId;
        private String nodeId;
        private String nodeName;
        private String agentName;
        private TriggerPhase triggerPhase;
        private LocalDateTime pausedAt;
        private Long userId;
    }

    @Data
    public static class ReviewDetailDTO {
        private String executionId;
        private String nodeId;
        private String nodeName;
        private TriggerPhase triggerPhase;
        private Map<String, Object> contextData;
        private HumanReviewConfigDTO config;
    }

    @Data
    public static class HumanReviewConfigDTO {
        private String prompt;
        private String[] editableFields;
    }

    @Data
    public static class ResumeExecutionRequest {
        private String executionId;
        private String nodeId;
        private Map<String, Object> edits;
        private String comment;
    }

    @Data
    public static class RejectExecutionRequest {
        private String executionId;
        private String nodeId;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumeExecutionResponse {
        private boolean success;
        private String message;
    }
}
