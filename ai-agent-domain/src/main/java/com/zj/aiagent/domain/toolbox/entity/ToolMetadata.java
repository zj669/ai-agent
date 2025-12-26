package com.zj.aiagent.domain.toolbox.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工具元数据
 * <p>
 * 描述一个 MCP 工具的基本信息，用于智能推荐和 Prompt 注入
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ToolMetadata {

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具描述
     */
    private String description;

    /**
     * 工具分类（search, calculator, weather, etc.）
     */
    private String category;

    /**
     * 输入参数 Schema（JSON Schema 格式）
     */
    private Map<String, Object> inputSchema;

    /**
     * 相关性评分（0-1，用于智能推荐）
     */
    private Double relevanceScore;

    /**
     * 工具提供者（哪个 MCP 客户端）
     */
    private String provider;
}
