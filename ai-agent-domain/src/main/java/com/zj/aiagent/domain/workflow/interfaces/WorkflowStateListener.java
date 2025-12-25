package com.zj.aiagent.domain.workflow.interfaces;

import com.zj.aiagent.domain.workflow.base.WorkflowState;

public interface WorkflowStateListener {
    // 节点开始执行
    void onNodeStarted(String nodeId, String nodeName);
    
    // 节点产生流式内容 (比如 LLM 吐字)
    void onNodeStreaming(String nodeId, String contentChunk);
    
    // 节点执行完成
    void onNodeCompleted(String nodeId, WorkflowState result);
    
    // 整个 DAG 失败
    void onWorkflowFailed(Throwable t);
}