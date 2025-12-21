package com.zj.aiagent.interfaces.web.dto.response.agent.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 工具响应 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MCP 工具信息")
public class McpToolResponse {

    @Schema(description = "数据库主键")
    private Long id;

    @Schema(description = "MCP ID")
    private String mcpId;

    @Schema(description = "MCP 名称")
    private String mcpName;

    @Schema(description = "传输类型")
    private String transportType;

    @Schema(description = "传输配置")
    private String transportConfig;

    @Schema(description = "请求超时时间（分钟）")
    private Integer requestTimeout;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;
}
