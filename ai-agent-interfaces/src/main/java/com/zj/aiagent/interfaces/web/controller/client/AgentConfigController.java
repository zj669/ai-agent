package com.zj.aiagent.interfaces.web.controller.client;

import com.zj.aiagent.application.agent.config.AgentConfigApplicationService;
import com.zj.aiagent.application.agent.config.dto.AdvisorDTO;
import com.zj.aiagent.application.agent.config.dto.McpToolDTO;
import com.zj.aiagent.application.agent.config.dto.ModelDTO;
import com.zj.aiagent.application.agent.config.dto.NodeTypeDTO;
import com.zj.aiagent.interfaces.common.Response;
import com.zj.aiagent.interfaces.web.dto.response.agent.config.AdvisorResponse;
import com.zj.aiagent.interfaces.web.dto.response.agent.config.McpToolResponse;
import com.zj.aiagent.interfaces.web.dto.response.agent.config.ModelResponse;
import com.zj.aiagent.interfaces.web.dto.response.agent.config.NodeTypeResponse;
import com.zj.aiagent.interfaces.web.dto.response.config.ConfigDefinitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent 配置控制器
 *
 * @author zj
 * @since 2025-12-21
 */
@Slf4j
@RestController
@RequestMapping("/client/agent/config")
@Tag(name = "Agent 配置管理", description = "Agent 拖拽配置相关接口")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.OPTIONS
})
public class AgentConfigController {

    @Resource
    private AgentConfigApplicationService agentConfigApplicationService;

