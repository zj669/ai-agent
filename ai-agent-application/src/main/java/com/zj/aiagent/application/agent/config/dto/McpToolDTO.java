package com.zj.aiagent.application.agent.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolDTO {

    /**
     * 数据库主键
     */
    private Long id;

    /**
     * MCP ID
     */
    private String mcpId;

    /**
     * MCP 名称
     */
    private String mcpName;

    /**
     * 传输类型
     */
    private String transportType;

    /**
     * 传输配置
     */
    private String transportConfig;

    /**
     * 请求超时时间
     */
    private Integer requestTimeout;

    /**
     * 状态
     */
    private Integer status;
}
