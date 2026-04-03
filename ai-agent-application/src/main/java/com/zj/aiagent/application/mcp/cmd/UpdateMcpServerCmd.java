package com.zj.aiagent.application.mcp.cmd;

import lombok.Data;

/**
 * 更新 MCP 服务器命令
 */
@Data
public class UpdateMcpServerCmd {
    private Long id;
    private Long userId;
    private String name;
    private String serverType;
    private String configJson;
    private Boolean enabled;
    private String description;
}
