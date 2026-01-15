package com.zj.aiagent.infrastructure.workflow.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * graphJson 顶层结构 DTO
 * 用于反序列化前端存储的工作流图 JSON
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphJsonDTO {

    /**
     * 图唯一标识
     */
    @JsonProperty("dagId")
    private String dagId;

    /**
     * 版本号
     */
    private String version;

    /**
     * 描述
     */
    private String description;

    /**
     * 起始节点ID
     */
    private String startNodeId;

    /**
     * 节点列表
     */
    private List<NodeJsonDTO> nodes;

    /**
     * 边列表
     */
    private List<EdgeJsonDTO> edges;
}
