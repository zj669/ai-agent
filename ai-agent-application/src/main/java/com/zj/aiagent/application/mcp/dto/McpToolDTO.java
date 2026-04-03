package com.zj.aiagent.application.mcp.dto;

import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * MCP 工具 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDTO {

    private Long serverId;
    private String serverName;
    private String toolName;
    private String fullName;
    private String description;
    private String inputSchema;

    public static McpToolDTO from(McpToolDefinition def) {
        return McpToolDTO.builder()
                .serverId(def.getServerId())
                .serverName(def.getServerName())
                .toolName(def.getToolName())
                .fullName(def.getFullName())
                .description(def.getDescription())
                .inputSchema(def.getInputSchema())
                .build();
    }
}
