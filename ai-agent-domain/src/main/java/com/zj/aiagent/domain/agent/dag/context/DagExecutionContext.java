package com.zj.aiagent.domain.agent.dag.context;


import com.zj.aiagent.shared.design.dag.DagContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG执行上下文实现
 */
public class DagExecutionContext implements DagContext {
    @Getter
    @Setter
    private ResponseBodyEmitter emitter;
    @Getter
    @Setter
    private Long agentId;
    private final String executionId;
    private final String conversationId;
    private final Map<String, Object> dataMap;
    private final Map<String, Object> nodeResults;

    public DagExecutionContext(String conversationId, ResponseBodyEmitter emitter, Long agentId) {
        this.emitter = emitter;
        this.agentId = agentId;
        this.executionId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.dataMap = new ConcurrentHashMap<>();
        this.nodeResults = new ConcurrentHashMap<>();
    }

    public DagExecutionContext(String conversationId) {
        this.emitter = null;
        this.executionId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.dataMap = new ConcurrentHashMap<>();
        this.nodeResults = new ConcurrentHashMap<>();
    }

    @Override
    public <T> void setValue(String key, T value) {
        dataMap.put(key, value);
    }

    @Override
    public <T> T getValue(String key) {
        return (T) dataMap.get(key);
    }

    @Override
    public <T> T getValue(String key, T defaultValue) {
        T value = getValue(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public <R> void setNodeResult(String nodeId, R result) {
        nodeResults.put(nodeId, result);
    }

    @Override
    public <R> R getNodeResult(String nodeId) {
        return (R) nodeResults.get(nodeId);
    }

    @Override
    public Map<String, Object> getAllNodeResults() {
        return new ConcurrentHashMap<>(nodeResults);
    }

    @Override
    public boolean isNodeExecuted(String nodeId) {
        return nodeResults.containsKey(nodeId);
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

}
