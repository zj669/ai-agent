package com.zj.aiagemt.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI智能体信息DTO
 * 
 * @author zj
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentInfoDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * AI智能体ID
     */
    private String id;

    /**
     * AI智能体名称
     */
    private String name;

    /**
     * AI智能体描述
     */
    private String description;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 智能体类型
     */
    private String type;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 更新时间
     */
    private String updateTime;
}