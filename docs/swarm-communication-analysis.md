# Swarm 多智能体通信架构分析

> 创建时间：2026-03-06
> 目的：分析「初始 Agent 派发任务为什么只有一个 Agent 收到消息」的根因

---

## 一、当前通信架构

### 1.1 群组模型

系统中 Agent 间通信基于**群组消息**，不是直接 P2P socket。

| 群组类型 | 成员 | 创建时机 |
|----------|------|----------|
| 初始 P2P 群 | human + assistant（Root Agent） | 创建 Workspace 时自动创建 |
| 任务三方群 | parent + 子 Agent + human | Root Agent 调用 `createAgent` 时自动创建 |

每个子 Agent 都有一个独立的三方群，Root Agent 通过不同的群向不同的子 Agent 发消息。

### 1.2 消息发送流程

```
Root Agent 调用 send(agentId, message)
  ↓
SwarmTools.send() 查找包含 caller 和 target 的群
  ↓
SwarmMessageService.sendMessage(groupId, req)
  ↓
保存消息到数据库 + 发布 SwarmMessageSentEvent
  ↓
SwarmMessageEventListener.onMessageSent（异步）
  ↓
遍历群成员（排除发送者），逐个 runtimeService.wakeAgent(memberId)
  ↓
目标 Agent 被唤醒 → processTurn()
```

### 1.3 Agent 唤醒机制

```java
// SwarmMessageEventListener.java
@Async
@EventListener
public void onMessageSent(SwarmMessageSentEvent event) {
    List<Long> memberIds = groupRepository.findMemberIds(event.getGroupId());
    for (Long memberId : memberIds) {
        if (!memberId.equals(event.getSenderId())) {
            runtimeService.wakeAgent(memberId);
        }
    }
}
```

每条消息发送后，群内所有非发送者成员都会被唤醒。

---

## 二、问题根因：为什么只有一个 Agent 收到消息

### 2.1 核心限制：`send` 后立即 break

在 `SwarmAgentRunner.processHumanMessages` 中：

```java
while (round < maxRoundsPerTurn && running) {
    // ... LLM 调用 ...
    
    if (response.hasToolCalls()) {
        for (AssistantMessage.ToolCall toolCall : response.getToolCalls()) {
            // ...
            String result = executeToolCall(toolName, toolArgs);
            
            if ("send".equals(toolName)) {
                break;  // ← 执行第一个 send 后立即跳出工具循环
            }
            // 非 send 工具继续执行
        }
        
        // 如果有 send，跳出外层 while 循环
        if (response.getToolCalls().stream().anyMatch(tc -> "send".equals(tc.name()))) {
            break;  // ← 跳出整个推理循环，等待被唤醒
        }
    }
}
```

**这意味着**：
1. Root Agent 每轮推理只能执行**一个 `send`** 工具调用
2. 执行完 `send` 后，整个推理循环立即退出
3. Root Agent 进入等待状态，直到被子 Agent 的回复唤醒
4. 被唤醒后才能发送下一个 `send`

### 2.2 典型场景时序

假设用户说「帮我写一本小说」，Root Agent 决定创建 4 个子 Agent：

```
Round 1: LLM → createAgent("目标分析") → 继续
Round 2: LLM → createAgent("题材调研") → 继续
Round 3: LLM → createAgent("大纲撰写") → 继续
Round 4: LLM → createAgent("小说润色") → 继续
Round 5: LLM → send(目标分析Agent, "请分析...") → BREAK！退出循环
         ↓
         Root Agent 进入等待...
         ↓
         目标分析 Agent 收到消息 → 处理 → 回复
         ↓
         Root Agent 被唤醒
         ↓
Round 6: LLM → send(题材调研Agent, "请调研...") → BREAK！
         ↓
         ... 串行执行 ...
```

**结果**：4 个子 Agent 是串行接收任务的，不是并行的。

### 2.3 Prompt 也强化了这个限制

```
【重要规则】
- createAgent 和 send 不要在同一轮调用
```

这条规则让 LLM 倾向于先创建所有 Agent，再逐个发送。但由于 `send` 后 break 的机制，每次只能发一个。

---

## 三、架构图

