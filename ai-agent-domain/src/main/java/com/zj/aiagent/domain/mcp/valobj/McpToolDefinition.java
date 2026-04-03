package com.zj.aiagent.domain.mcp.valobj;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MCP 工具定义值对象
 * 描述单个 MCP 工具的元信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDefinition {

    /**
     * 所属服务器 ID
     */
    private Long serverId;

    /**
     * 所属服务器名称
     */
    private String serverName;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 完整工具名称，格式: mcp__{serverId}__{toolName}
     */
    private String fullName;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具输入 Schema（JSON 字符串）
     */
    private String inputSchema;

    /**
     * 生成完整名称
     */
    public static String makeFullName(Long serverId, String toolName) {
        return "mcp__" + serverId + "__" + toolName;
    }

    /**
     * 从完整名称解析 serverId
     */
    public static Long parseServerId(String fullName) {
        if (fullName == null || !fullName.startsWith("mcp__")) {
            return null;
        }
        String[] parts = fullName.split("__");
        if (parts.length < 2) {
            return null;
        }
        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从完整名称解析 toolName
     */
    public static String parseToolName(String fullName) {
        if (fullName == null || !fullName.startsWith("mcp__")) {
            return null;
        }
        String[] parts = fullName.split("__");
        if (parts.length < 3) {
            return null;
        }
        return parts[2];
    }
}
