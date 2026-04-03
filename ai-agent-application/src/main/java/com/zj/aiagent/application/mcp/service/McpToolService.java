package com.zj.aiagent.application.mcp.service;

import com.zj.aiagent.application.mcp.dto.McpToolDTO;
import com.zj.aiagent.domain.mcp.port.IMcpToolRegistry;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP 工具应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpToolService {

    private final IMcpToolRegistry toolRegistry;

    /**
     * 获取指定服务器的工具列表
     */
    public List<McpToolDTO> getToolsByServer(Long serverId) {
        List<McpToolDefinition> tools = toolRegistry.getToolsByServer(serverId);
        return tools.stream()
                .map(McpToolDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有已连接服务器的工具汇总
     */
    public List<McpToolDTO> getAllTools() {
        List<McpToolDefinition> tools = toolRegistry.getAllTools();
        return tools.stream()
                .map(McpToolDTO::from)
                .collect(Collectors.toList());
    }
}
