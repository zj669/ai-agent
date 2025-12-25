package com.zj.aiagent.infrastructure.listener;

import com.zj.aiagent.domain.workflow.base.WorkflowState;
import com.zj.aiagent.domain.workflow.interfaces.WorkflowStateListener;
import lombok.AllArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

@AllArgsConstructor
public class SSEWorkflowStateListener implements WorkflowStateListener {
    private ResponseBodyEmitter emitter;

    @Override
    public void onNodeStarted(String nodeId, String nodeName) {

    }

    @Override
    public void onNodeStreaming(String nodeId, String contentChunk) {

    }

    @Override
    public void onNodeCompleted(String nodeId, WorkflowState result) {

    }

    @Override
    public void onWorkflowFailed(Throwable t) {

    }
}
