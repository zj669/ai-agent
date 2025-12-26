package com.zj.aiagent.domain.toolbox.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果
 * <p>
 * 封装 MCP 工具的执行结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolExecutionResult {
    
    /**
     * 工具名称
     */
    private String toolName;
    
    /**
     * 执行是否成功
     */
    private boolean success;
    
    /**
     * 执行结果内容
     */
    private String result;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;
    
    /**
     * 执行时间戳
     */
    private Long timestamp;
    
    /**
     * 创建成功结果
     */
    public static ToolExecutionResult success(String toolName, String result, long durationMs) {
        return ToolExecutionResult.builder()
                .toolName(toolName)
                .success(true)
                .result(result)
                .durationMs(durationMs)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static ToolExecutionResult failure(String toolName, String errorMessage) {
        return ToolExecutionResult.builder()
                .toolName(toolName)
                .success(false)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
