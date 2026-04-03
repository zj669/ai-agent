package com.zj.aiagent.domain.mcp.port;

import com.zj.aiagent.domain.mcp.entity.McpServer;

import java.util.List;
import java.util.Optional;

/**
 * MCP 服务器仓储端口接口
 */
public interface IMcpServerRepository {

    /**
     * 保存 MCP 服务器（含新增和更新）
     */
    void save(McpServer server);

    /**
     * 根据 ID 查询
     */
    Optional<McpServer> findById(Long id);

    /**
     * 根据用户 ID 查询所有未删除的服务器
     */
    List<McpServer> findByUserId(Long userId);

    /**
     * 根据 ID 逻辑删除
     */
    void deleteById(Long id);
}
