package com.zj.aiagent.infrastructure.mcp.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.mcp.entity.McpServer;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MCP 传输抽象基类
 * <p>
 * 提供 ObjectMapper 注入、JSON-RPC 请求构建、工具解析等通用逻辑
 */
@Slf4j
public abstract class AbstractMcpTransport implements IMcpTransport {

    protected final ObjectMapper objectMapper;

    protected AbstractMcpTransport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public List<McpToolDefinition> discoverTools(McpServer server) {
        String response = sendJsonrpcRequest(server, "tools/list", Collections.emptyMap());
        return parseToolsFromResponse(response, server.getId(), server.getName());
    }

    @Override
    public McpToolResult executeTool(McpServer server, String toolName, Map<String, Object> arguments) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments != null ? arguments : Collections.emptyMap());
        String response = sendJsonrpcRequest(server, "tools/call", params);
        return parseToolResult(response);
    }

    /**
     * 发送 JSON-RPC 请求（子类实现具体传输方式）
     *
     * @param server MCP 服务器实体
     * @param method JSON-RPC 方法名
     * @param params 请求参数
     * @return 原始响应字符串
     */
    protected abstract String sendJsonrpcRequest(McpServer server, String method, Map<String, Object> params);

    /**
     * 构建 JSON-RPC 请求字符串
     */
    protected String buildJsonrpcRequest(String method, String id, Map<String, Object> params) {
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

    /**
     * 从响应中解析工具列表
     */
    protected List<McpToolDefinition> parseToolsFromResponse(String response, Long serverId, String serverName) {
        List<McpToolDefinition> tools = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return tools;
        }

        try {
            JsonNode node = objectMapper.readTree(response);
            JsonNode result = node.get("result");
            if (result != null && result.has("tools")) {
                JsonNode toolsArray = result.get("tools");
                if (toolsArray != null && toolsArray.isArray()) {
                    for (JsonNode tool : toolsArray) {
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
                for (JsonNode tool : node) {
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
            log.warn("[MCP] Failed to parse tools response for serverId={}: {}", serverId, e.getMessage());
        }

        log.info("[MCP] Parsed {} tools from serverId={}", tools.size(), serverId);
        return tools;
    }

    /**
     * 从响应中解析工具执行结果
     */
    protected McpToolResult parseToolResult(String response) {
        if (response == null || response.isBlank()) {
            return McpToolResult.failed("Empty response");
        }
        try {
            JsonNode node = objectMapper.readTree(response);
            JsonNode result = node.get("result");
            if (result != null) {
                if (result.has("content")) {
                    JsonNode content = result.get("content");
                    if (content.isArray() && content.size() > 0) {
                        JsonNode first = content.get(0);
                        if (first.has("text")) {
                            return McpToolResult.success(first.get("text").asText());
                        }
                    }
                    return McpToolResult.success(content.toString());
                }
                return McpToolResult.success(result.toString());
            }
            if (node.has("error")) {
                JsonNode error = node.get("error");
                String msg = error.has("message") ? error.get("message").asText() : "Unknown error";
                return McpToolResult.failed(msg);
            }
            return McpToolResult.success(response);
        } catch (Exception e) {
            return McpToolResult.success(response);
        }
    }

    /**
     * 解析 SSE 流内容，返回第一个完整的 JSON-RPC 响应。
     * <p>
     * 算法：
     * <ol>
     *   <li>按行扫描（兼容 \n 和 \r\n）</li>
     *   <li>"event:" 行跳过（不积累）</li>
     *   <li>"data:" 行将冒号后内容追加到缓冲区</li>
     *   <li>空行触发解析：缓冲区非空且为有效 JSON → 返回</li>
     *   <li>流结束时检查缓冲区是否有未解析的完整 JSON</li>
     * </ol>
     *
     * @param sseContent SSE 原始文本
     * @return 第一个有效的 JSON-RPC 字符串；若解析失败返回空字符串
     */
    protected String parseSseContent(String sseContent) {
        if (sseContent == null || sseContent.isBlank()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        String[] lines = sseContent.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                if (buffer.length() > 0) {
                    String json = buffer.toString().trim();
                    if (isValidJson(json)) {
                        log.debug("[MCP][SSE] Parsed complete JSON ({} chars)", json.length());
                        return json;
                    }
                    buffer.setLength(0);
                }
                continue;
            }
            if (line.startsWith("event:")) {
                log.debug("[MCP][SSE] Skipping event directive: {}", line);
                continue;
            }
            if (line.startsWith("data:")) {
                buffer.append(line.substring("data:".length()));
                log.debug("[MCP][SSE] Accumulated data line (total buffer: {})", buffer.length());
                continue;
            }
            if (line.startsWith(":") && !line.startsWith("data:") && !line.startsWith("event:")) {
                log.debug("[MCP][SSE] Skipping comment line: {}", line);
                continue;
            }
            log.debug("[MCP][SSE] Skipping unrecognized line: {}", line);
        }
        // 流结束时检查缓冲区
        if (buffer.length() > 0) {
            String json = buffer.toString().trim();
            if (isValidJson(json)) {
                log.debug("[MCP][SSE] Parsed JSON at stream end ({} chars)", json.length());
                return json;
            }
        }
        log.warn("[MCP][SSE] No valid JSON-RPC response found in SSE stream");
        return "";
    }

    private boolean isValidJson(String text) {
        if (text == null || text.isBlank()) return false;
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
}