    /**
     * 查询所有节点类型
     *
     * @return 节点类型列表
     */
    @GetMapping("/node-types")
    @Operation(summary = "查询节点类型列表", description = "获取所有可用的节点类型及其支持的配置项")
    public Response<List<NodeTypeResponse>> getNodeTypes() {
        try {
            log.info("查询节点类型列表");

            List<NodeTypeDTO> nodeTypeDTOList = agentConfigApplicationService.getNodeTypes();

            List<NodeTypeResponse> responseList = nodeTypeDTOList.stream()
                    .map(this::convertToNodeTypeResponse)
                    .collect(Collectors.toList());

            return Response.success(responseList);

        } catch (Exception e) {
            log.error("查询节点类型列表失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有模型
     *
     * @return 模型列表
     */
    @GetMapping("/models")
    @Operation(summary = "查询模型列表", description = "获取所有启用的模型配置")
    public Response<List<ModelResponse>> getModels() {
        try {
            log.info("查询模型列表");

            List<ModelDTO> modelDTOList = agentConfigApplicationService.getModels();

            List<ModelResponse> responseList = modelDTOList.stream()
                    .map(this::convertToModelResponse)
                    .collect(Collectors.toList());

            return Response.success(responseList);

        } catch (Exception e) {
            log.error("查询模型列表失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有 Advisor
     *
     * @return Advisor 列表
     */
    @GetMapping("/advisors")
    @Operation(summary = "查询 Advisor 列表", description = "获取所有启用的 Advisor 配置")
    public Response<List<AdvisorResponse>> getAdvisors() {
        try {
            log.info("查询 Advisor 列表");

            List<AdvisorDTO> advisorDTOList = agentConfigApplicationService.getAdvisors();

            List<AdvisorResponse> responseList = advisorDTOList.stream()
                    .map(this::convertToAdvisorResponse)
                    .collect(Collectors.toList());

            return Response.success(responseList);

        } catch (Exception e) {
            log.error("查询 Advisor 列表失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有 MCP 工具
     *
     * @return MCP 工具列表
     */
    @GetMapping("/mcp-tools")
    @Operation(summary = "查询 MCP 工具列表", description = "获取所有启用的 MCP 工具配置")
    public Response<List<McpToolResponse>> getMcpTools() {
        try {
            log.info("查询 MCP 工具列表");

            List<McpToolDTO> mcpToolDTOList = agentConfigApplicationService.getMcpTools();

            List<McpToolResponse> responseList = mcpToolDTOList.stream()
                    .map(this::convertToMcpToolResponse)
                    .collect(Collectors.toList());

            return Response.success(responseList);

        } catch (Exception e) {
            log.error("查询 MCP 工具列表失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 转换节点类型 DTO 到响应
     */
    private NodeTypeResponse convertToNodeTypeResponse(NodeTypeDTO dto) {
        return NodeTypeResponse.builder()
                .nodeType(dto.getNodeType())
                .nodeTypeValue(dto.getNodeTypeValue())
                .nodeName(dto.getNodeName())
                .description(dto.getDescription())
                .icon(dto.getIcon())
                .supportedConfigs(dto.getSupportedConfigs())
                .build();
    }

    /**
     * 转换模型 DTO 到响应
     */
    private ModelResponse convertToModelResponse(ModelDTO dto) {
        return ModelResponse.builder()
                .id(dto.getId())
                .modelId(dto.getModelId())
                .modelName(dto.getModelName())
                .modelType(dto.getModelType())
                .apiId(dto.getApiId())
                .status(dto.getStatus())
                .build();
    }

    /**
     * 转换 Advisor DTO 到响应
     */
    private AdvisorResponse convertToAdvisorResponse(AdvisorDTO dto) {
        return AdvisorResponse.builder()
                .id(dto.getId())
                .advisorId(dto.getAdvisorId())
                .advisorName(dto.getAdvisorName())
                .advisorType(dto.getAdvisorType())
                .orderNum(dto.getOrderNum())
                .extParam(dto.getExtParam())
                .status(dto.getStatus())
                .build();
    }

    /**
     * 转换 MCP 工具 DTO 到响应
     */
    private McpToolResponse convertToMcpToolResponse(McpToolDTO dto) {
        return McpToolResponse.builder()
                .id(dto.getId())
                .mcpId(dto.getMcpId())
                .mcpName(dto.getMcpName())
                .transportType(dto.getTransportType())
                .transportConfig(dto.getTransportConfig())
                .requestTimeout(dto.getRequestTimeout())
                .status(dto.getStatus())
                .build();
    }

    /**
     * 查询节点配置项定义
     *
     * @param nodeType 节点类型（可选）
     * @return 配置项定义列表
     */
    @GetMapping("/config-definitions")
    @Operation(summary = "查询配置项定义", description = "获取配置项的类型定义和表单 Schema")
    public Response<List<com.zj.aiagent.interfaces.web.dto.response.config.ConfigDefinitionResponse>> getConfigDefinitions(
            @RequestParam(required = false) String nodeType) {
        try {
            log.info("查询配置项定义，节点类型: {}", nodeType);

            List<AgentConfigApplicationService.ConfigDefinitionDTO> dtoList = agentConfigApplicationService
                    .getConfigDefinitions(nodeType);

            List<ConfigDefinitionResponse> responseList = dtoList
                    .stream()
                    .map(this::convertToConfigDefinitionResponse)
                    .collect(Collectors.toList());

            return Response.success(responseList);

        } catch (Exception e) {
            log.error("查询配置项定义失败", e);
            return Response.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 转换配置定义 DTO 到响应
     */
    private ConfigDefinitionResponse convertToConfigDefinitionResponse(
            AgentConfigApplicationService.ConfigDefinitionDTO dto) {

        List<ConfigDefinitionResponse.ConfigOption> options = dto.getOptions().stream()
                .map(opt -> ConfigDefinitionResponse.ConfigOption.builder()
                        .id(opt.getId())
                        .name(opt.getName())
                        .type(opt.getType())
                        .extra(opt.getExtra())
                        .build())
                .collect(Collectors.toList());

        return ConfigDefinitionResponse.builder()
                .configType(dto.getConfigType())
                .configName(dto.getConfigName())
                .options(options)
                .build();
    }
}
