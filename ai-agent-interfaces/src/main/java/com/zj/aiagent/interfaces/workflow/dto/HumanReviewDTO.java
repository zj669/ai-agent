package com.zj.aiagent.interfaces.workflow.dto;

import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

public class HumanReviewDTO {

    @Data
    public static class PendingReviewDTO {
        private String executionId;
        private String nodeId;
        private String nodeName;
        private String agentName; // Optional, might need extra query
        private TriggerPhase triggerPhase;
        private LocalDateTime pausedAt;
    }

    @Data
    public static class ReviewDetailDTO {
        private String executionId;
        private String nodeId;
        private String nodeName;
        private TriggerPhase triggerPhase;
        private Map<String, Object> contextData; // Inputs or Outputs depending on phase
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
        private String nodeId; // Verification
        private Map<String, Object> edits;
        private String comment;
        // version for optimistic lock? SchedulerService checks strict logic, maybe
        // controller should pass version if we want strict API control.
        // But SchedulerService uses Redisson Lock, not passing version from FE
        // explicitly in strict sense,
        // though Execution entity has version.
        // Ideally we pass expected version to ensure no concurrent resume.
        // Let's keep it simple for now, SchedulerService handles concurrency with lock.
    }
}
