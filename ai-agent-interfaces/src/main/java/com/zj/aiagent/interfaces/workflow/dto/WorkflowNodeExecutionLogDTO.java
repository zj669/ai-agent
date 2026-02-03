package com.zj.aiagent.interfaces.workflow.dto;

import com.zj.aiagent.domain.workflow.entity.WorkflowNodeExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流节点执行日志 DTO
 * 用于返回节点执行的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNodeExecutionLogDTO {
    
    private Long id;
    private String executionId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    
    /**
     * 渲染模式: HIDDEN, THOUGHT, MESSAGE
     */
    private String renderMode;
    
    /**
     * 执行状态: 0:Running, 1:Success, 2:Failed
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusText;
    
    /**
     * 完整输入 (JSON)
     */
    private Map<String, Object> inputs;
    
    /**
     * 完整输出 (JSON)
     */
    private Map<String, Object> outputs;
    
    private String errorMessage;
    
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 从领域实体转换为 DTO
     */
    public static WorkflowNodeExecutionLogDTO from(WorkflowNodeExecutionLog log) {
        if (log == null) {
            return null;
        }
        
        // 计算执行耗时
        Long durationMs = null;
        if (log.getStartTime() != null && log.getEndTime() != null) {
            durationMs = java.time.Duration.between(log.getStartTime(), log.getEndTime()).toMillis();
        }
        
        // 状态文本映射
        String statusText = getStatusText(log.getStatus());
        
        return WorkflowNodeExecutionLogDTO.builder()
                .id(log.getId())
                .executionId(log.getExecutionId())
                .nodeId(log.getNodeId())
                .nodeName(log.getNodeName())
                .nodeType(log.getNodeType())
                .renderMode(log.getRenderMode())
                .status(log.getStatus())
                .statusText(statusText)
                .inputs(log.getInputs())
                .outputs(log.getOutputs())
                .errorMessage(log.getErrorMessage())
                .startTime(log.getStartTime())
                .endTime(log.getEndTime())
                .durationMs(durationMs)
                .build();
    }
    
    /**
     * 获取状态文本描述
     */
    private static String getStatusText(Integer status) {
        if (status == null) {
            return "Unknown";
        }
        return switch (status) {
            case 0 -> "Running";
            case 1 -> "Success";
            case 2 -> "Failed";
            default -> "Unknown";
        };
    }
}
