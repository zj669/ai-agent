package com.zj.aiagemt.service.dag.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 节点配置类 - 存储节点的可配置项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeConfig {

    /**
     * 系统提示词(数据库配置，用户不可修改)
     */
    private String systemPrompt;

    /**
     * 模型完整配置
     */
    private ModelConfig model;

//    /**
//     * 记忆配置
//     */
//    private MemoryConfig memory;

    /**
     * 用户提示词
     */
    private String userPrompt;

    /**
     * 当前节点功能描述（影响决策节点执行）
     */
    private String description;

    /**
     * Advisor配置列表
     */
    private List<AdvisorConfig> advisors;

    /**
     * MCP工具配置列表
     */
    private List<McpToolConfig> mcpTools;

    /**
     * 节点超时时间(毫秒)
     */
    private Long timeout;

    /**
     * 自定义配置(针对特定节点类型)
     */
    private Map<String, Object> customConfig;
}
