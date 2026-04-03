package com.zj.aiagent.infrastructure.mcp.adapter;

import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.port.IMcpServerRepository;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import com.zj.aiagent.infrastructure.mcp.transport.IMcpTransport;
import com.zj.aiagent.infrastructure.mcp.transport.McpTransportFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;

/**
 * MCP 连接池 - 管理所有 MCP 服务器的生命周期
 * <p>
 * 懒连接策略：仅在首次调用工具时才连接服务器
 * 并发安全：使用 ConcurrentHashMap + pendingConnections 防止重复连接
 * <p>
 * 传输协议通过 {@link McpTransportFactory} 委托给对应的 {@link IMcpTransport} 实现，
 * 自身不再包含 if-else 判断逻辑
 */
@Slf4j
@Component
public class McpConnectionPool {

    private final IMcpServerRepository repository;
    private final McpTransportFactory transportFactory;

    /**
     * serverId -> 连接状态
     */
    private final ConcurrentHashMap<Long, ServerConnectionState> pool = new ConcurrentHashMap<>();

    /**
     * serverId -> 连接 Promise（防止并发重复连接）
     */
    private final ConcurrentHashMap<Long, CompletableFuture<List<McpToolDefinition>>> pendingConnections = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "mcp-connector");
        t.setDaemon(true);
        return t;
    });

    public McpConnectionPool(IMcpServerRepository repository, McpTransportFactory transportFactory) {
        this.repository = repository;
        this.transportFactory = transportFactory;
    }

    /**
     * 懒连接 + 发现工具
     */
    public CompletableFuture<List<McpToolDefinition>> connectAndDiscover(Long serverId) {
        return pendingConnections.computeIfAbsent(serverId, id -> {
            log.info("[McpConnectionPool] Starting lazy connect for serverId={}", serverId);
            return doConnect(serverId)
                    .whenComplete((r, e) -> {
                        pendingConnections.remove(serverId);
                        if (e != null) {
                            log.error("[McpConnectionPool] Connect failed for serverId={}", serverId, e);
                        }
                    });
        });
    }

    private CompletableFuture<List<McpToolDefinition>> doConnect(Long serverId) {
        return CompletableFuture.supplyAsync(() -> {
            Optional<McpServer> optServer = repository.findById(serverId);
            if (optServer.isEmpty()) {
                throw new IllegalArgumentException("MCP Server not found: " + serverId);
            }

            McpServer server = optServer.get();
            server.markConnecting();
            repository.save(server);
            pool.put(serverId, new ServerConnectionState(server.getName(), McpServerStatus.CONNECTING));

            try {
                List<McpToolDefinition> tools = discoverTools(server);
                server.markConnected();
                repository.save(server);
                ServerConnectionState state = pool.get(serverId);
                if (state != null) {
                    state.setStatus(McpServerStatus.CONNECTED);
                    state.setTools(tools);
                }
                log.info("[McpConnectionPool] Connected serverId={}, discovered {} tools", serverId, tools.size());
                return tools;
            } catch (Exception e) {
                server.markError();
                repository.save(server);
                ServerConnectionState state = pool.get(serverId);
                if (state != null) {
                    state.setStatus(McpServerStatus.ERROR);
                }
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * 发现工具 - 委托给传输策略
     */
    private List<McpToolDefinition> discoverTools(McpServer server) {
        McpServerConfig config = server.getConfig();
        if (config == null) {
            log.warn("[McpConnectionPool] No config for serverId={}", server.getId());
            return Collections.emptyList();
        }

        try {
            IMcpTransport transport = transportFactory.getTransport(config);
            return transport.discoverTools(server);
        } catch (Exception e) {
            log.error("[McpConnectionPool] Failed to discover tools for serverId={}", server.getId(), e);
            return Collections.emptyList();
        }
    }

    public boolean isConnected(Long serverId) {
        ServerConnectionState state = pool.get(serverId);
        return state != null && state.getStatus() == McpServerStatus.CONNECTED;
    }

    public void disconnect(Long serverId) {
        ServerConnectionState state = pool.remove(serverId);
        if (state != null) {
            state.getProcess().ifPresent(p -> {
                p.destroyForcibly();
                log.info("[McpConnectionPool] Destroyed stdio process for serverId={}", serverId);
            });
        }

        try {
            repository.findById(serverId).ifPresent(server -> {
                server.markDisconnected();
                repository.save(server);
                log.info("[McpConnectionPool] Marked DISCONNECTED in DB for serverId={}", serverId);
            });
        } catch (Exception e) {
            // 即使 DB 更新失败，也强制从池中移除（池已在前一步清理）
            log.warn("[McpConnectionPool] Failed to update DB status for serverId={}, pool entry removed: {}",
                    serverId, e.getMessage());
        }

        log.info("[McpConnectionPool] Disconnected serverId={}", serverId);
    }

    @PreDestroy
    public void disconnectAll() {
        log.info("[McpConnectionPool] Shutting down all MCP connections, count={}", pool.size());
        new ArrayList<>(pool.keySet()).forEach(this::disconnect);
    }

    /**
     * 获取指定服务器缓存的工具列表
     */
    public List<McpToolDefinition> getCachedTools(Long serverId) {
        ServerConnectionState state = pool.get(serverId);
        if (state == null || state.getTools() == null) {
            return Collections.emptyList();
        }
        return state.getTools();
    }

    /**
     * 获取所有已连接服务器缓存的工具列表
     */
    public List<McpToolDefinition> getAllCachedTools() {
        List<McpToolDefinition> allTools = new ArrayList<>();
        pool.values().forEach(state -> {
            if (state.getTools() != null) {
                allTools.addAll(state.getTools());
            }
        });
        return allTools;
    }

    /**
     * 获取指定用户的所有已连接 MCP 服务器缓存的工具列表（workspace 隔离）。
     *
     * <p>通过 userId 找到该用户的所有 MCP 服务器 ID，仅返回这些服务器的工具。
     *
     * @param userId 用户 ID
     * @return 该用户的所有 MCP 工具列表
     */
    public List<McpToolDefinition> getCachedToolsByUserId(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 获取该用户的所有 MCP 服务器 ID
        List<Long> userServerIds = repository.findByUserId(userId)
                .stream()
                .map(com.zj.aiagent.domain.mcp.entity.McpServer::getId)
                .toList();

        if (userServerIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<McpToolDefinition> result = new ArrayList<>();
        for (Long serverId : userServerIds) {
            ServerConnectionState state = pool.get(serverId);
            if (state != null && state.getTools() != null) {
                result.addAll(state.getTools());
            }
        }
        return result;
    }

    /**
     * 执行 MCP 工具调用（同步版本，内部使用 CompletableFuture）
     */
    public CompletableFuture<McpToolResult> executeTool(
            Long serverId, String toolName, Map<String, Object> args, String executionId) {
        return CompletableFuture.supplyAsync(() -> {
            ServerConnectionState state = pool.get(serverId);
            if (state == null || state.getStatus() != McpServerStatus.CONNECTED) {
                return McpToolResult.failed("Server not connected: " + serverId);
            }

            McpServer server = repository.findById(serverId).orElse(null);
            if (server == null) {
                return McpToolResult.failed("Server not found: " + serverId);
            }

            McpServerConfig config = server.getConfig();
            if (config == null) {
                return McpToolResult.failed("No config for server: " + serverId);
            }

            try {
                IMcpTransport transport = transportFactory.getTransport(config);
                return transport.executeTool(server, toolName, args);
            } catch (Exception e) {
                log.error("[McpConnectionPool] Tool execution failed serverId={} tool={}", serverId, toolName, e);
                return McpToolResult.failed(e.getMessage());
            }
        }, executor);
    }

    /**
     * 服务器连接状态内部类
     */
    public static class ServerConnectionState {
        private final String serverName;
        private volatile McpServerStatus status;
        private volatile List<McpToolDefinition> tools;
        private volatile Process stdioProcess;

        public ServerConnectionState(String serverName, McpServerStatus status) {
            this.serverName = serverName;
            this.status = status;
        }

        public String getServerName() { return serverName; }
        public McpServerStatus getStatus() { return status; }
        public void setStatus(McpServerStatus status) { this.status = status; }
        public List<McpToolDefinition> getTools() { return tools; }
        public void setTools(List<McpToolDefinition> tools) { this.tools = tools; }
        public Optional<Process> getProcess() { return Optional.ofNullable(stdioProcess); }
        public void setProcess(Process process) { this.stdioProcess = process; }
    }
}
