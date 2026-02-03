# 后端 RenderMode 修复 - 只有最终节点输出显示在聊天消息中

## 问题描述

之前所有 LLM 节点的输出都会显示在聊天消息正文中,导致中间节点的思考过程也被当作最终回复显示。

## 解决方案

### 核心机制

通过 `StreamContext.isFinalOutputNode` 标记来区分节点类型:
- **最终输出节点** (`isFinalOutputNode = true`): `renderMode = MARKDOWN`,显示在聊天消息中
- **中间节点** (`isFinalOutputNode = false`): `renderMode = THOUGHT`,显示在思维链中

### 实现细节

#### 1. Domain 层 - 添加标记字段

**文件**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/StreamContext.java`

```java
/**
 * 是否为最终输出节点
 * true: 输出显示在聊天消息中 (renderMode = MARKDOWN)
 * false: 输出显示在思维链中 (renderMode = THOUGHT)
 */
private boolean isFinalOutputNode;
```

#### 2. Domain 层 - 扩展接口

**文件**: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java`

```java
/**
 * 推送增量文本（打字机效果）- 支持指定是否为思考过程
 * 
 * @param delta 增量内容
 * @param isThought 是否为思考过程
 */
void publishDelta(String delta, boolean isThought);
```

#### 3. Infrastructure 层 - 实现逻辑

**文件**: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java`

```java
@Override
public void publishDelta(String delta) {
    // 根据节点是否为最终输出节点决定 renderMode
    // 最终输出节点: MARKDOWN (显示在聊天消息中)
    // 中间节点: THOUGHT (显示在思维链中)
    publishDelta(delta, !context.isFinalOutputNode());
}

@Override
public void publishDelta(String delta, boolean isThought) {
    if (delta == null || delta.isEmpty()) {
        return;
    }
    log.trace("[Stream] Publishing delta for node: {}, length: {}, isThought: {}",
            context.getNodeId(), delta.length(), isThought);
    publish(SseEventType.UPDATE, ExecutionStatus.RUNNING, null, delta, isThought);
}
```

#### 4. Application 层 - 设置标记

**文件**: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`

```java
private void scheduleNode(String executionId, Node node, String parentId) {
    // 判断是否为最终输出节点
    // 1. END 节点始终是最终输出节点
    // 2. 其他节点默认不是最终输出节点(输出显示在思维链中)
    boolean isFinalOutputNode = "END".equals(node.getNodeId());

    StreamContext streamContext = StreamContext.builder()
            .executionId(executionId)
            .nodeId(node.getNodeId())
            .parentId(parentId)
            .nodeType(node.getType() != null ? node.getType().name() : "UNKNOWN")
            .nodeName(node.getName())
            .isFinalOutputNode(isFinalOutputNode)  // 设置标记
            .build();
    
    StreamPublisher streamPublisher = streamPublisherFactory.create(streamContext);
    // ...
}
```

## 判断规则

### 当前实现
- **END 节点**: `isFinalOutputNode = true` → `renderMode = MARKDOWN`
- **其他所有节点**: `isFinalOutputNode = false` → `renderMode = THOUGHT`

### 未来扩展
如果需要支持更复杂的判断逻辑,可以在 `scheduleNode` 方法中添加:

```java
boolean isFinalOutputNode = false;

// 1. END 节点始终是最终输出节点
if ("END".equals(node.getNodeId())) {
    isFinalOutputNode = true;
}

// 2. 或者通过节点配置指定
else if (node.getConfig().getBoolean("isFinalOutput", false)) {
    isFinalOutputNode = true;
}

// 3. 或者判断是否为工作流的最后一个 LLM 节点
else if (isLastLlmNode(execution, node)) {
    isFinalOutputNode = true;
}
```

## RenderMode 完整说明

| RenderMode | 用途 | 显示位置 | 触发条件 |
|-----------|------|---------|---------|
| `MARKDOWN` | 最终输出 | 聊天消息正文 | `isFinalOutputNode = true` |
| `THOUGHT` | 思考过程 | 思维链 | `isFinalOutputNode = false` |
| `HIDDEN` | 隐藏输出 | 不显示 | 特殊节点 |
| `TEXT` | 纯文本 | 错误消息等 | 错误处理 |
| `JSON_EVENT` | JSON 事件 | 系统事件 | 工作流暂停等 |

## 数据流

### 中间节点 (例如: 思考节点)
```
LLM 节点执行
  ↓
streamPublisher.publishDelta(chunk)
  ↓
isFinalOutputNode = false
  ↓
isThought = true
  ↓
renderMode = "THOUGHT"
  ↓
前端: 显示在思维链中 ✅
```

### 最终输出节点 (END 节点)
```
END 节点执行
  ↓
streamPublisher.publishDelta(chunk)
  ↓
isFinalOutputNode = true
  ↓
isThought = false
  ↓
renderMode = "MARKDOWN"
  ↓
前端: 显示在聊天消息正文中 ✅
```

## 前端配合

前端已经修改为只处理 `renderMode === 'MARKDOWN'` 的内容作为聊天消息:

```typescript
// app/frontend/src/features/chat/hooks/useChat.ts
if (data.nodeType === 'LLM' && dataPayload.renderMode === 'MARKDOWN') {
    // 只有 MARKDOWN 模式的内容才更新聊天消息
    setMessages(prev => [...prev, assistantMessage]);
}
```

## 测试建议

### 功能测试
1. **单节点工作流**: 验证 END 节点输出显示在聊天消息中
2. **多节点工作流**: 验证中间 LLM 节点输出显示在思维链中
3. **复杂工作流**: 验证思考节点 → 最终输出节点的完整流程

### 验证点
- ✅ 中间节点的输出只在思维链中显示
- ✅ END 节点的输出在聊天消息正文中显示
- ✅ 思维链正确显示所有节点的执行过程
- ✅ 聊天消息正文只显示最终回答

## 修改的文件

1. `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/StreamContext.java`
2. `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java`
3. `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java`
4. `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`

## 编译状态

✅ 所有模块编译成功
✅ 无编译错误
✅ 无警告(除了已知的 deprecation 和 unchecked 警告)

---

**实现日期**: 2026-02-03
**实现人员**: Kiro AI Assistant
