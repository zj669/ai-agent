package com.zj.aiagent.domain.mcp.port;

import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 工具注册表端口
 * Swarm Agent 和 Workflow Engine 均通过此接口获取 MCP 工具
 */
public interface IMcpToolRegistry {

    /**
     * 获取所有已连接服务器的工具
     */
    List<McpToolDefinition> getAllTools();

    /**
     * 获取指定服务器的工具
     */
    List<McpToolDefinition> getToolsByServer(Long serverId);

    /**
     * 执行 MCP 工具
     *
     * @param serverId     服务器 ID
     * @param toolName     工具名称（不含前缀）
     * @param args         工具参数
     * @param executionId  执行 ID（用于中止追踪）
     * @return 工具执行结果
     */
    CompletableFuture<McpToolResult> execute(Long serverId, String toolName,
                                             java.util.Map<String, Object> args, String executionId);

    /**
     * 中止指定 executionId 的所有调用
     */
    void abort(String executionId);

    /**
     * 刷新指定服务器的工具列表
     */
    CompletableFuture<Void> refreshServer(Long serverId);

    /**
     * 获取指定用户的所有已连接 MCP 服务器工具（用于 workspace 隔离）。
     *
     * @param userId 用户 ID
     * @return 该用户的所有 MCP 工具列表
     */
    List<McpToolDefinition> getToolsByUserId(Long userId);
}
