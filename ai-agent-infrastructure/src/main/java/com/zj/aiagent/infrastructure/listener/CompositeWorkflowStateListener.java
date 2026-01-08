package com.zj.aiagent.infrastructure.listener;

import com.zj.aiagent.shared.design.workflow.WorkflowState;
import com.zj.aiagent.shared.design.workflow.WorkflowStateListener;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 复合工作流状态监听器
 * <p>
 * 将多个监听器组合在一起，统一分发事件
 * <p>
 * 使用场景：同时支持 SSE 推送和历史记录保存
 */
@Slf4j
public class CompositeWorkflowStateListener implements WorkflowStateListener {

    private final List<WorkflowStateListener> listeners = new ArrayList<>();

    public CompositeWorkflowStateListener(WorkflowStateListener... listeners) {
        if (listeners != null) {
            for (WorkflowStateListener listener : listeners) {
                if (listener != null) {
                    this.listeners.add(listener);
                }
            }
        }
    }

    /**
     * 添加监听器
     */
    public CompositeWorkflowStateListener addListener(WorkflowStateListener listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
        return this;
    }

    @Override
    public void onWorkflowStarted(int totalNodes) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onWorkflowStarted(totalNodes);
            } catch (Exception e) {
                log.error("监听器执行失败 [onWorkflowStarted]: listener={}",
                        listener.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void onWorkflowCompleted(boolean success) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onWorkflowCompleted(success);
            } catch (Exception e) {
                log.error("监听器执行失败 [onWorkflowCompleted]: listener={}",
                        listener.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void onWorkflowFailed(Throwable t) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onWorkflowFailed(t);
            } catch (Exception e) {
                log.error("监听器执行失败 [onWorkflowFailed]: listener={}",
                        listener.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void onNodeStarted(String nodeId, String nodeName) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onNodeStarted(nodeId, nodeName);
            } catch (Exception e) {
                log.error("监听器执行失败 [onNodeStarted]: listener={}, nodeId={}",
                        listener.getClass().getSimpleName(), nodeId, e);
            }
        }
    }

    @Override
    public void onNodeStreaming(String nodeId, String nodeName, String contentChunk) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onNodeStreaming(nodeId, nodeName, contentChunk);
            } catch (Exception e) {
                log.error("监听器执行失败 [onNodeStreaming]: listener={}, nodeId={}",
                        listener.getClass().getSimpleName(), nodeId, e);
            }
        }
    }

    @Override
    public void onNodeCompleted(String nodeId, String nodeName, WorkflowState result, long durationMs) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onNodeCompleted(nodeId, nodeName, result, durationMs);
            } catch (Exception e) {
                log.error("监听器执行失败 [onNodeCompleted]: listener={}, nodeId={}",
                        listener.getClass().getSimpleName(), nodeId, e);
            }
        }
    }

    @Override
    public void onNodeFailed(String nodeId, String nodeName, String error, long durationMs) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onNodeFailed(nodeId, nodeName, error, durationMs);
            } catch (Exception e) {
                log.error("监听器执行失败 [onNodeFailed]: listener={}, nodeId={}",
                        listener.getClass().getSimpleName(), nodeId, e);
            }
        }
    }

    @Override
    public void onNodePaused(String nodeId, String nodeName, String message) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onNodePaused(nodeId, nodeName, message);
            } catch (Exception e) {
                log.error("监听器执行失败 [onNodePaused]: listener={}, nodeId={}",
                        listener.getClass().getSimpleName(), nodeId, e);
            }
        }
    }

    @Override
    public void onFinalAnswer(String contentChunk) {
        for (WorkflowStateListener listener : listeners) {
            try {
                listener.onFinalAnswer(contentChunk);
            } catch (Exception e) {
                log.error("监听器执行失败 [onFinalAnswer]: listener={}",
                        listener.getClass().getSimpleName(), e);
            }
        }
    }
}
