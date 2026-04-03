package com.zj.aiagent.infrastructure.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.mcp.valobj.McpServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MCP 传输工厂
 * <p>
 * 根据 {@link McpServerConfig#getType()} 动态生成对应的传输策略实例。
 * 支持三种传输类型：stdio / http / sse
 */
@Slf4j
@Component
public class McpTransportFactory {

    private final ObjectMapper objectMapper;
    private final StdioMcpTransport stdioTransport;
    private final HttpMcpTransport httpTransport;
    private final SseMcpTransport sseTransport;

    public McpTransportFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.stdioTransport = new StdioMcpTransport(objectMapper);
        this.httpTransport = new HttpMcpTransport(objectMapper);
        this.sseTransport = new SseMcpTransport(objectMapper);
    }

    /**
     * 根据配置获取对应的传输策略
     *
     * @param config MCP 服务器配置
     * @return 传输策略实例
     */
    public IMcpTransport getTransport(McpServerConfig config) {
        if (config == null) {
            log.warn("[MCP][Factory] No config provided, defaulting to HTTP transport");
            return httpTransport;
        }

        if (config.isStdio()) {
            return stdioTransport;
        }
        if (config.isSse()) {
            return sseTransport;
        }
        if (config.isHttp()) {
            return httpTransport;
        }

        log.warn("[MCP][Factory] Unknown transport type '{}', defaulting to HTTP", config.getType());
        return httpTransport;
    }
}
