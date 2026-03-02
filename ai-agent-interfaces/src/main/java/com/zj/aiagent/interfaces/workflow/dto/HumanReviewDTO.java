package com.zj.aiagent.interfaces.workflow.dto;

import com.zj.aiagent.domain.workflow.valobj.TriggerPhase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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
        /** 所有节点的输入输出（包含上游节点和当前节点） */
        private List<NodeContextDTO> nodes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeContextDTO {
        private String nodeId;
        private String nodeName;
        private String nodeType;
        private String status;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
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
