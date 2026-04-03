package com.zj.aiagent.infrastructure.mcp.adapter;

import com.zj.aiagent.domain.mcp.port.IMcpConnectionManager;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Spring 管理的 MCP 连接生命周期管理器
 * <p>
 * 包装 {@link McpConnectionPool}，暴露为 {@link IMcpConnectionManager} 端口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpConnectionManager implements IMcpConnectionManager {

    private final McpConnectionPool pool;

    @Override
    public CompletableFuture<List<McpToolDefinition>> connectAndDiscover(Long serverId) {
        return pool.connectAndDiscover(serverId);
    }

    @Override
    public boolean isConnected(Long serverId) {
        return pool.isConnected(serverId);
    }

    @Override
    public void disconnect(Long serverId) {
        pool.disconnect(serverId);
    }

    @Override
    public void disconnectAll() {
        log.info("[McpConnectionManager] Delegating disconnectAll to pool");
        pool.disconnectAll();
    }
}
