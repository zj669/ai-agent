package com.zj.aiagent.domain.workflow.entity;

import com.zj.aiagent.domain.workflow.base.WorkflowState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ExecutionResult {
    private String nodeId;
    private String error;


    public static ExecutionResult error(String nodeId, String message){
        return ExecutionResult.builder()
                .nodeId(nodeId)
                .error(message)
                .build();
    }

    public static ExecutionResult success(WorkflowState initialState){
        return ExecutionResult.builder()
                .build();
    }

    public static ExecutionResult pause(String nodeId){
        return ExecutionResult.builder()
                .nodeId(nodeId)
                .build();
    }
}
