package com.zj.aiagent.infrastructure.workflow.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 位置 DTO
 * 对应节点的前端可视化位置
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PositionDTO {

    /**
     * X 坐标
     */
    private Double x;

    /**
     * Y 坐标
     */
    private Double y;
}
