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
 * SSE MCP 传输实现
 * <p>
 * 处理 SSE 流式响应，内置健壮的逐行解析逻辑。
 * <p>
 * SSE 格式规范（EventStream）：每条消息以 "event:" 或 "data:" 开头，
 * 空行标志消息结束。解析器逐行扫描：
 * <ul>
 *   <li>跳过 "event:" 行（仅记录事件类型）</li>
 *   <li>累加 "data:" 行后的内容</li>
 *   <li>空行触发 JSON 解析（若已积累内容）</li>
 *   <li>忽略注释行（以 ":" 开头但非 "data:" / "event:"）</li>
 * </ul>
 * <p>
 * 已知 bug 修复：旧代码使用 {@code response.contains("data:")} 匹配 SSE 响应，
 * 当服务器在 data: 前发送 event: 行时匹配失败，导致"无法获取工具"。
 */
@Slf4j
public class SseMcpTransport extends AbstractMcpTransport {

    private final HttpClient httpClient;

    public SseMcpTransport(ObjectMapper objectMapper) {
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
                    .header("Accept", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofSeconds("tools/call".equals(method) ? 60 : 30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

            if (config.getHeaders() != null) {
                config.getHeaders().forEach(reqBuilder::header);
            }

            HttpResponse<String> resp = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                log.warn("[MCP][SSE] HTTP {} for serverId={}: {}", resp.statusCode(), server.getId(), resp.body());
                return "";
            }

            // 复用父类的 SSE 解析逻辑
            return parseSseContent(resp.body());
        } catch (Exception e) {
            log.error("[MCP][SSE] Request failed serverId={} method={}", server.getId(), method, e);
            return "";
        }
    }

    private String resolveUrl(McpServerConfig config) {
        String url = config.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("No URL configured for SSE transport");
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
