package com.zj.aiagent.interfaces.workflow.dto;

import com.zj.aiagent.domain.workflow.entity.Execution;
import com.zj.aiagent.domain.workflow.valobj.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDTO {
    private String executionId;
    private Long agentId;
    private Long userId;
    private String conversationId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Map<String, String> nodeStatuses;

    public static ExecutionDTO from(Execution execution) {
        if (execution == null) {
            return null;
        }
        return ExecutionDTO.builder()
                .executionId(execution.getExecutionId())
                .agentId(execution.getAgentId())
                .userId(execution.getUserId())
                .conversationId(execution.getConversationId())
                .status(execution.getStatus().name())
                .startTime(execution.getCreatedAt())
                .endTime(execution.getUpdatedAt()) // Assuming updatedAt is endTime for completed execution
                .nodeStatuses(execution.getNodeStatuses().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().name())))
                .build();
    }
}
