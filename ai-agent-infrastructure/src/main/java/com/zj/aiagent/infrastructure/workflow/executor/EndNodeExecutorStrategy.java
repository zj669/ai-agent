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
 * 结束节点执行策略
 * 直通执行器 - 将输入透传到输出，标记工作流结束
 */
@Slf4j
@Component
public class EndNodeExecutorStrategy implements NodeExecutorStrategy {

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node,
            Map<String, Object> resolvedInputs,
            StreamPublisher streamPublisher) {

        log.info("[End Node {}] Workflow ending, collecting outputs", node.getNodeId());

        // 透传输入作为最终输出
        Map<String, Object> outputs = new HashMap<>(resolvedInputs);
        // 移除内部使用的上下文对象
        outputs.remove("__context__");

        // 添加结束标记
        outputs.put("__workflow_ended__", true);

        return CompletableFuture.completedFuture(NodeExecutionResult.success(outputs));
    }

    @Override
    public NodeType getSupportedType() {
        return NodeType.END;
    }
}
