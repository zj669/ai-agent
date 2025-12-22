package com.zj.aiagent.domain.agent.dag.context;

import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
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

    /**
     * 执行实例ID
     */
    private Long instanceId;

    private final String executionId;
    private final String conversationId;
    private final Map<String, Object> dataMap;
    private final Map<String, Object> nodeResults;

    // ==================== 领域对象 ====================
    /** 用户输入数据 */
    @Getter
    private final UserInputData userInputData;

    /** 执行阶段数据 */
    @Getter
    private final ExecutionData executionData;

    /** 人工介入数据 */
    @Getter
    private final HumanInterventionData humanInterventionData;

    /** 进度数据 */
    @Getter
    private ProgressData progressData;

    /** DAG 图对象 */
    @Getter
    @Setter
    private DagGraph dagGraph;

    public DagExecutionContext(String conversationId, ResponseBodyEmitter emitter, Long agentId) {
        this.emitter = emitter;
        this.agentId = agentId;
        this.executionId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.dataMap = new ConcurrentHashMap<>();
        this.nodeResults = new ConcurrentHashMap<>();
        // 初始化领域对象
        this.userInputData = new UserInputData();
        this.executionData = new ExecutionData();
        this.humanInterventionData = new HumanInterventionData();
    }

    public DagExecutionContext(String conversationId) {
        this.emitter = null;
        this.executionId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.dataMap = new ConcurrentHashMap<>();
        this.nodeResults = new ConcurrentHashMap<>();
        // 初始化领域对象
        this.userInputData = new UserInputData();
        this.executionData = new ExecutionData();
        this.humanInterventionData = new HumanInterventionData();
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

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    // ==================== 领域对象便捷方法 ====================

    /**
     * 初始化进度数据
     */
    public void initProgress(int totalNodes) {
        this.progressData = new ProgressData(0, totalNodes);
    }

    /**
     * 获取有效的用户输入
     */
    public String getEffectiveUserInput() {
        return userInputData.getEffectiveInput();
    }

    /**
     * 设置用户输入
     */
    public void setUserInput(String input) {
        userInputData.setUserInput(input);
    }

    /**
     * 检查是否等待人工介入
     */
    public boolean isWaitingForHuman() {
        return humanInterventionData.isWaitingForHuman();
    }
}
