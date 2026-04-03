package com.zj.aiagent.domain.mcp.entity;

import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * MCP 服务器聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServer {

    private Long id;
    private Long userId;
    private String name;
    private String serverType;
    private McpServerConfig config;
    private Boolean enabled;
    private McpServerStatus status;
    private String description;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 安全检查：验证归属权
     */
    public boolean isOwnedBy(Long requestUserId) {
        return this.userId != null && this.userId.equals(requestUserId);
    }

    /**
     * 是否可连接（未连接且已启用）
     */
    public boolean isConnectable() {
        return Boolean.TRUE.equals(enabled)
                && status == McpServerStatus.DISCONNECTED;
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return status == McpServerStatus.CONNECTED;
    }

    /**
     * 标记为连接中
     */
    public void markConnecting() {
        this.status = McpServerStatus.CONNECTING;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为已连接
     */
    public void markConnected() {
        this.status = McpServerStatus.CONNECTED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为连接异常
     */
    public void markError() {
        this.status = McpServerStatus.ERROR;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为已断开
     */
    public void markDisconnected() {
        this.status = McpServerStatus.DISCONNECTED;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 启用服务器
     */
    public void enable() {
        this.enabled = true;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 禁用服务器
     */
    public void disable() {
        this.enabled = false;
        this.updateTime = LocalDateTime.now();
    }
}
