package com.zj.aiagent.domain.agent.config.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点模板领域实体
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeTemplateEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点展示名称
     */
    private String nodeName;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 前端图标标识
     */
    private String icon;

    /**
     * 默认系统提示词
     */
    private String defaultSystemPrompt;

    /**
     * 前端表单配置Schema (JSON格式)
     */
    private String configSchema;
}
