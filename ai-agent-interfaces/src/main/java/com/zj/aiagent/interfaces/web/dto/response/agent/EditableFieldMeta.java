package com.zj.aiagent.interfaces.web.dto.response.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可编辑字段元数据DTO
 *
 * @author zj
 * @since 2025-12-30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditableFieldMeta {

    /**
     * 字段键名 (对应 stateData 中的 key)
     */
    private String key;

    /**
     * 显示标签
     */
    private String label;

    /**
     * 字段类型 (json, text, list, messages)
     */
    private String type;

    /**
     * 字段描述
     */
    private String description;

    /**
     * 是否可编辑
     */
    private Boolean editable;
}
