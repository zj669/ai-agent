# Workflow 消息持久化优化 - 设计文档

## 1. 架构设计

### 1.1 整体流程

```
用户发送消息
    ↓
WorkflowController.startExecution()
    ↓
SchedulerService.startExecution()
    ├─→ 保存用户消息到 messages 表 (NEW)
    ├─→ 初始化 Assistant 消息 (PENDING) (NEW)
    └─→ 启动 workflow 执行
        ↓
    每个节点执行完成
        ↓
    NodeCompletedEvent 监听器
        └─→ 保存到 workflow_node_execution_log 表 (已有)
        ↓
    Workflow 执行完成
        ↓
    SchedulerService.onExecutionComplete() (NEW)
        ├─→ 从 workflow_node_execution_log 查询最后一个 LLM 节点输出
        └─→ 更新 messages 表中的 Assistant 消息 (COMPLETED)
```

### 1.2 数据流向

```
┌─────────────────────────────────────────────────────────┐
│                    用户输入                              │
└────────────────────┬────────────────────────────────────┘
                     ↓
         ┌───────────────────────┐
         │   messages 表          │
         │  (USER 消息)           │
         └───────────────────────┘
                     ↓
         ┌───────────────────────┐
         │   messages 表          │
         │  (ASSISTANT PENDING)   │
         └───────────────────────┘
                     ↓
         ┌───────────────────────┐
         │  Workflow 执行         │
         └───────────┬───────────┘
                     ↓
         ┌───────────────────────┐
         │ workflow_node_        │
         │ execution_log 表      │
         │ (每个节点的执行日志)   │
         └───────────┬───────────┘
                     ↓
         ┌───────────────────────┐
         │   messages 表          │
         │  (ASSISTANT COMPLETED) │
         │  (更新最终响应)        │
         └───────────────────────┘
```

### 1.3 关键变更点

1. **SchedulerService**: 添加消息保存逻辑
2. **ChatApplicationService**: 复用现有的消息保存方法
3. **去掉 Redis**: 所有数据持久化到 MySQL
4. **查询优化**: 从 workflow_node_execution_log 提取最终响应

## 2. 详细设计

### 2.1 SchedulerService 增强

#### 2.1.1 startExecution() 方法修改

```java
private void startExecution(Execution execution, Map<String, Object> inputs,
        ExecutionMode mode) {
    log.info("[Scheduler] Starting execution: {}, mode: {}", execution.getExecutionId(), mode);

    // ========== 新增: 保存用户消息 ==========
    if (StringUtils.hasText(execution.getConversationId())) {
        String userInput = extractUserQuery(inputs);
        if (StringUtils.hasText(userInput)) {
            try {
                // 1. 保存用户消息
                Message userMessage = chatApplicationService.appendUserMessage(
                    execution.getConversationId(), 
                    userInput,
                    Map.of("executionId", execution.getExecutionId())
                );
                log.info("[Scheduler] Saved user message: {}", userMessage.getId());
                
                // 2. 初始化 Assistant 消息 (PENDING 状态)
                String assistantMessageId = chatApplicationService.initAssistantMessage(
                    execution.getConversationId(),
                    execution.getExecutionId()
                );
                
                // 3. 保存 messageId 到 execution（用于后续更新）
                execution.setAssistantMessageId(assistantMessageId);
                
                log.info("[Scheduler] Initialized assistant message: {}", assistantMessageId);
            } catch (Exception e) {
                log.error("[Scheduler] Failed to save messages: {}", e.getMessage(), e);
                // 不阻塞 workflow 执行
            }
        }
    }

    // ========== 记忆水合 (Memory Hydration) ==========
    hydrateMemory(execution, inputs);

    // 1. 启动执行，获取就绪节点
    List<Node> readyNodes = execution.start(inputs);

    // 2. 持久化初始状态
    executionRepository.save(execution);

    // 3. 调度就绪节点
    scheduleNodes(execution.getExecutionId(), readyNodes, null);
}
```

#### 2.1.2 新增 onExecutionComplete() 方法

