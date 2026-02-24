package com.zj.aiagent.infrastructure.workflow.executor;

import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.port.NodeExecutorStrategy;
import com.zj.aiagent.domain.workflow.port.StreamPublisher;
import com.zj.aiagent.domain.workflow.valobj.ExecutionContext;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 开始节点执行策略
 * 将全局输入（用户消息等）透传到输出，供下游节点通过 sourceRef 引用
 */
@Slf4j
@Component
public class StartNodeExecutorStrategy implements NodeExecutorStrategy {

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        log.info("[Start Node {}] Passing through inputs", node.getNodeId());

        // 从注入的 __context__ 中获取全局输入（用户消息等），合并到输出
        Map<String, Object> outputs = new HashMap<>();

        ExecutionContext context = (ExecutionContext) resolvedInputs.get("__context__");
        if (context != null && context.getInputs() != null) {
            outputs.putAll(context.getInputs());
        }

        // 再合并 resolvedInputs 中的非系统字段
        for (Map.Entry<String, Object> entry : resolvedInputs.entrySet()) {
            if (!entry.getKey().startsWith("__")) {
                outputs.put(entry.getKey(), entry.getValue());
            }
        }

        return CompletableFuture.completedFuture(NodeExecutionResult.success(outputs));
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.START;
    }
}
