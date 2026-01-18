package com.zj.aiagent.application.agent.dto;

import lombok.Data;
import java.util.List;

/**
 * 配置字段分组 DTO
 * 用于按 groupName 将配置字段分组展示
 */
@Data
public class ConfigFieldGroupDTO {
    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 该分组下的配置字段列表
     */
    private List<ConfigFieldDTO> fields;
}