```java
/**
 * Workflow 执行完成回调
 * 从 workflow_node_execution_log 提取最终响应，更新 messages 表
 */
private void onExecutionComplete(Execution execution) {
    log.info("[Scheduler] Execution completed: {}, status: {}", 
        execution.getExecutionId(), execution.getStatus());
    
    // 只处理成功完成的执行
    if (execution.getStatus() != ExecutionStatus.SUCCEEDED) {
        log.warn("[Scheduler] Execution {} not succeeded, skip message finalization", 
            execution.getExecutionId());
        
        // 失败的情况，更新消息状态为 FAILED
        if (StringUtils.hasText(execution.getAssistantMessageId())) {
            try {
                chatApplicationService.finalizeMessage(
                    execution.getAssistantMessageId(),
                    "执行失败: " + execution.getStatus(),
                    null,
                    MessageStatus.FAILED
                );
            } catch (Exception e) {
                log.error("[Scheduler] Failed to update failed message: {}", e.getMessage());
            }
        }
        return;
    }
    
    // 检查是否有 conversationId 和 messageId
    if (!StringUtils.hasText(execution.getConversationId()) || 
        !StringUtils.hasText(execution.getAssistantMessageId())) {
        log.debug("[Scheduler] No conversation or message to finalize for execution: {}", 
            execution.getExecutionId());
        return;
    }
    
    try {
        // 从 workflow_node_execution_log 提取最终响应
        String finalResponse = extractFinalResponseFromLogs(execution.getExecutionId());
        
        // 更新 Assistant 消息
        chatApplicationService.finalizeMessage(
            execution.getAssistantMessageId(),
            finalResponse,
            null, // thoughtProcess 从 workflow_node_execution_log 查询，不存储在 messages 表
            MessageStatus.COMPLETED
        );
        
        log.info("[Scheduler] Finalized assistant message for execution: {}", 
            execution.getExecutionId());
    } catch (Exception e) {
        log.error("[Scheduler] Failed to finalize message: {}", e.getMessage(), e);
    }
}

/**
 * 从 workflow_node_execution_log 提取最终响应
 * 从最终节点（END 节点）的输出提取
 */
private String extractFinalResponseFromLogs(String executionId) {
    // 1. 优先查询 END 节点的输出
    WorkflowNodeExecutionLog endLog = workflowNodeExecutionLogRepository
        .findByExecutionIdAndNodeId(executionId, "END");
    
    if (endLog != null && endLog.getOutputs() != null) {
        Map<String, Object> outputs = endLog.getOutputs();
        
        // 尝试提取常见的响应字段
        Object response = outputs.get("response");
        if (response == null) response = outputs.get("text");
        if (response == null) response = outputs.get("output");
        if (response == null) response = outputs.get("result");
        
        if (response != null) {
            return response.toString();
        }
    }
    
    // 2. 如果 END 节点没有输出，查询最后执行的节点
    List<WorkflowNodeExecutionLog> allLogs = workflowNodeExecutionLogRepository
        .findByExecutionIdOrderByEndTime(executionId);
    
    if (!allLogs.isEmpty()) {
        WorkflowNodeExecutionLog lastLog = allLogs.get(allLogs.size() - 1);
        Map<String, Object> outputs = lastLog.getOutputs();
        
        if (outputs != null && !outputs.isEmpty()) {
            // 尝试提取响应
            Object response = outputs.get("response");
            if (response == null) response = outputs.get("text");
            if (response == null) response = outputs.get("output");
            
            if (response != null) {
                return response.toString();
            }
        }
    }
    
    log.warn("[Scheduler] No final response found for execution: {}", executionId);
    return "执行完成";
}
```

#### 2.1.3 修改 onNodeComplete() 方法

在 `onNodeComplete()` 方法中，检测 workflow 是否完成，如果完成则调用 `onExecutionComplete()`:

```java
private void onNodeComplete(String executionId, String nodeId, String nodeName, NodeType nodeType,
        NodeExecutionResult result, Map<String, Object> inputs) {
    // ... 现有逻辑 ...
    
    // 6. Update DB
    executionRepository.update(execution);

    // 7. Publish Event (保存到 workflow_node_execution_log)
    NodeCompletedEvent logEvent = NodeCompletedEvent.builder()
            .executionId(executionId)
            .nodeId(nodeId)
            .nodeName(nodeName)
            .nodeType(nodeType.name())
            .renderMode(result.getStatus() == ExecutionStatus.SUCCEEDED ? "MESSAGE" : "HIDDEN")
            .status(result.getStatus().getCode())
            .inputs(inputs)
            .outputs(result.getOutputs())
            .errorMessage(result.getErrorMessage())
            .startTime(java.time.LocalDateTime.now())
            .endTime(java.time.LocalDateTime.now())
            .build();
    applicationEventPublisher.publishEvent(logEvent);
    
    // ========== 新增: 检查是否完成 ==========
    if (execution.getStatus() == ExecutionStatus.SUCCEEDED ||
            execution.getStatus() == ExecutionStatus.FAILED) {
        log.info("[Scheduler] Execution {} finished with status: {}", 
            executionId, execution.getStatus());
        
        // 保存最终消息
        onExecutionComplete(execution);
        return;
    }
    
    // ... 现有逻辑 ...
}
```

