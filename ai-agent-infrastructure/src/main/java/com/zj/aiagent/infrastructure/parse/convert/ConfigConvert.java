package com.zj.aiagent.infrastructure.parse.convert;

import com.zj.aiagent.domain.model.parse.entity.ModelConfigEntity;
import com.zj.aiagent.domain.prompt.parse.entity.PromptConfigEntity;
import com.zj.aiagent.domain.toolbox.parse.entity.McpConfigEntity;
import com.zj.aiagent.infrastructure.parse.entity.GraphJsonSchema;

public class ConfigConvert {
    public static McpConfigEntity convertMcp(GraphJsonSchema.NodeDefinition node){
        return McpConfigEntity.builder()
                .config(node.getConfig())
                .build();
    }

    public static PromptConfigEntity convertPrompt(GraphJsonSchema.NodeDefinition node){
        return PromptConfigEntity.builder()
                .config(node.getConfig())
                .build();
    }

    public static ModelConfigEntity convertModel(GraphJsonSchema.NodeDefinition node){
        return ModelConfigEntity.builder()
                .config(node.getConfig())
                .build();
    }
}
