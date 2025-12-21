package com.zj.aiagent.application.agent.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Advisor DTO
 *
 * @author zj
 * @since 2025-12-21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvisorDTO {

    /**
     * 数据库主键
     */
    private Long id;

    /**
     * Advisor ID
     */
    private String advisorId;

    /**
     * Advisor 名称
     */
    private String advisorName;

    /**
     * Advisor 类型
     */
    private String advisorType;

    /**
     * 顺序号
     */
    private Integer orderNum;

    /**
     * 扩展参数配置
     */
    private String extParam;

    /**
     * 状态
     */
    private Integer status;
}
