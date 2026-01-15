package com.zj.aiagent.interfaces.meta.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 工具元数据 DTO
 * 用于前端画布展示工具列表和 Schema
 */
@Data
public class ToolMetadataDTO {
    /**
     * 工具 ID (对应 NodeType)
     */
    private String toolId;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具图标 URL
     */
    private String icon;

    /**
     * 输入 Schema (JSON Schema 格式)
     */
    private JsonNode inputSchema;

    /**
     * 输出 Schema (JSON Schema 格式)
     */
    private JsonNode outputSchema;
}
