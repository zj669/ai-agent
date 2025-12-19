package com.zj.aiagemt.common.design.dag;

/**
 * DAG节点执行异常
 */
public class DagNodeExecutionException extends Exception {

    private final String nodeId;
    private final boolean retryable;

    public DagNodeExecutionException(String message) {
        this(message, null, null, true);
    }

    public DagNodeExecutionException(String message, Throwable cause) {
        this(message, cause, null, true);
    }

    public DagNodeExecutionException(String message, Throwable cause, String nodeId, boolean retryable) {
        super(message, cause);
        this.nodeId = nodeId;
        this.retryable = retryable;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
