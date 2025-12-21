package com.zj.aiagent.domain.agent.config.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具领域实体
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpToolEntity {

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
     * 传输配置（JSON）
     */
    private String transportConfig;

    /**
     * 请求超时时间（分钟）
     */
    private Integer requestTimeout;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;
}
