package com.zj.aiagent.domain.workflow.port;

import com.zj.aiagent.domain.workflow.entity.Node;
import com.zj.aiagent.domain.workflow.valobj.NodeExecutionResult;
import com.zj.aiagent.domain.workflow.valobj.NodeType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 节点执行策略接口（端口）
 * 采用策略模式，将调度与执行解耦
 * 
 * 实现类位于 Infrastructure 层
 */
public interface NodeExecutorStrategy {

    /**
     * 异步执行节点逻辑
     * 
     * @param node           节点定义
     * @param resolvedInputs 已解析的输入参数（SpEL 已替换为实际值）
     * @return 包含执行结果的 CompletableFuture
     */
    CompletableFuture<NodeExecutionResult> executeAsync(Node node, Map<String, Object> resolvedInputs);

    /**
     * 获取支持的节点类型
     * 
     * @return 节点类型
     */
    NodeType getSupportedType();

    /**
     * 是否支持流式输出
     * 
     * @return true 表示支持 SSE 流式推送
     */
    default boolean supportsStreaming() {
        return false;
    }
}
