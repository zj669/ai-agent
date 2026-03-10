# Bug 记录：终止无效 + Agent 状态不刷新 + 工具调用不展示

| 项目 | 内容 |
|------|------|
| **日期** | 2026-03-06 |
| **模块** | 后端 `SwarmAgentRunner` / `SwarmLlmCaller` + 前端 Swarm 聊天 |
| **严重度** | P0（终止功能无效） / P1（状态不刷新） / P2（工具图标缺失） |
| **发现方式** | 用户测试 + 浏览器控制台日志分析 |

---

## 问题描述

### 问题1：终止按钮无效
点击终止按钮后，前端 API 调用成功（`stopAgent API succeeded`），但后端 Agent 仍继续运行。

**根因**：`SwarmAgentRunner.stop()` 仅设置 `running = false`，但 `SwarmLlmCaller.callStreamWithTools` 中 `flux.blockLast()` 是无限期阻塞操作，线程卡在等待 LLM API 返回，无法检查 `running` 标志。

### 问题2：Agent 状态不刷新
Agent 回复完成后，侧边栏状态灯仍为红色（BUSY），需刷新页面才能恢复。

**根因**：`onStreamDone` SSE 回调中没有调用 `reloadAgents()`，只清除了 streaming 状态和刷新消息。

### 问题3：createAgent 工具调用不展示
Agent 调用 `createAgent` 工具时，聊天记录中没有显示工具调用的蓝色 Tag。

**根因**：`ToolCallBadge.tsx` 的 `TOOL_ICONS` 只包含旧工具名 `create`，不包含重命名后的 `createAgent`、`executeWorkflow`、`listAgents`。

---

## 修复方案

### 后端修复

**SwarmAgentRunner.java**：
- 新增 `volatile Thread runThread` 字段，在 `run()` 入口保存 `Thread.currentThread()`
- `stop()` 中调用 `runThread.interrupt()` 中断阻塞的 `blockLast()`
- `processHumanMessages` 循环条件加入 `&& running`
- 工具执行循环开头检查 `if (!running) break`
- `run()` catch 块检查线程中断状态

**SwarmLlmCaller.java**：
- 两处 `flux.blockLast()` 改为 `flux.blockLast(Duration.ofMinutes(5))`，避免无限阻塞
- 线程被中断时 Reactor 会取消 Flux 订阅并抛出异常

### 前端修复

**SwarmMainPage.tsx**：
- `onStreamDone` 回调中增加 `reloadAgents()` 调用

**ToolCallBadge.tsx**：
- `TOOL_ICONS` 增加 `createAgent`、`executeWorkflow`、`listAgents` 映射

---

## 涉及文件

| 文件 | 操作 | 改动要点 |
|------|------|----------|
| `SwarmAgentRunner.java` | 修改 | 线程引用 + 中断 + 循环检查 |
| `SwarmLlmCaller.java` | 修改 | `blockLast` 加 5 分钟超时 |
| `SwarmMainPage.tsx` | 修改 | `onStreamDone` 增加 `reloadAgents()` |
| `ToolCallBadge.tsx` | 修改 | 增加新工具名的图标映射 |

---

## 验证

- `mvn compile -DskipTests` → BUILD SUCCESS
- `npx tsc --noEmit` → 零错误
