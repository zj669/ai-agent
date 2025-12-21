package com.zj.aiagent.application.agent.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型 DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelDTO {

    /**
     * 数据库主键
     */
    private Long id;

    /**
     * 模型ID
     */
    private String modelId;

    /**
     * 模型名称
     */
    private String modelName;

    /**
     * 模型类型
     */
    private String modelType;

    /**
     * 关联的API ID
     */
    private String apiId;

    /**
     * 状态
     */
    private Integer status;
}
