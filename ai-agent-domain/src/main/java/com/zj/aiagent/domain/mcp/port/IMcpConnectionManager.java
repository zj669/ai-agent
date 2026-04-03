package com.zj.aiagent.domain.mcp.port;

import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 连接生命周期管理端口
 */
public interface IMcpConnectionManager {

    /**
     * 连接指定服务器并发现工具（懒连接）
     *
     * @param serverId 服务器 ID
     * @return 发现的工具列表
     */
    CompletableFuture<List<McpToolDefinition>> connectAndDiscover(Long serverId);

    /**
     * 检查服务器是否已连接
     */
    boolean isConnected(Long serverId);

    /**
     * 断开指定服务器连接
     */
    void disconnect(Long serverId);

    /**
     * 断开所有服务器连接（应用关闭时调用）
     */
    void disconnectAll();
}
