package com.zj.aiagent.infrastructure.mcp.transport;

import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;

import java.util.List;
import java.util.Map;

/**
 * MCP 传输策略接口
 * <p>
 * 定义三种传输协议的核心契约：stdio / HTTP / SSE
 */
public interface IMcpTransport {

    /**
     * 发现工具列表
     *
     * @param server MCP 服务器实体
     * @return 工具定义列表
     */
    List<McpToolDefinition> discoverTools(McpServer server);

    /**
     * 执行工具调用
     *
     * @param server    MCP 服务器实体
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @return 执行结果
     */
    McpToolResult executeTool(McpServer server, String toolName, Map<String, Object> arguments);
}
