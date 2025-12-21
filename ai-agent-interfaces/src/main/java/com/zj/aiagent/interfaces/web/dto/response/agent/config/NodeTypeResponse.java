package com.zj.aiagent.interfaces.web.dto.response.agent.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 节点类型响应 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "节点类型信息")
public class NodeTypeResponse {

    @Schema(description = "节点类型枚举值")
    private String nodeType;

    @Schema(description = "节点类型数值")
    private Integer nodeTypeValue;

    @Schema(description = "节点展示名称")
    private String nodeName;

    @Schema(description = "节点描述")
    private String description;

    @Schema(description = "节点图标")
    private String icon;

    @Schema(description = "支持的配置项列表")
    private List<String> supportedConfigs;
}
