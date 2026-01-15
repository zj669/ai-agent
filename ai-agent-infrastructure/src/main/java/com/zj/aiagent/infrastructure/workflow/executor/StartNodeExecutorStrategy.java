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
 * 开始节点执行策略
 * 直通执行器 - 将输入透传到输出
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

        // 直接透传输入作为输出
        Map<String, Object> outputs = new HashMap<>(resolvedInputs);
        // 移除内部使用的上下文对象
        outputs.remove("__context__");

        return CompletableFuture.completedFuture(NodeExecutionResult.success(outputs));
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.START;
    }
}
