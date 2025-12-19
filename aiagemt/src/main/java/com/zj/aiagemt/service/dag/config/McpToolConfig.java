package com.zj.aiagemt.service.dag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP工具配置类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolConfig {

    /**
     * MCP工具ID
     */
    private String mcpId;

    /**
     * MCP工具名称
     */
    private String mcpName;

    /**
     * MCP工具类型 (FILE_SYSTEM, CODE_EXECUTOR, WEB_SEARCH, etc.)
     */
    private String mcpType;
}
