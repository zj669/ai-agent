package com.zj.aiagent.domain.agent.dag.context;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 执行阶段数据
 * 封装规划和执行相关的所有数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionData {

    /** 规划结果 */
    private String planResult;

    /** 当前执行结果 */
    private String executionResult;

    /** 上次执行结果 */
    private String previousExecutionResult;

    /** 执行历史 */
    private StringBuilder executionHistory;

    /**
     * 追加执行历史
     */
    public void appendHistory(String result) {
        if (executionHistory == null) {
            executionHistory = new StringBuilder();
        }
        executionHistory.append("\n[执行结果]: ").append(result);
    }

    /**
     * 获取执行历史字符串
     */
    public String getHistoryAsString() {
        return executionHistory != null ? executionHistory.toString() : "";
    }

    /**
     * 更新执行结果并记录历史
     */
    public void updateExecutionResult(String result) {
        this.previousExecutionResult = this.executionResult;
        this.executionResult = result;
        appendHistory(result);
    }
}
