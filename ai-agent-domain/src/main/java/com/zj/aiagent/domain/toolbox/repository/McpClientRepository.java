package com.zj.aiagent.domain.toolbox.repository;

import io.modelcontextprotocol.client.McpSyncClient;

import java.util.List;
import java.util.Optional;

/**
 * MCP 客户端管理接口
 * <p>
 * 定义 MCP 客户端的获取和管理技术接口，具体实现由基础设施层提供
 * （如 Spring Context、配置文件、远程服务等）
 */
public interface McpClientRepository {

    /**
     * 获取所有 MCP 客户端
     *
     * @param executionId 执行ID
     * @return MCP 客户端列表
     */
    List<McpSyncClient> getAllClients(String executionId);

    /**
     * 查找支持指定工具的客户端
     *
     * @param executionId 执行ID
     * @param toolName    工具名称
     * @return MCP 客户端（如果找到）
     */
    Optional<McpSyncClient> findClientForTool(String executionId, String toolName);

    /**
     * 刷新 MCP 客户端
     *
     * @param executionId 执行ID
     */
    void refresh(String executionId);
}
