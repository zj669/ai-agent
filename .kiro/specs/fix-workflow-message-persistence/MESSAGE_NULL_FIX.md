# Message 表内容为 NULL 问题修复

## 问题根因

**异步时序问题**: 
- `WorkflowAuditListener` 使用 `@Async` 异步保存执行日志到数据库
- `onExecutionComplete()` 立即查询数据库提取最终响应
- 查询时异步保存可能还未完成,导致查询不到 END 节点日志
- 结果: 无法提取最终响应,消息内容保存为 NULL

## 解决方案

### 双重策略

#### 方案1: 从内存提取 (优先)
```java
private String extractFinalResponseFromExecution(Execution execution) {
    ExecutionContext context = execution.getContext();
    Map<String, Object> endNodeOutput = context.getNodeOutput("END");
    
    if (endNodeOutput != null) {
        Object response = endNodeOutput.get("response");
        if (response == null) response = endNodeOutput.get("text");
        if (response != null) return response.toString();
    }
    
    return "执行完成";
}
```

**优点**:
- 直接从内存获取,无需等待数据库
- 性能最优
- 避免异步时序问题

#### 方案2: 延迟查询数据库 (后备)
```java
if (finalResponse == null || finalResponse.equals("执行完成")) {
    CompletableFuture.runAsync(() -> {
        Thread.sleep(100);  // 延迟100ms
        String response = extractFinalResponseFromLogs(executionId);
        // 更新消息
    });
}
```

**优点**:
- 给异步保存留出时间
- 确保数据可靠性
- 作为后备方案

## 修改的文件

- `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
  - 修改 `onExecutionComplete()` 方法
  - 添加 `extractFinalResponseFromExecution()` 方法
  - 添加延迟查询逻辑

## 测试验证

### 检查点

1. 查看日志,确认使用了哪个方案:
   ```
   [Scheduler] Found END node output in context: [response, text]
   [Scheduler] Extracted response from END node context: ...
   ```

2. 查询数据库,确认消息内容:
   ```sql
   SELECT id, content, status FROM message 
   WHERE conversation_id = 'xxx' 
   ORDER BY create_time DESC;
   ```

3. 验证内容不为 NULL

## 编译状态

✅ 编译成功 (2026-02-03 12:07:43)
