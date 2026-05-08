package com.zj.aiagent.infrastructure.workflow.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zj.aiagent.domain.mcp.port.IMcpToolRegistry;
import com.zj.aiagent.domain.mcp.valobj.McpToolDefinition;
import com.zj.aiagent.domain.mcp.valobj.McpToolResult;
import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 工具节点执行策略
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolNodeExecutorStrategy implements NodeExecutorStrategy {

    private static final String CONFIG_MCP_TOOL_NAME = "mcpToolName";
    private static final String CONFIG_SELECTED_TOOL = "selectedTool";
    private static final String CONFIG_SELECTED_TOOL_FULL_NAME = "fullName";
    private static final String OUTPUT_TOOL_RESPONSE = "tool_response";
    private static final String OUTPUT_RESULT = "result";
    private static final String ERROR_TOOL_NOT_CONFIGURED = "MCP 工具未配置，请在 TOOL 节点选择工具";

    private final IMcpToolRegistry mcpToolRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        String mcpToolName = resolveMcpToolName(node);
        String executionId = "node-" + node.getNodeId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[Tool Node {}] Preparing MCP tool: {}, executionId={}",
                node.getNodeId(), mcpToolName, executionId);

        if (mcpToolName == null || mcpToolName.isBlank()) {
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("error", ERROR_TOOL_NOT_CONFIGURED);
            return CompletableFuture.completedFuture(
                    NodeExecutionResult.failed(ERROR_TOOL_NOT_CONFIGURED, outputs));
        }

        // 解析格式: mcp__{serverId}__{toolName}
        Long serverId = McpToolDefinition.parseServerId(mcpToolName);
        String toolName = McpToolDefinition.parseToolName(mcpToolName);

        if (serverId == null || toolName == null) {
            log.error("[Tool Node {}] Invalid mcpToolName format: {}", node.getNodeId(), mcpToolName);
            return CompletableFuture.completedFuture(
                    NodeExecutionResult.failed("Invalid mcpToolName format: " + mcpToolName));
        }

        List<String> missingInputs = validateRequiredToolInputs(node, resolvedInputs);
        if (!missingInputs.isEmpty()) {
            String error = String.join("; ", missingInputs);
            log.error("[Tool Node {}] {}", node.getNodeId(), error);
            return CompletableFuture.completedFuture(
                NodeExecutionResult.failed(error, Map.of("error", error))
            );
        }

        Map<String, Object> toolArgs = new HashMap<>();
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            if (!entry.getKey().startsWith("__")) {
                toolArgs.put(entry.getKey(), entry.getValue());
            }
        }

        log.info(
            "[Tool Node {}] Executing MCP tool: {}, executionId={}, argKeys={}",
            node.getNodeId(),
            mcpToolName,
            executionId,
            toolArgs.keySet()
        );

        return mcpToolRegistry.execute(serverId, toolName, toolArgs, executionId)
                .thenApply(result -> {
                    if (result.isAborted()) {
                        log.info("[Tool Node {}] Tool execution aborted executionId={}",
                                node.getNodeId(), executionId);
                        Map<String, Object> outputs = new HashMap<>();
                        outputs.put("aborted", true);
                        return NodeExecutionResult.failed("Tool execution aborted", outputs);
                    }

                    if (!result.isSuccess()) {
                        log.error("[Tool Node {}] Tool execution failed: {}",
                                node.getNodeId(), result.getErrorMessage());
                        return NodeExecutionResult.failed(result.getErrorMessage());
                    }

                    Map<String, Object> outputs = new HashMap<>();
                    outputs.put(OUTPUT_TOOL_RESPONSE, result.getContent());
                    outputs.put(OUTPUT_RESULT, result.getContent());
                    log.info("[Tool Node {}] Tool executed successfully, executionId={}",
                            node.getNodeId(), executionId);
                    return NodeExecutionResult.success(outputs);
                })
                .exceptionally(ex -> {
                    log.error("[Tool Node {}] Tool execution exception executionId={}",
                            node.getNodeId(), executionId, ex);
                    return NodeExecutionResult.failed(ex.getMessage());
                });
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.TOOL;
    }

    private String resolveMcpToolName(Node node) {
        if (node == null || node.getConfig() == null) {
            return null;
        }

        String configuredToolName = node.getConfig().getString(CONFIG_MCP_TOOL_NAME);
        if (configuredToolName != null && !configuredToolName.isBlank()) {
            return configuredToolName;
        }

        Map<String, Object> selectedTool = node.getConfig().getMap(CONFIG_SELECTED_TOOL);
        if (selectedTool == null) {
            return null;
        }

        Object fullName = selectedTool.get(CONFIG_SELECTED_TOOL_FULL_NAME);
        if (fullName == null) {
            return null;
        }

        String fullNameText = fullName.toString();
        return fullNameText.isBlank() ? null : fullNameText;
    }

    @SuppressWarnings("unchecked")
    private List<String> validateRequiredToolInputs(
        Node node,
        Map<String, Object> resolvedInputs
    ) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> selectedTool =
            node.getConfig() != null
                ? node.getConfig().getMap(CONFIG_SELECTED_TOOL)
                : null;
        Object inputSchema =
            selectedTool != null ? selectedTool.get("inputSchemaStr") : null;
        if (inputSchema == null) {
            return errors;
        }

        try {
            Map<String, Object> schema = objectMapper.readValue(
                inputSchema.toString(),
                Map.class
            );
            Object requiredRaw = schema.get("required");
            if (!(requiredRaw instanceof List<?> requiredFields)) {
                return errors;
            }

            for (Object requiredRawField : requiredFields) {
                String field = requiredRawField != null
                    ? requiredRawField.toString()
                    : "";
                if (field.isBlank()) {
                    continue;
                }

                Object value = resolvedInputs.get(field);
                if (isBlankValue(value)) {
                    Object ref = node.getInputs() != null
                        ? node.getInputs().get(field)
                        : null;
                    errors.add(
                        "TOOL 入参解析失败：node=" +
                            node.getNodeId() +
                            " input=" +
                            field +
                            " ref=" +
                            (ref != null ? ref : "<未配置>") +
                            " reason=必填参数为空"
                    );
                }
            }
        } catch (Exception e) {
            errors.add(
                "TOOL 入参解析失败：node=" +
                    node.getNodeId() +
                    " reason=工具 inputSchema 解析失败: " +
                    e.getMessage()
            );
        }

        return errors;
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        return value instanceof String text && text.isBlank();
    }
}
