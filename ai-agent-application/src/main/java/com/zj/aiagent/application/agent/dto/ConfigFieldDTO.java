package com.zj.aiagent.application.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ConfigFieldDTO {
    private Long fieldId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private JsonNode options;
    private String defaultValue;
    private String placeholder;
    private String description;
    private JsonNode validationRules;

    // Mapping specific properties
    private String groupName;
    private Integer sortOrder;
    private String overrideDefault;
    private Integer isRequired;
}
