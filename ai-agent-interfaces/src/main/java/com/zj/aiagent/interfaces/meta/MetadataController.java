package com.zj.aiagent.interfaces.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zj.aiagent.application.agent.dto.NodeTemplateDTO;
import com.zj.aiagent.application.agent.service.MetadataApplicationService;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import com.zj.aiagent.infrastructure.workflow.executor.NodeExecutorFactory;
import com.zj.aiagent.interfaces.meta.dto.ToolMetadataDTO;
import com.zj.aiagent.shared.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 元数据控制器
 * 提供工具、节点类型等元数据查询接口
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetadataController {

    private final NodeExecutorFactory executorFactory;
    private final ObjectMapper objectMapper;

    private final MetadataApplicationService metadataService;

    @GetMapping("/node-templates")
    public Response<List<NodeTemplateDTO>> getNodeTemplates() {
        return Response.success(metadataService.getAllNodeTemplates());
    }

    /**
     * 获取所有工具元数据
     * 用于前端画布展示可用的节点工具
     */
    @GetMapping("/tools")
    public Response<List<ToolMetadataDTO>> getTools() {
        List<ToolMetadataDTO> tools = new ArrayList<>();

        // 遍历所有 NodeType，为每个创建元数据
        for (NodeType nodeType : NodeType.values()) {
            if (nodeType == NodeType.START || nodeType == NodeType.END) {
                continue; // 跳过流程控制节点
            }

            ToolMetadataDTO dto = new ToolMetadataDTO();
            dto.setToolId(nodeType.name().toLowerCase());
            dto.setName(getNodeTypeName(nodeType));
            dto.setDescription(getNodeTypeDescription(nodeType));
            dto.setIcon(getNodeTypeIcon(nodeType));
            dto.setInputSchema(createInputSchema(nodeType));
            dto.setOutputSchema(createOutputSchema(nodeType));

            tools.add(dto);
        }

        return Response.success(tools);
    }

    private String getNodeTypeName(NodeType nodeType) {
        return switch (nodeType) {
            case LLM -> "大语言模型";
            case HTTP -> "HTTP 请求";
            case CONDITION -> "条件判断";
            case TOOL -> "MCP 工具";
            default -> nodeType.name();
        };
    }

    private String getNodeTypeDescription(NodeType nodeType) {
        return switch (nodeType) {
            case LLM -> "调用大语言模型生成文本";
            case HTTP -> "发送 HTTP 请求获取数据";
            case CONDITION -> "根据条件分支执行";
            case TOOL -> "调用 MCP 工具";
            default -> "未知节点类型";
        };
    }

    private String getNodeTypeIcon(NodeType nodeType) {
        // 返回图标 URL 或 emoji
        return switch (nodeType) {
            case LLM -> "🤖";
            case HTTP -> "🌐";
            case CONDITION -> "🔀";
            case TOOL -> "🔧";
            default -> "⚙️";
        };
    }

    private ObjectNode createInputSchema(NodeType nodeType) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();

        switch (nodeType) {
            case LLM -> {
                ObjectNode prompt = objectMapper.createObjectNode();
                prompt.put("type", "string");
                prompt.put("description", "提示词");
                properties.set("prompt", prompt);
            }
            case HTTP -> {
                ObjectNode url = objectMapper.createObjectNode();
                url.put("type", "string");
                url.put("description", "请求 URL");
                properties.set("url", url);
            }
            case CONDITION -> {
                ObjectNode condition = objectMapper.createObjectNode();
                condition.put("type", "string");
                condition.put("description", "条件表达式");
                properties.set("condition", condition);
            }
        }

        schema.set("properties", properties);
        return schema;
    }

    private ObjectNode createOutputSchema(NodeType nodeType) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode output = objectMapper.createObjectNode();
        output.put("type", "string");
        output.put("description", "节点输出结果");
        properties.set("output", output);

        schema.set("properties", properties);
        return schema;
    }
}
