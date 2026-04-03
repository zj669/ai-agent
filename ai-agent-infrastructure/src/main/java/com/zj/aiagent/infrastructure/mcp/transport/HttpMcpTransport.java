package com.zj.aiagent.infrastructure.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * HTTP MCP 传输实现
 * <p>
 * 处理标准 HTTP POST JSON-RPC 通信
 */
@Slf4j
public class HttpMcpTransport extends AbstractMcpTransport {

    private final HttpClient httpClient;

    public HttpMcpTransport(ObjectMapper objectMapper) {
        super(objectMapper);
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    protected String sendJsonrpcRequest(McpServer server, String method, Map<String, Object> params) {
        McpServerConfig config = server.getConfig();
        try {
            String url = resolveUrl(config);
            String requestBody = buildJsonrpcRequest(method, java.util.UUID.randomUUID().toString(), params);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    // exa.ai 等 MCP 服务器要求同时接受 application/json 和 text/event-stream
                    // 必须分两次 addHeader，不能用逗号拼接（服务器解析为单值导致 406）
                    .header("Accept", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds("tools/call".equals(method) ? 60 : 30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (config.getHeaders() != null) {
                config.getHeaders().forEach(reqBuilder::header);
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("[MCP][Http] HTTP {} for serverId={}: {}", resp.statusCode(), server.getId(), resp.body());
                return "";
            }

            // exa.ai 返回 SSE 格式（event: message\ndata: {...}），需要解析后才能得到纯 JSON
            String body = resp.body();
            if (body != null && (body.startsWith("event:") || body.startsWith("data:"))) {
                log.debug("[MCP][Http] Detected SSE response for serverId={}, parsing SSE stream", server.getId());
                return parseSseContent(body);
            }
            return body;
        } catch (Exception e) {
            log.error("[MCP][Http] Request failed serverId={} method={}", server.getId(), method, e);
            return "";
        }
    }

    private String resolveUrl(McpServerConfig config) {
        String url = config.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("No URL configured for HTTP transport");
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
        return url;
    }
}
