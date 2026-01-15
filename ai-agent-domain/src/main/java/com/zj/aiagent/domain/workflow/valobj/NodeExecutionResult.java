package com.zj.aiagent.domain.workflow.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

/**
 * 节点执行结果值对象
 * 包含执行状态、输出数据和路由信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionResult {

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 输出数据
     */
    private Map<String, Object> outputs;

    /**
     * 选中的分支ID（仅 Condition 节点使用）
     */
    private String selectedBranchId;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 暂停阶段
     */
    private TriggerPhase triggerPhase;

    // --- 工厂方法 ---

    public static NodeExecutionResult success(Map<String, Object> outputs) {
        return NodeExecutionResult.builder()
                .status(ExecutionStatus.SUCCEEDED)
                .outputs(outputs)
                .build();
    }

    public static NodeExecutionResult failed(String errorMessage) {
        return NodeExecutionResult.builder()
                .status(ExecutionStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    public static NodeExecutionResult routing(String branchId, Map<String, Object> outputs) {
        return NodeExecutionResult.builder()
                .status(ExecutionStatus.SUCCEEDED)
                .selectedBranchId(branchId)
                .outputs(outputs)
                .build();
    }

    public static NodeExecutionResult paused(TriggerPhase phase) {
        return paused(phase, null);
    }

    public static NodeExecutionResult paused(TriggerPhase phase, Map<String, Object> outputs) {
        return NodeExecutionResult.builder()
                .status(ExecutionStatus.PAUSED_FOR_REVIEW)
                .triggerPhase(phase)
                .outputs(outputs)
                .build();
    }

    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCEEDED;
    }

    public boolean isPaused() {
        return status == ExecutionStatus.PAUSED || status == ExecutionStatus.PAUSED_FOR_REVIEW;
    }

    public boolean isRouting() {
        return selectedBranchId != null;
    }
}