### 2.2 Execution 实体增强

添加 `assistantMessageId` 字段:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execution {
    private String executionId;
    private Long agentId;
    private Long userId;
    private String conversationId;
    
    // ========== 新增字段 ==========
    /**
     * 关联的 Assistant 消息 ID
     * 用于在 workflow 执行完成后更新消息内容
     */
    private String assistantMessageId;
    
    // ... 其他字段 ...
}
```

### 2.2 WorkflowNodeExecutionLogRepository 增强

添加查询方法:

```java
public interface WorkflowNodeExecutionLogRepository {
    
    // 现有方法...
    
    /**
     * 根据 executionId 和 nodeId 查询节点日志
     */
    WorkflowNodeExecutionLog findByExecutionIdAndNodeId(String executionId, String nodeId);
    
    /**
     * 查询指定 execution 的所有节点日志
     * 按 end_time 升序排序
     */
    List<WorkflowNodeExecutionLog> findByExecutionIdOrderByEndTime(String executionId);
}
```

实现类:

```java
@Override
public WorkflowNodeExecutionLog findByExecutionIdAndNodeId(String executionId, String nodeId) {
    LambdaQueryWrapper<WorkflowNodeExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(WorkflowNodeExecutionLogPO::getExecutionId, executionId)
           .eq(WorkflowNodeExecutionLogPO::getNodeId, nodeId)
           .last("LIMIT 1");
    
    WorkflowNodeExecutionLogPO po = baseMapper.selectOne(wrapper);
    return po != null ? toDomain(po) : null;
}

@Override
public List<WorkflowNodeExecutionLog> findByExecutionIdOrderByEndTime(String executionId) {
    LambdaQueryWrapper<WorkflowNodeExecutionLogPO> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(WorkflowNodeExecutionLogPO::getExecutionId, executionId)
           .orderByAsc(WorkflowNodeExecutionLogPO::getEndTime);
    
    List<WorkflowNodeExecutionLogPO> pos = baseMapper.selectList(wrapper);
    return pos.stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
}
```

### 2.4 数据库设计说明

**重要架构决策：**

1. **不使用 MySQL 存储 Execution**
   - `Execution` 实体存储在 Redis（热数据，48 小时 TTL）
   - 使用 Jackson 序列化，自动处理 `assistantMessageId` 字段
   - 无需创建 `workflow_execution` MySQL 表

2. **workflow_node_execution_log 是唯一的持久化表**
   - 存储所有节点的执行日志
   - 可以通过聚合查询得到执行级别的信息
   - 避免数据冗余和不一致

3. **数据关联**
   - `messages.metadata.executionId` → `workflow_node_execution_log.execution_id`
   - 通过 `executionId` 关联对话消息和思维链

### 2.5 去掉 Redis 依赖

**当前使用 Redis 的地方：**
1. ~~SSE 流式推送~~ - 保留（用于实时推送）
2. ~~执行取消标记~~ - 改用 MySQL 的 workflow_execution.status 字段
3. ~~人工审核待处理队列~~ - 改用 MySQL 查询

**优化方案：**

#### 2.5.1 执行取消

**当前实现：**
```java
public void cancelExecution(String executionId) {
    redisTemplate.opsForValue().set(CANCEL_KEY_PREFIX + executionId, "true", 1, TimeUnit.HOURS);
}

private boolean isCancelled(String executionId) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(CANCEL_KEY_PREFIX + executionId));
}
```

**优化后：**
```java
public void cancelExecution(String executionId) {
    Execution execution = executionRepository.findById(executionId)
        .orElseThrow(() -> new IllegalArgumentException("Execution not found"));
    
    execution.cancel(); // 设置状态为 CANCELLED
    executionRepository.update(execution);
}

