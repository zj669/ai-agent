package com.zj.aiagent.domain.agent.dag.context;

import com.zj.aiagent.domain.agent.dag.entity.DagGraph;
import com.zj.aiagent.shared.design.dag.DagContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DAG执行上下文实现
 */
@Slf4j
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

    // ==================== 循环状态管理 ====================

    /**
     * 节点执行次数跟踪 (nodeId -> 执行次数)
     * 用于防止无限循环
     */
    private final Map<String, AtomicInteger> nodeExecutionCounts;

    /**
     * 最大循环次数限制
     * 单个节点在循环中最多可执行的次数
     */
    @Setter
    private int maxLoopIterations = 10;

    /**
     * 消息历史 (Reducer 累积模式)
     * 用于保存多轮对话历史
     */
    @Getter
    private final List<ChatMessage> messageHistory;

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

    // ==================== 取消执行管理 ====================

    /**
     * 取消标志（volatile 保证多线程可见性）
     * 当用户取消执行或客户端断开连接时设置为 true
     */
    private volatile boolean cancelled = false;

    /**
     * 标记执行为已取消
     */
    public void cancel() {
        this.cancelled = true;
        log.info("DAG execution cancelled: executionId={}, conversationId={}",
                executionId, conversationId);
    }

    /**
     * 检查是否已取消
     * 
     * @return true 如果执行已被取消
     */
    public boolean isCancelled() {
        return cancelled;
    }

    public DagExecutionContext(String conversationId, ResponseBodyEmitter emitter, Long agentId) {
        this.emitter = emitter;
        this.agentId = agentId;
        this.executionId = UUID.randomUUID().toString();
        this.conversationId = conversationId;
        this.dataMap = new ConcurrentHashMap<>();
        this.nodeResults = new ConcurrentHashMap<>();

        // 初始化循环状态管理
        this.nodeExecutionCounts = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();

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

        // 初始化循环状态管理
        this.nodeExecutionCounts = new ConcurrentHashMap<>();
        this.messageHistory = new ArrayList<>();

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

    // ==================== 循环状态管理方法 ====================

    /**
     * 检查节点是否可以继续循环执行
     * 
     * @param nodeId 节点ID
     * @return true 如果节点执行次数未超过限制
     */
    public boolean canExecuteNode(String nodeId) {
        AtomicInteger count = nodeExecutionCounts.computeIfAbsent(nodeId, k -> new AtomicInteger(0));
        return count.get() < maxLoopIterations;
    }

    /**
     * 增加节点执行计数
     * 
     * @param nodeId 节点ID
     * @return 增加后的执行次数
     */
    public int incrementNodeExecutionCount(String nodeId) {
        return nodeExecutionCounts.computeIfAbsent(nodeId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 获取节点执行次数
     * 
     * @param nodeId 节点ID
     * @return 执行次数，如果节点未执行过则返回 0
     */
    public int getNodeExecutionCount(String nodeId) {
        AtomicInteger count = nodeExecutionCounts.get(nodeId);
        return count != null ? count.get() : 0;
    }

    /**
     * 添加消息到历史 (Reducer 模式)
     * 
     * @param message 聊天消息
     */
    public void addMessage(ChatMessage message) {
        messageHistory.add(message);
    }

    /**
     * 添加消息到历史 (便捷方法)
     * 
     * @param role    角色
     * @param content 内容
     */
    public void addMessage(String role, String content) {
        messageHistory.add(new ChatMessage(role, content));
    }

    /**
     * 添加消息到历史 (带来源节点)
     * 
     * @param role         角色
     * @param content      内容
     * @param sourceNodeId 来源节点ID
     */
    public void addMessage(String role, String content, String sourceNodeId) {
        messageHistory.add(new ChatMessage(role, content, sourceNodeId));
    }

    /**
     * 获取最大循环次数
     * 
     * @return 最大循环次数
     */
    public int getMaxLoopIterations() {
        return maxLoopIterations;
    }

    /**
     * 获取数据映射表（用于快照）
     * 注意：返回副本以防止外部修改
     * 
     * @return 数据映射表的副本
     */
    public Map<String, Object> getDataMap() {
        return new ConcurrentHashMap<>(dataMap);
    }
}
