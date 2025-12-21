package com.zj.aiagent.application.agent.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 节点类型 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeTypeDTO {

    /**
     * 节点类型枚举值
     */
    private String nodeType;

    /**
     * 节点类型数值
     */
    private Integer nodeTypeValue;

    /**
     * 节点展示名称
     */
    private String nodeName;

    /**
     * 节点描述
     */
    private String description;

    /**
     * 节点图标
     */
    private String icon;

    /**
     * 支持的配置项列表
     */
    private List<String> supportedConfigs;
}
