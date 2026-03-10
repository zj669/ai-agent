# Swarm 多 Agent 并行派发任务修复

- **日期**: 2026-03-06
- **模块**: Swarm / 后端
- **严重程度**: 功能缺陷

## 问题描述

Root Agent 创建多个子 Agent 后，只有第一个子 Agent 能收到任务消息，其余子 Agent 无法收到。任务派发变成串行：每次只能 send 一个子 Agent，等其回复后才能 send 下一个。

## 根因分析

`SwarmAgentRunner.processHumanMessages` 中，执行 `send` 工具调用后立即 `break` 跳出内层 for 循环和外层 while 循环，导致一轮 LLM 推理中只有第一个 `send` 被执行。

问题代码位置：
- 内层 break：`if ("send".equals(toolName)) break;`（第 276 行）
- 外层 break：`if (response.getToolCalls().stream().anyMatch(...)) break;`（第 288 行）

同时 Root Prompt 中的规则「createAgent 和 send 不要在同一轮调用」限制了 LLM 的行为，且未明确说明可以一次性 send 多个 Agent。

## 修复方案

### 1. SwarmAgentRunner.java

- 移除 send 后的内层 `break`，让 send 与其他工具（如 createAgent）一样，执行后继续处理后续 tool call
- 所有 tool call（包括 send）执行后都追加到 messages 列表
- 用 `boolean hasSend` 标记本轮是否有 send 调用
- 外层循环改为：有 send 则执行完所有工具后退出循环等待子 Agent 回复；无 send 则继续下一轮 LLM 推理

### 2. SwarmPromptTemplate.java

- 删除「createAgent 和 send 不要在同一轮调用」
- 新增「创建完所有子 Agent 后，在下一轮一次性向所有子 Agent 发送任务（多次调用 send）」
- 新增「可以在一轮中调用多个 send，向不同子 Agent 并行派发任务」
- 新增「每个子 Agent 回复后立即处理其结果，不必等所有子 Agent 都完成」

## 修改文件

| 文件 | 改动 |
|------|------|
| `SwarmAgentRunner.java` | 移除 send 后 break，增加 hasSend 标记控制外层循环 |
| `SwarmPromptTemplate.java` | 更新 ROOT_TEMPLATE 协作流程和规则 |

## 改动后的时序

```
Round 1-3: LLM -> createAgent x3 -> 继续下一轮
Round 4:   LLM -> send(A1, "任务1"), send(A2, "任务2"), send(A3, "任务3")
           -> 全部执行 -> 退出循环等待
           -> A1、A2、A3 并行被唤醒、并行处理
           -> 子 Agent 逐个回复 -> Root 逐个处理 -> 投递给 Human
```
