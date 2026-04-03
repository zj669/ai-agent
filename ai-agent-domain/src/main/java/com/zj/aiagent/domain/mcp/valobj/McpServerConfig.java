package com.zj.aiagent.domain.mcp.valobj;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * MCP 服务器配置值对象
 * 支持三种传输协议：stdio / sse / http
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfig {

    /**
     * 服务器类型: stdio / sse / http
     */
    private String type;

    // --- stdio 配置 ---
    /**
     * 命令（如 npx、node、python）
     */
    private String command;

    /**
     * 命令行参数列表
     */
    private List<String> args;

    /**
     * 环境变量
     */
    private Map<String, String> env;

    // --- sse / http 配置 ---
    /**
     * 服务器 URL
     */
    private String url;

    /**
     * HTTP 请求头（如 Authorization）
     */
    private Map<String, String> headers;

    /**
     * HTTP endpoint 路径（仅 http 类型使用）
     */
    private String endpoint;

    /**
     * 判断是否为 stdio 类型
     */
    public boolean isStdio() {
        return "stdio".equalsIgnoreCase(type);
    }

    /**
     * 判断是否为 sse 类型
     */
    public boolean isSse() {
        return "sse".equalsIgnoreCase(type);
    }

    /**
     * 判断是否为 http 类型
     */
    public boolean isHttp() {
        return "http".equalsIgnoreCase(type);
    }
}
