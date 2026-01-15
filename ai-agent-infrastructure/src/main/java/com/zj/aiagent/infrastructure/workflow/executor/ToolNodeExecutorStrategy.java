package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MCP 工具节点执行策略
 * TODO: 后续集成 MCP (Model Context Protocol) 工具调用
 */
@Slf4j
@Component
public class ToolNodeExecutorStrategy implements NodeExecutorStrategy {

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        log.info("[Tool Node {}] Executing tool: {}", node.getNodeId(), node.getName());

        // TODO: 实现 MCP 工具调用
        // 1. 从 node.getConfig() 获取工具名称和参数
        // 2. 调用 MCP 客户端执行工具
        // 3. 解析返回结果

        // 临时实现：返回模拟结果
        Map<String, Object> outputs = new HashMap<>();
        outputs.put("toolName", node.getName());
        outputs.put("status", "success");
        outputs.put("result", "Tool execution placeholder - MCP integration pending");

        log.warn("[Tool Node {}] MCP integration not yet implemented, returning placeholder result",
                node.getNodeId());

        return CompletableFuture.completedFuture(NodeExecutionResult.success(outputs));
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.TOOL;
    }
}
