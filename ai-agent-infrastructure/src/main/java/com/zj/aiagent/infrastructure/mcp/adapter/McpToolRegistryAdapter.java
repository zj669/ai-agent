package com.zj.aiagent.infrastructure.mcp.adapter;

import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.port.IMcpServerRepository;
import com.zj.aiagent.domain.mcp.port.IMcpToolRegistry;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册表实现
 * <p>
 * 维护所有已连接服务器的工具缓存，按 executionId 追踪调用以支持中止
 */
@Slf4j
@Component
public class McpToolRegistryAdapter implements IMcpToolRegistry {

    private final McpConnectionPool connectionPool;
    private final IMcpServerRepository serverRepository;

    /**
     * executionId -> 是否已中止
     */
    private final ConcurrentHashMap<String, Boolean> abortedCalls = new ConcurrentHashMap<>();

    public McpToolRegistryAdapter(McpConnectionPool connectionPool, IMcpServerRepository serverRepository) {
        this.connectionPool = connectionPool;
        this.serverRepository = serverRepository;
    }

    @Override
    public List<McpToolDefinition> getAllTools() {
        return connectionPool.getAllCachedTools();
    }

    @Override
    public List<McpToolDefinition> getToolsByServer(Long serverId) {
        return connectionPool.getCachedTools(serverId);
    }

    @Override
    public CompletableFuture<McpToolResult> execute(
            Long serverId, String toolName, Map<String, Object> args, String executionId) {
        log.info("[McpToolRegistry] execute serverId={} tool={} executionId={}", serverId, toolName, executionId);

        // 检查是否已中止
        if (Boolean.TRUE.equals(abortedCalls.get(executionId))) {
            log.info("[McpToolRegistry] Call already aborted executionId={}", executionId);
            return CompletableFuture.completedFuture(McpToolResult.aborted());
        }

        // 懒连接
        if (!connectionPool.isConnected(serverId)) {
            log.info("[McpToolRegistry] Server not connected, triggering lazy connect serverId={}", serverId);
            return connectionPool.connectAndDiscover(serverId)
                    .thenCompose(tools -> {
                        if (Boolean.TRUE.equals(abortedCalls.get(executionId))) {
                            return CompletableFuture.completedFuture(McpToolResult.aborted());
                        }
                        return connectionPool.executeTool(serverId, toolName, args, executionId);
                    });
        } else {
            return connectionPool.executeTool(serverId, toolName, args, executionId);
        }
    }

    @Override
    public void abort(String executionId) {
        log.info("[McpToolRegistry] Abort executionId={}", executionId);
        abortedCalls.put(executionId, true);
    }

    @Override
    public CompletableFuture<Void> refreshServer(Long serverId) {
        log.info("[McpToolRegistry] Refresh server serverId={}", serverId);
        connectionPool.disconnect(serverId);
        return connectionPool.connectAndDiscover(serverId)
                .thenApply(tools -> null);
    }

    @Override
    public List<McpToolDefinition> getToolsByUserId(Long userId) {
        return connectionPool.getCachedToolsByUserId(userId);
    }

    @Override
    public void connectAllUserServers(Long userId) {
        if (userId == null) {
            log.info("[McpToolRegistry] connectAllUserServers: userId is null, skip");
            return;
        }
        List<McpServer> servers = serverRepository.findByUserId(userId);
        if (servers.isEmpty()) {
            log.info("[McpToolRegistry] connectAllUserServers: no servers found for userId={}", userId);
            return;
        }
        servers.stream()
            .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
            .filter(s -> !connectionPool.isConnected(s.getId()))
            .forEach(s -> {
                log.info("[McpToolRegistry] connectAllUserServers: connecting serverId={}, name={}", s.getId(), s.getName());
                connectionPool.connectAndDiscover(s.getId());
            });
    }
}
