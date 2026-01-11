package com.zj.aiagent.application.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import java.util.List;

@Data
public class NodeTemplateDTO {
    private Long id;
    private String typeCode;
    private String name;
    private String description;
    private String icon;
    private String category;
    private Integer sortOrder;
    private JsonNode defaultSchemaPolicy;
    private JsonNode initialSchema;

    // Configuration Fields
    private List<ConfigFieldDTO> configFields;
}
