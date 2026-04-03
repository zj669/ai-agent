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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

    private final IMcpToolRegistry mcpToolRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        String mcpToolName = node.getConfig() != null ? node.getConfig().getString("mcpToolName") : null;
        String executionId = "node-" + node.getNodeId() + "-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("[Tool Node {}] Executing MCP tool: {}, executionId={}",
                node.getNodeId(), mcpToolName, executionId);

        if (mcpToolName == null || mcpToolName.isBlank()) {
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("error", "No mcpToolName configured");
            return CompletableFuture.completedFuture(
                    NodeExecutionResult.failed("mcpToolName not configured"));
        }

        // 解析格式: mcp__{serverId}__{toolName}
        Long serverId = McpToolDefinition.parseServerId(mcpToolName);
        String toolName = McpToolDefinition.parseToolName(mcpToolName);

        if (serverId == null || toolName == null) {
            log.error("[Tool Node {}] Invalid mcpToolName format: {}", node.getNodeId(), mcpToolName);
            return CompletableFuture.completedFuture(
                    NodeExecutionResult.failed("Invalid mcpToolName format: " + mcpToolName));
        }

        // 从 resolvedInputs 提取工具参数
        Map<String, Object> toolArgs = new HashMap<>(resolvedInputs);

        return mcpToolRegistry.execute(serverId, toolName, toolArgs, executionId)
                .thenApply(result -> {
                    if (result.isAborted()) {
                        log.info("[Tool Node {}] Tool execution aborted executionId={}",
                                node.getNodeId(), executionId);
                        Map<String, Object> outputs = new HashMap<>();
                        outputs.put("aborted", true);
                        return NodeExecutionResult.success(outputs);
                    }

                    if (!result.isSuccess()) {
                        log.error("[Tool Node {}] Tool execution failed: {}",
                                node.getNodeId(), result.getErrorMessage());
                        return NodeExecutionResult.failed(result.getErrorMessage());
                    }

                    Map<String, Object> outputs = new HashMap<>();
                    outputs.put("result", result.getContent());
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
}