private boolean isCancelled(String executionId) {
    return executionRepository.findById(executionId)
        .map(e -> e.getStatus() == ExecutionStatus.CANCELLED)
        .orElse(false);
}
```

#### 2.5.2 人工审核待处理队列

**当前实现：**
```java
// Add to Pending Review Set
RSet<String> pendingSet = redissonClient.getSet("human_review:pending");
pendingSet.add(executionId);
```

**优化后：**
```java
// 直接查询数据库
public List<Execution> getPendingReviews() {
    return executionRepository.findByStatus(ExecutionStatus.PAUSED);
}
```

## 3. 时序图

### 3.1 消息保存流程

```
用户 -> WorkflowController: POST /api/workflow/execution/start
WorkflowController -> SchedulerService: startExecution()
SchedulerService -> ChatApplicationService: appendUserMessage()
ChatApplicationService -> ConversationRepository: saveMessage(USER)
SchedulerService -> ChatApplicationService: initAssistantMessage()
ChatApplicationService -> ConversationRepository: saveMessage(ASSISTANT, PENDING)
SchedulerService -> WorkflowEngine: execute()
WorkflowEngine -> SchedulerService: onNodeComplete()
SchedulerService -> EventPublisher: publishEvent(NodeCompletedEvent)
EventPublisher -> NodeExecutionLogListener: onNodeCompleted()
NodeExecutionLogListener -> WorkflowNodeExecutionLogRepository: save()
SchedulerService -> SchedulerService: onExecutionComplete()
SchedulerService -> WorkflowNodeExecutionLogRepository: findByExecutionIdAndNodeType()
SchedulerService -> ChatApplicationService: finalizeMessage()
ChatApplicationService -> ConversationRepository: updateMessage(ASSISTANT, COMPLETED)
```

## 4. API 设计

### 4.1 查询对话历史

```
GET /api/chat/conversations/{conversationId}/messages?page=1&size=50
```

**响应：**
```json
[
  {
    "id": "msg-001",
    "role": "USER",
    "content": "你好",
    "createdAt": "2026-02-03T10:00:00",
    "metadata": {
      "executionId": "exec-001"
    }
  },
  {
    "id": "msg-002",
    "role": "ASSISTANT",
    "content": "你好！有什么可以帮助你的吗？",
    "status": "COMPLETED",
    "createdAt": "2026-02-03T10:00:05",
    "metadata": {
      "executionId": "exec-001"
    }
  }
]
```

### 4.2 查询思维链

```
GET /api/workflow/execution/{executionId}/logs
```

**响应：**
```json
[
  {
    "nodeId": "node-1",
    "nodeName": "意图识别",
    "nodeType": "LLM",
    "status": "SUCCEEDED",
    "inputs": { "input": "你好" },
    "outputs": { "intent": "greeting" },
    "startTime": "2026-02-03T10:00:01",
    "endTime": "2026-02-03T10:00:03"
  },
  {
    "nodeId": "node-2",
    "nodeName": "生成回复",
    "nodeType": "LLM",
    "status": "SUCCEEDED",
    "inputs": { "intent": "greeting" },
    "outputs": { "response": "你好！有什么可以帮助你的吗？" },
    "startTime": "2026-02-03T10:00:03",
    "endTime": "2026-02-03T10:00:05"
  }
]
```

## 5. 错误处理

### 5.1 消息保存失败
- 不阻塞 workflow 执行
- 记录错误日志
- 继续执行 workflow

### 5.2 消息更新失败
- 记录错误日志
- 消息保持 PENDING 状态
- 可通过后台任务修复

### 5.3 缺少 conversationId
- 跳过消息保存
- 记录警告日志
- Workflow 正常执行

### 5.4 查询日志失败
- 返回默认响应 "执行完成"
- 记录错误日志

## 6. 性能优化

### 6.1 索引优化

```sql
-- messages 表
CREATE INDEX idx_conversation_created ON messages(conversation_id, created_at);

-- workflow_node_execution_log 表
CREATE INDEX idx_execution_type_time ON workflow_node_execution_log(execution_id, node_type, end_time);
```

### 6.2 查询优化

- 使用分页查询避免一次加载过多数据
- 只查询必要的字段
- 使用索引加速查询

### 6.3 事务优化

- 消息保存使用独立事务
- 不阻塞 workflow 执行主流程

## 7. 监控和日志

### 7.1 关键日志
- 消息保存成功/失败
- Workflow 执行完成
- 消息更新状态
- 从日志提取响应

### 7.2 指标监控
- 消息保存成功率
- 消息保存耗时
- PENDING 状态消息数量
- 日志查询耗时

## 8. 兼容性

### 8.1 向后兼容
- 现有的直接聊天功能不受影响
- API 接口保持不变
- 数据库字段可为空，兼容旧数据

### 8.2 渐进式迁移
- 新执行自动保存消息
- 旧执行数据不受影响

## 9. 未来优化

### 9.1 实时流式更新
- 在流式输出过程中实时更新消息内容
- 使用 WebSocket 推送更新

### 9.2 思维链可视化增强
- 前端展示更丰富的思维过程
- 支持节点间的依赖关系可视化

### 9.3 消息引用
- 保存知识库引用 (citations)
- 支持溯源查询
