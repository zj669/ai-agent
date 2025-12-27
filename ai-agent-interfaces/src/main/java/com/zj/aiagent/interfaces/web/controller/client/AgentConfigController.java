package com.zj.aiagent.interfaces.web.controller.client;

import com.zj.aiagent.application.config.IAgentConfigApplicationService;
import com.zj.aiagent.infrastructure.persistence.entity.AiConfigFieldDefinitionPO;
import com.zj.aiagent.infrastructure.persistence.entity.AiNodeTemplatePO;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.response.config.ConfigFieldResponse;
import com.zj.aiagent.interfaces.web.dto.response.config.NodeTemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/client/agent/config")
@Tag(name = "agent表单配置", description = "agent表单配置")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
                RequestMethod.GET,
                RequestMethod.POST,
                RequestMethod.OPTIONS
})
public class AgentConfigController {

        @Resource
        private IAgentConfigApplicationService agentConfigApplicationService;

        @GetMapping("/node-templates")
        @Operation(summary = "获取所有可用的节点模板")
        public Response<List<NodeTemplateResponse>> getNodeTemplates() {
                List<AiNodeTemplatePO> templates = agentConfigApplicationService.getNodeTemplates();
                List<NodeTemplateResponse> responses = templates.stream()
                                .map(this::toNodeTemplateResponse)
                                .collect(Collectors.toList());
                return Response.success(responses);
        }

        @GetMapping("/config-schema/{module}")
        @Operation(summary = "获取配置字段Schema")
        public Response<List<ConfigFieldResponse>> getConfigSchema(@PathVariable("module") String module) {
                List<AiConfigFieldDefinitionPO> fields = agentConfigApplicationService.getConfigSchema(module);
                List<ConfigFieldResponse> responses = fields.stream()
                                .map(this::toConfigFieldResponse)
                                .collect(Collectors.toList());
                return Response.success(responses);
        }

        /**
         * PO 转 DTO
         */
        private NodeTemplateResponse toNodeTemplateResponse(AiNodeTemplatePO po) {
                return NodeTemplateResponse.builder()
                                .templateId(po.getTemplateId())
                                .nodeType(po.getNodeType())
                                .nodeName(po.getNodeName())
                                .templateLabel(po.getTemplateLabel())
                                .description(po.getDescription())
                                .baseType(po.getBaseType())
                                .icon(po.getIcon())
                                .systemPromptTemplate(po.getSystemPromptTemplate())
                                .outputSchema(po.getOutputSchema())
                                .editableFields(po.getEditableFields())
                                .isBuiltIn(po.getIsBuiltIn())
                                .build();
        }

        /**
         * PO 转 DTO
         */
        private ConfigFieldResponse toConfigFieldResponse(AiConfigFieldDefinitionPO po) {
                return ConfigFieldResponse.builder()
                                .fieldName(po.getFieldName())
                                .fieldLabel(po.getFieldLabel())
                                .fieldType(po.getFieldType())
                                .required(po.getRequired())
                                .description(po.getDescription())
                                .defaultValue(po.getDefaultValue())
                                .options(po.getOptions())
                                .sortOrder(po.getSortOrder())
                                .build();
        }
}
