# 优化 Swarm/Workflow 多智能体协作提示词与交互方式

> 参照 Claude-Code Coordinator 模式

## 1. Goal

优化 Java 后端中 Swarm Agent 和 Workflow 节点执行器的提示词设计及智能体间交互协议，使多智能体协作更高效、可控。

## 2. Background: Claude-Code Coordinator 模式核心要素

基于 `docs/claude-code-multi-agent-patterns.md` 的调研：

### 2.1 Phase-Based Workflow
| Phase | 执行者 | 职责 |
|-------|--------|------|
| Research | Workers（并行） | 调研代码库，找相关文件 |
| Synthesis | Coordinator | 整合发现，撰写规格文档 |
| Implementation | Workers | 按规格实施变更 |
| Verification | Workers | 测试变更是否正确 |

### 2.2 Continue vs. Spawn 决策矩阵
- Research 命中精确文件 → Continue（上下文有价值）
- 研究广泛、实现狭窄 → Spawn 新 Agent（避免探索噪声）
- 验证他人代码 → Spawn 新 Agent（避免偏见）
- 纠正失败/扩展近期工作 → Continue（错误上下文有价值）

### 2.3 Task Notification 协议
Worker 结果以 XML 格式通知：
```xml
<task-notification>
  <task-id>...</task-id>
  <status>completed|failed|killed</status>
  <summary>摘要</summary>
  <result>最终文本</result>
  <usage>...</usage>
</task-notification>
```

### 2.4 星型 Coordinator 架构
- Coordinator 由 `CLAUDE_CODE_COORDINATOR_MODE` 环境变量激活
- 工具集：`Agent`（Spawn）、`SendMessage`（继续）、`TaskStop`（停止）、团队管理
- Worker 通过 `idle_notification` 上报结果

## 3. 初步问题（待确认）

### Q1: 优化范围 — 哪个子系统？
**✅ 确认变更: [A 仅 Swarm] → [C] Swarm Agent（后端） + 前端协作可视化**
- 后端：`SwarmTools`、`SwarmAgentRunner`、`SwarmMessageService`
- 前端：需要展示 Coordinator ↔ Worker 的派发、运行状态、结果上报等协作过程

### Q2: 核心痛点
**✅ 确认: [A] + [B] + [C] 全部**
- A: Agent 输出格式不稳定，难以可靠解析
- B: 缺少 Phase 结构化任务分解，Agent 不知道该先做什么
- C: 任务分配→执行→结果上报链路不完整

### Q3: "优化提示词" 具体指哪些方面？
**✅ 确认: [D] 以上全部**
- A: 重新设计 Agent System Prompt — 角色定义、行为规范、输出格式约束
- B: 改进任务分配消息模板 — `send()` 消息的结构化格式（角色 + 目标 + 约束）
- C: 引入 Structured Output — JSON Schema 规范 LLM 输出，便于解析

### Q4: "参照 Coordinator" 具体指哪些方面？
**✅ 确认: [D] 以上全部**
- A: 引入 Phase 概念 — `Research → Synthesis → Implementation → Verification` 阶段性任务分解
- B: 引入任务分解 + 结果汇总机制 — Coordinator 发任务，Worker 执行并上报结构化结果
- C: 引入 Continue vs. Spawn 的动态决策逻辑 — 根据 Worker 上下文质量决定继续还是新建

### Q5: Coordinator 角色引入方式
**✅ 确认: 复用 parentId（不新增 CoordinatorType）**
- 参考 Claude-Code：`CLAUDE_CODE_COORDINATOR_MODE` env var 控制 main session 角色，非 agent type
- Java 实现：在 `SwarmAgent.systemPrompt` 中增加条件分支
  - `parentId != null` → Worker prompt（执行者）
  - `parentId == null && 有子 agent` → Coordinator prompt（协调者）

### Q6: 前端协作可视化范围
**✅ 确认: 部分实现（参照 Claude-Code）**
- 形式：类似终端，Coordinator 下方显示所有 background agents 行
- 需要实现：
  - **AgentTreeView** — Coordinator 在顶 + Workers 树形列表，参照 `TeammateSpinnerTree`
  - **WorkerCard** — 每个 Worker 状态卡片（running/idle/completed/failed + 耗时/token）
  - **SwarmNotification** — Worker 派发、完成、失败的通知提示
  - **SwarmSSEListener** — 监听 `/api/swarm/agent/{id}/stream`，解析 `task-notification`
- 不需要：SwarmMessageList（群聊形式）

## 4. 待确认的技术决策

- 是否需要修改数据库 schema（SwarmMessage、SwarmGroup 新增字段？）
- 是否需要新增 API endpoint？
- 提示词模板存储在哪里（数据库 / 代码 / 文件）？
- 是否需要引入 Playground 便于调试提示词？

## 5. Acceptance Criteria（草稿，待确认）

- [ ] [待填写]
