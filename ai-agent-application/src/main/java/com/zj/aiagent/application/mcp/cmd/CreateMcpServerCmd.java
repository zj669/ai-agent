package com.zj.aiagent.application.mcp.cmd;

import lombok.Data;

/**
 * 创建 MCP 服务器命令
 */
@Data
public class CreateMcpServerCmd {
    private Long userId;
    private String name;
    private String serverType;
    private String configJson;
    private Boolean enabled;
    private String description;
}
