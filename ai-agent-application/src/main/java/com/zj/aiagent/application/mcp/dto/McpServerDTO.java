package com.zj.aiagent.application.mcp.dto;

import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP 服务器 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerDTO {

    private Long id;
    private Long userId;
    private String name;
    private String serverType;
    private McpServerConfig config;
    private Boolean enabled;
    private McpServerStatus status;
    private String statusDesc;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static McpServerDTO from(McpServer server) {
        return McpServerDTO.builder()
                .id(server.getId())
                .userId(server.getUserId())
                .name(server.getName())
                .serverType(server.getServerType())
                .config(server.getConfig())
                .enabled(server.getEnabled())
                .status(server.getStatus())
                .statusDesc(server.getStatus() != null ? server.getStatus().getDesc() : "")
                .description(server.getDescription())
                .createTime(server.getCreateTime())
                .updateTime(server.getUpdateTime())
                .build();
    }
}
