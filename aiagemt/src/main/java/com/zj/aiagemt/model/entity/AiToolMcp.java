package com.zj.aiagemt.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI工具MCP配置实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("ai_tool_mcp")
public class AiToolMcp {

    /**
     * 主键ID
     */
    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * MCP ID
     */
    @Schema(description = "MCP ID")
    private String mcpId;

    /**
     * MCP名称
     */
    @Schema(description = "MCP名称")
    private String mcpName;

    /**
     * 传输类型(sse/stdio)
     */
    @Schema(description = "传输类型")
    private String transportType;

    /**
     * 传输配置(sse/stdio)
     */
    @Schema(description = "传输配置")
    private String transportConfig;

    /**
     * 请求超时时间(分钟)
     */
    @Schema(description = "请求超时时间")
    private Integer requestTimeout;

    /**
     * 状态(0:禁用,1:启用)
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
