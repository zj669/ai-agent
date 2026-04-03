package com.zj.aiagent.domain.mcp.valobj;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * MCP 工具执行结果值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolResult {

    /**
     * 工具执行是否成功
     */
    private boolean success;

    /**
     * 执行结果内容（JSON 字符串）
     */
    private String content;

    /**
     * 错误信息（失败时填充）
     */
    private String errorMessage;

    /**
     * 是否被中止
     */
    private boolean aborted;

    /**
     * 成功结果工厂方法
     */
    public static McpToolResult success(String content) {
        return McpToolResult.builder()
                .success(true)
                .content(content)
                .aborted(false)
                .build();
    }

    /**
     * 失败结果工厂方法
     */
    public static McpToolResult failed(String errorMessage) {
        return McpToolResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .aborted(false)
                .build();
    }

    /**
     * 中止结果工厂方法
     */
    public static McpToolResult aborted() {
        return McpToolResult.builder()
                .success(false)
                .aborted(true)
                .build();
    }
}