```
┌─────────────────────────────────────────────────────────┐
│                    Workspace                             │
│                                                          │
│  ┌──────────┐    P2P群     ┌──────────────┐             │
│  │  Human   │◄────────────►│ Root Agent   │             │
│  └──────────┘              └──────┬───────┘             │
│       │                          │                       │
│       │                    ┌─────┼─────┐                │
│       │                    │     │     │                │
│       │              三方群1│三方群2│三方群3│三方群4        │
│       │                    │     │     │                │
│       ▼                    ▼     ▼     ▼                │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐   │
│  │ 目标分析 │  │ 题材调研 │  │ 大纲撰写 │  │ 小说润色 │   │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘   │
│                                                          │
│  每个三方群 = Root + 子Agent + Human                      │
│  Root 每次只能 send 一个 → 串行派发                       │
└─────────────────────────────────────────────────────────┘
```

---

## 四、消息流转详细分析

### 4.1 send 工具的群查找逻辑

```java
// SwarmTools.send()
List<SwarmGroupDTO> groups = messageService.listGroups(callerWorkspaceId, callerAgentId);
for (SwarmGroupDTO g : groups) {
    if (g.getMemberIds() != null && g.getMemberIds().contains(agentId)) {
        groupId = g.getId();
        break;  // 找到第一个包含目标 Agent 的群
    }
}
```

Root Agent 是所有三方群的成员，所以能找到每个子 Agent 对应的群。但每次只执行一个 `send`。

### 4.2 唤醒链路

```
send(agentId=子Agent1, message="任务内容")
  → SwarmMessageService.sendMessage(三方群1, req)
  → 保存消息 + 发布 SwarmMessageSentEvent
  → SwarmMessageEventListener.onMessageSent
  → 遍历三方群1的成员 [Root, 子Agent1, Human]
  → wakeAgent(子Agent1)  ← 唤醒子 Agent
  → wakeAgent(Human)     ← Human 不是 Agent Runner，忽略
  → Root 不在列表中（是发送者，被排除）
```

### 4.3 子 Agent 回复链路

```
子Agent1 被唤醒 → processTurn → processAgentMessages
  → LLM 推理 → send(RootAgentId, "结果...")
  → SwarmMessageService.sendMessage(三方群1, req)
  → SwarmMessageEventListener → wakeAgent(Root)
  → Root 被唤醒 → processTurn → processAgentMessages
  → 协调者模式：跳过 send，将内容投递到 Human P2P 群
```

---

## 五、问题总结与改进方向

### 5.1 当前行为（串行）

| 步骤 | 行为 | 问题 |
|------|------|------|
| 创建 Agent | 可以在一轮中创建多个（多轮 LLM） | 正常 |
| 派发任务 | 每次只能 send 一个，然后等待回复 | **串行瓶颈** |
| 等待回复 | 第一个子 Agent 回复后才能 send 第二个 | **效率低** |
| 汇总结果 | 每次只收一个子 Agent 的结果 | **逐个汇总** |

### 5.2 改进方案（如果需要并行派发）

**方案 A：移除 send 后的 break（最小改动）**

修改 `processHumanMessages` 中的逻辑，允许在一轮中执行多个 `send`：
- 移除 `if ("send".equals(toolName)) break;`
- 移除外层 `if (response.getToolCalls().stream().anyMatch(tc -> "send".equals(tc.name()))) break;`
- 所有 send 执行完后再退出循环等待

风险：需要处理多个子 Agent 并行回复时的汇总逻辑。

**方案 B：批量 send 工具（新增工具）**

新增 `sendBatch(agents: [{agentId, message}])` 工具，一次性向多个 Agent 派发任务。

**方案 C：修改 Prompt 引导 LLM 行为**

更新 Root Prompt，明确说明：
- 可以在一轮中调用多个 send
- 创建完所有 Agent 后，在下一轮一次性 send 给所有 Agent

### 5.3 当前 Prompt 的限制

```
- createAgent 和 send 不要在同一轮调用
```

这条规则是合理的（避免创建和发送混在一起），但没有说明可以在一轮中 send 多个 Agent。需要补充：

```
- 可以在一轮中向多个子 Agent 发送任务（多次调用 send）
- 所有 send 执行完后，等待子 Agent 回复
```

---

## 六、关键代码位置

| 文件 | 行号 | 说明 |
|------|------|------|
| `SwarmAgentRunner.java` | ~275 | `send` 后 break 内层循环 |
| `SwarmAgentRunner.java` | ~287 | `send` 后 break 外层循环 |
| `SwarmTools.java` | ~65 | `send` 方法：查找群 → 发消息 |
| `SwarmWorkspaceService.java` | ~164 | `createAgent`：创建三方群 |
| `SwarmMessageEventListener.java` | ~27 | 消息 → 唤醒群成员 |
| `SwarmPromptTemplate.java` | ~9 | Root Prompt 模板 |
