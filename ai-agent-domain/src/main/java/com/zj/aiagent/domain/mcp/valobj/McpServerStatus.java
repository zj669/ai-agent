package com.zj.aiagent.domain.mcp.valobj;

import lombok.Getter;

/**
 * MCP 服务器连接状态枚举
 */
@Getter
public enum McpServerStatus {
    DISCONNECTED("未连接"),
    CONNECTING("连接中"),
    CONNECTED("已连接"),
    ERROR("连接异常");

    private final String desc;

    McpServerStatus(String desc) {
        this.desc = desc;
    }
}
