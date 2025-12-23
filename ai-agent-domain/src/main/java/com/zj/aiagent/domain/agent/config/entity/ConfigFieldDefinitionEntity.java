package com.zj.aiagent.domain.agent.config.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置字段定义实体
 *
 * @author zj
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFieldDefinitionEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 配置类型
     */
    private String configType;

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 字段标签
     */
    private String fieldLabel;

    /**
     * 字段类型
     */
    private String fieldType;

    /**
     * 是否必填
     */
    private Boolean required;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 可选项（JSON数组字符串）
     */
    private String options;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 获取解析后的选项列表
     */
    public List<String> getParsedOptions() {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return com.alibaba.fastjson2.JSON.parseArray(options, String.class);
        } catch (Exception e) {
            return null;
        }
    }
}
