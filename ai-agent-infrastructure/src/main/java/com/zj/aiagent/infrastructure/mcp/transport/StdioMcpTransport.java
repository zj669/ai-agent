package com.zj.aiagent.infrastructure.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Stdio MCP 传输实现
 * <p>
 * 处理本地进程 stdio 通信：启动子进程，通过 stdin 发送 JSON-RPC 请求，读取 stdout 响应
 */
@Slf4j
public class StdioMcpTransport extends AbstractMcpTransport {

    public StdioMcpTransport(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    protected String sendJsonrpcRequest(McpServer server, String method, Map<String, Object> params) {
        McpServerConfig config = server.getConfig();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            List<String> cmd = new ArrayList<>();
            cmd.add(config.getCommand());
            if (config.getArgs() != null) {
                cmd.addAll(config.getArgs());
            }
            pb.command(cmd);

            if (config.getEnv() != null) {
                pb.environment().putAll(config.getEnv());
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            String id = java.util.UUID.randomUUID().toString();
            String request = buildJsonrpcRequest(method, id, params);

            StringBuilder response = new StringBuilder();
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                writer.write(request + "\n");
                writer.flush();

                // 对于 initialize 方法，先发送 initialize + notifications/initialized，再发送 tools/list
                if ("tools/list".equals(method)) {
                    String initRequest = buildJsonrpcRequest("initialize", java.util.UUID.randomUUID().toString(), Map.of(
                            "protocolVersion", "2024-11-05",
                            "capabilities", Map.of("tools", Map.of()),
                            "clientInfo", Map.of("name", "ai-agent", "version", "1.0.0")
                    ));
                    String initializedRequest = buildJsonrpcRequest("notifications/initialized", java.util.UUID.randomUUID().toString(), Map.of());
                    writer.write(initRequest + "\n");
                    writer.write(initializedRequest + "\n");
                    writer.flush();
                }

                String line;
                int count = 0;
                int maxLines = "tools/call".equals(method) ? 100 : 50;
                while ((line = reader.readLine()) != null && count++ < maxLines) {
                    response.append(line);
                }
            }

            boolean finished = process.waitFor("tools/call".equals(method) ? 10 : 30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            }

            return response.toString();
        } catch (Exception e) {
            log.error("[MCP][Stdio] Request failed serverId={} method={}", server.getId(), method, e);
            return "";
        }
    }
}
