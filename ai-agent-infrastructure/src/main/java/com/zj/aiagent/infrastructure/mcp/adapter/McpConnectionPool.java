package com.zj.aiagent.infrastructure.mcp.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.port.IMcpServerRepository;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpServerStatus;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * MCP 连接池 - 管理所有 MCP 服务器的生命周期
 * <p>
 * 懒连接策略：仅在首次调用工具时才连接服务器
 * 并发安全：使用 ConcurrentHashMap + pendingConnections 防止重复连接
 */
@Slf4j
@Component
public class McpConnectionPool {

    private final IMcpServerRepository repository;
    private final ObjectMapper objectMapper;

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

    public McpConnectionPool(IMcpServerRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
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
     * 发现工具 - 根据服务器类型选择传输协议
     */
    private List<McpToolDefinition> discoverTools(McpServer server) {
        McpServerConfig config = server.getConfig();
        if (config == null) {
            log.warn("[McpConnectionPool] No config for serverId={}", server.getId());
            return Collections.emptyList();
        }

        try {
            if (config.isStdio()) {
                return discoverViaStdio(server);
            } else if (config.isSse() || config.isHttp()) {
                return discoverViaHttp(server);
            } else {
                log.warn("[McpConnectionPool] Unknown server type {} for serverId={}",
                        config.getType(), server.getId());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("[McpConnectionPool] Failed to discover tools for serverId={}", server.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 通过 stdio 发现工具
     */
    private List<McpToolDefinition> discoverViaStdio(McpServer server) {
        McpServerConfig config = server.getConfig();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmd = new ArrayList<>();
            cmd.add(config.getCommand());
            if (config.getArgs() != null) {
                cmd.addAll(config.getArgs());
            }
            pb.command(cmd);

            // 设置环境变量
            if (config.getEnv() != null) {
                Map<String, String> env = pb.environment();
                env.putAll(config.getEnv());
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 发送 MCP JSON-RPC initialize + tools/list
            String initializeRequest = buildJsonrpcRequest("initialize", UUID.randomUUID().toString(), Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of("tools", Map.of()),
                    "clientInfo", Map.of("name", "ai-agent", "version", "1.0.0")
            ));
            String initializedRequest = buildJsonrpcRequest("notifications/initialized", UUID.randomUUID().toString(), Map.of());
            String listToolsRequest = buildJsonrpcRequest("tools/list", UUID.randomUUID().toString(), Map.of());

            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
                writer.write(initializeRequest + "\n");
                writer.write(initializedRequest + "\n");
                writer.write(listToolsRequest + "\n");
                writer.flush();
            }

            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count++ < 50) {
                    response.append(line);
                }
            }

            // 等待进程退出，最多 30 秒
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }

            return parseToolsFromResponse(response.toString(), server.getId(), server.getName());
        } catch (Exception e) {
            log.error("[McpConnectionPool] stdio discover failed for serverId={}", server.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 通过 HTTP 发现工具
     */
    private List<McpToolDefinition> discoverViaHttp(McpServer server) {
        McpServerConfig config = server.getConfig();
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            String url = config.getUrl();
            if (url == null || url.isBlank()) {
                log.warn("[McpConnectionPool] No URL for http serverId={}", server.getId());
                return Collections.emptyList();
            }
            if (config.getEndpoint() != null && !config.getEndpoint().isBlank()) {
                if (url.endsWith("/") && config.getEndpoint().startsWith("/")) {
                    url = url + config.getEndpoint().substring(1);
                } else if (!url.endsWith("/") && !config.getEndpoint().startsWith("/")) {
                    url = url + "/" + config.getEndpoint();
                } else {
                    url = url + config.getEndpoint();
                }
            }

            String requestBody = buildJsonrpcRequest("tools/list", UUID.randomUUID().toString(), Map.of());

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (config.getHeaders() != null) {
                config.getHeaders().forEach(reqBuilder::header);
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("[McpConnectionPool] HTTP {} for serverId={}: {}", resp.statusCode(), server.getId(), resp.body());
                return Collections.emptyList();
            }

            String responseStr = resp.body();
            if (responseStr != null && responseStr.contains("data: {\"jsonrpc\"")) {
                int idx = responseStr.indexOf("data: {");
                responseStr = responseStr.substring(idx + 6).trim();
            }

            return parseToolsFromResponse(responseStr, server.getId(), server.getName());
        } catch (Exception e) {
            log.error("[McpConnectionPool] HTTP discover failed for serverId={}", server.getId(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建 JSON-RPC 请求（内部格式：params 中的 name + arguments 直接拼接）
     * 适用于 tools/list（空 params）和 tools/call（name + arguments）
     */
    private String buildJsonrpcRequest(String method, String id, Map<String, Object> params) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        // MCP JSON-RPC: params 直接包含 name + arguments，不额外嵌套一层 "params"
        request.put("params", params);
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build JSON-RPC request", e);
        }
    }

    private List<McpToolDefinition> parseToolsFromResponse(String response, Long serverId, String serverName) {
        List<McpToolDefinition> tools = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return tools;
        }

        try {
            var node = objectMapper.readTree(response);
            var result = node.get("result");
            if (result != null && result.has("tools")) {
                var toolsArray = result.get("tools");
                if (toolsArray != null && toolsArray.isArray()) {
                    for (var tool : toolsArray) {
                        String toolName = tool.has("name") ? tool.get("name").asText() : null;
                        String description = tool.has("description") ? tool.get("description").asText() : "";
                        String inputSchema = tool.has("inputSchema") ? tool.get("inputSchema").toString() : "{}";
                        if (toolName != null && !toolName.isBlank()) {
                            tools.add(McpToolDefinition.builder()
                                    .serverId(serverId)
                                    .serverName(serverName)
                                    .toolName(toolName)
                                    .fullName(McpToolDefinition.makeFullName(serverId, toolName))
                                    .description(description)
                                    .inputSchema(inputSchema)
                                    .build());
                        }
                    }
                }
            }
            // 直接数组格式
            if (tools.isEmpty() && node.isArray()) {
                for (var tool : node) {
                    String toolName = tool.has("name") ? tool.get("name").asText() : null;
                    String description = tool.has("description") ? tool.get("description").asText() : "";
                    String inputSchema = tool.has("inputSchema") ? tool.get("inputSchema").toString() : "{}";
                    if (toolName != null && !toolName.isBlank()) {
                        tools.add(McpToolDefinition.builder()
                                .serverId(serverId)
                                .serverName(serverName)
                                .toolName(toolName)
                                .fullName(McpToolDefinition.makeFullName(serverId, toolName))
                                .description(description)
                                .inputSchema(inputSchema)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[McpConnectionPool] Failed to parse tools response for serverId={}: {}", serverId, e.getMessage());
        }

        log.info("[McpConnectionPool] Parsed {} tools from serverId={}", tools.size(), serverId);
        return tools;
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
        repository.findById(serverId).ifPresent(server -> {
            server.markDisconnected();
            repository.save(server);
        });
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
                if (config.isStdio()) {
                    return executeStdioTool(server, toolName, args);
                } else if (config.isSse() || config.isHttp()) {
                    return executeHttpTool(server, toolName, args);
                } else {
                    return McpToolResult.failed("Unknown server type");
                }
            } catch (Exception e) {
                log.error("[McpConnectionPool] Tool execution failed serverId={} tool={}", serverId, toolName, e);
                return McpToolResult.failed(e.getMessage());
            }
        }, executor);
    }

    private McpToolResult executeStdioTool(McpServer server, String toolName, Map<String, Object> args) {
        McpServerConfig config = server.getConfig();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmd = new ArrayList<>();
            cmd.add(config.getCommand());
            if (config.getArgs() != null) cmd.addAll(config.getArgs());
            pb.command(cmd);
            if (config.getEnv() != null) pb.environment().putAll(config.getEnv());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", toolName);
            params.put("arguments", args != null ? args : Collections.emptyMap());

            String callRequest = buildJsonrpcRequest("tools/call", UUID.randomUUID().toString(), params);

            StringBuilder response = new StringBuilder();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                writer.write(callRequest + "\n");
                writer.flush();
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null && count++ < 100) {
                    response.append(line);
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) process.destroyForcibly();

            return parseToolResult(response.toString());
        } catch (Exception e) {
            return McpToolResult.failed(e.getMessage());
        }
    }

    private McpToolResult executeHttpTool(McpServer server, String toolName, Map<String, Object> args) {
        McpServerConfig config = server.getConfig();
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            String url = config.getUrl();
            if (url == null || url.isBlank()) {
                return McpToolResult.failed("No URL configured");
            }
            if (config.getEndpoint() != null && !config.getEndpoint().isBlank()) {
                if (url.endsWith("/") && config.getEndpoint().startsWith("/")) {
                    url = url + config.getEndpoint().substring(1);
                } else if (!url.endsWith("/") && !config.getEndpoint().startsWith("/")) {
                    url = url + "/" + config.getEndpoint();
                } else {
                    url = url + config.getEndpoint();
                }
            }

            Map<String, Object> params = new LinkedHashMap<>();
            params.put("name", toolName);
            params.put("arguments", args != null ? args : Collections.emptyMap());

            String requestBody = buildJsonrpcRequest("tools/call", UUID.randomUUID().toString(), params);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (config.getHeaders() != null) {
                config.getHeaders().forEach(reqBuilder::header);
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            
            String responseStr = resp.body();
            if (responseStr != null && responseStr.contains("data: {\"jsonrpc\"")) {
                int idx = responseStr.indexOf("data: {");
                responseStr = responseStr.substring(idx + 6).trim();
            }
            
            return parseToolResult(responseStr);
        } catch (Exception e) {
            return McpToolResult.failed(e.getMessage());
        }
    }

    private McpToolResult parseToolResult(String response) {
        if (response == null || response.isBlank()) {
            return McpToolResult.failed("Empty response");
        }
        try {
            var node = objectMapper.readTree(response);
            var result = node.get("result");
            if (result != null) {
                if (result.has("content")) {
                    var content = result.get("content");
                    if (content.isArray() && content.size() > 0) {
                        var first = content.get(0);
                        if (first.has("text")) {
                            return McpToolResult.success(first.get("text").asText());
                        }
                    }
                    return McpToolResult.success(content.toString());
                }
                return McpToolResult.success(result.toString());
            }
            if (node.has("error")) {
                var error = node.get("error");
                String msg = error.has("message") ? error.get("message").asText() : "Unknown error";
                return McpToolResult.failed(msg);
            }
            return McpToolResult.success(response);
        } catch (Exception e) {
            return McpToolResult.success(response);
        }
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
