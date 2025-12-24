package com.zj.aiagent.domain.agent.dag.exception;

/**
 * DAG 执行取消异常
 * 当检测到用户取消执行时抛出此异常
 *
 * @author zj
 * @since 2025-12-24
 */
public class DagCancelledException extends RuntimeException {
    private final String nodeId;
    private final String executionId;

    public DagCancelledException(String message, String nodeId, String executionId) {
        super(message);
        this.nodeId = nodeId;
        this.executionId = executionId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getExecutionId() {
        return executionId;
    }
}
