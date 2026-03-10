# Swarm 多智能体协作模块 — 探索文档

> 创建时间：2026-03-05
> 最后更新：2026-03-05
> 状态：需求确认完成，待实施

---

## 〇、目标需求

### 0.1 业务愿景

将系统已有的工作流（Agent）能力与多智能体协作结合，让 AI Agent 能够自主创建工作流、执行工作流、创建子 Agent 并派发子任务，形成**一层**协作拓扑，最终由初始 Agent 汇总结果。

**典型场景**：用户说「帮我写一本小说」→ 初始 Agent 按领域拆分（目标分析、题材调研、大纲撰写、小说润色）→ 为每个领域创建子 Agent → 派发任务 → 子 Agent 完成后发回结果 → 初始 Agent 汇总。

### 0.2 核心需求

| 编号 | 需求 | 说明 |
|------|------|------|
| R1 | 工作流创建封装为工具 | 初始 Agent 可通过工具调用设计完整 graphJson 创建新工作流（即新 Agent） |
| R2 | 工作流执行封装为工具 | 初始 Agent 可通过 `executeWorkflow(agentId)` 执行已发布 Agent 的工作流，复用 SchedulerService |
| R3 | 默认页面 | 人类 + 一个初始 Agent |
| R4 | 点击侧边栏切换 Agent | 切换后聊天面板展示人类与该 Agent 的 P2P 对话，用户以人类身份发消息 |
| R5 | Agent 间消息通信 | 系统为 Agent 提供 `send` 工具，用于向指定 Agent 发消息、派发子任务 |
| R6 | 禁止递归 | 子 Agent 不能创建子 Agent，也不能创建或执行工作流 |
| R7 | 去掉 Graph 展示 | 工具调用已在聊天中展示 Agent 创建过程，graph 不再需要 |

### 0.3 工具集设计

**初始 Agent（Root Agent）工具集：**

| 工具 | 参数 | 说明 |
|------|------|------|
| `createAgent` | role, description, graphJson? | 创建子 Agent，可选附带工作流图。工作流就是 Agent，两者合并为同一工具 |
| `executeWorkflow` | agentId | 执行某个已发布 Agent 的工作流，复用 SchedulerService，结果作为工具返回值 |
| `send` | agentId, message | 向指定 Agent 发消息（派发子任务） |
| `listAgents` | — | 列出工作空间内所有 Agent |
| `self` | — | 返回自身信息 |

**子 Agent 工具集（受限）：**

| 工具 | 参数 | 说明 |
|------|------|------|
| `send` | agentId, message | 向指定 Agent 发消息（返回结果给父 Agent） |
| `self` | — | 返回自身信息 |

### 0.4 执行流程

```
用户 → [人类消息] → 初始 Agent
  → LLM 分析任务，决定拆分策略
  → 调用 createAgent("researcher", "题材调研") 创建子 Agent
  → 调用 createAgent("writer", "大纲撰写") 创建子 Agent
  → 调用 send(researcherId, "请调研玄幻题材...") 派发任务
  → 子 Agent 被唤醒 → 纯 LLM 推理 → send(rootAgentId, "调研结果...")
  → 初始 Agent 被唤醒 → 汇总所有子 Agent 结果
  → 回复用户
```

### 0.5 UI 布局（目标）

```
┌─────────────────────────────────────────┐
│ 左侧 Sidebar (240px)  │ 右侧 Chat Panel │
│                        │                 │
│ Agent 列表（树形）      │ 消息列表         │
│ ├── 👤 我（human）     │ （人类与选中      │
│ ├── 🤖 初始 Agent     │   Agent 的 P2P） │
│ │   ├── 🤖 调研员     │                 │
│ │   └── 🤖 撰写员     │ 输入框           │
│                        │                 │
└─────────────────────────────────────────┘
```

点击侧边栏 Agent 项 → 切换聊天面板为人类与该 Agent 的 P2P 对话。

### 0.6 关键决策记录

| 决策 | 结论 | 原因 |
|------|------|------|
| 工作流执行是否持久化 | 临时执行，不持久化 | Swarm 场景下工作流是辅助手段 |
| 工作流中人工审核由谁处理 | 父 Agent | 保持自动化链路 |
| createAgent 与 createWorkflow 是否分开 | 合并为一个工具 | 系统中工作流 = Agent |
| 子 Agent 是否可递归创建 | 禁止 | 避免复杂度爆炸 |
| 是否保留 Graph 展示 | 去掉 | 工具调用已在聊天中可视化 |
| Agent 切换方式 | 侧边栏点击 | 去掉 graph 后的自然交互 |

---

## 一、模块概述（现状）

Swarm 模块实现了一个**类 IM 的多 Agent 协作系统**。人类用户和多个 AI Agent 在「工作空间」中通过群组消息进行协作。Agent 具备自主创建子 Agent、互相发消息、调用工具的能力，形成树状协作拓扑。

### 核心概念

| 概念 | 说明 |
|------|------|
| **Workspace** | 工作空间，协作的最小隔离单元，属于某个用户 |
| **Agent** | 工作空间内的参与者，分为 `HUMAN`（人类）和 `ASSISTANT`（AI），通过 `parentId` 形成树结构 |
| **Group** | IM 群组，P2P 为两人群，也支持多人群（如三方群） |
| **Message** | 群组内的消息，附带 `senderId`、`contentType` |
| **Tool** | Agent 可调用的工具（create / send / listAgents 等） |

---

## 二、数据模型

### 2.1 数据库表（5 张）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `swarm_workspace` | 工作空间 | id, name, user_id, llm_config_id, max_rounds_per_turn |
| `swarm_workspace_agent` | Agent | id, workspace_id, agent_id, role, parent_id, llm_history, status |
| `swarm_group` | 群组 | id, workspace_id, name, context_tokens |
| `swarm_group_member` | 群成员 | group_id, agent_id, last_read_message_id（联合主键） |
| `swarm_message` | 消息 | id, workspace_id, group_id, sender_id, content_type, content, send_time |

### 2.2 领域实体

| 实体 | 所在层 | 聚合关系 |
|------|--------|----------|
| `SwarmWorkspace` | domain | 聚合根 |
| `SwarmAgent` | domain | 实体，parentId 自引用形成树 |
| `SwarmGroup` | domain | 实体 |
| `SwarmMessage` | domain | 实体 |

### 2.3 值对象

| 值对象 | 枚举值 |
|--------|--------|
| `SwarmAgentStatus` | IDLE（空闲）, BUSY（忙碌）, WAKING（唤醒中）, STOPPED（已停止） |
| `SwarmMessageType` | TEXT |
| `SwarmRole` | HUMAN, ASSISTANT |

---

## 三、功能清单

### 3.1 Workspace 管理

| 功能 | API | 状态 |
|------|-----|------|
| 创建工作空间 | `POST /api/swarm/workspace` | ✅ 已实现 |
| 列出工作空间 | `GET /api/swarm/workspace` | ✅ 已实现 |
| 获取工作空间详情 | `GET /api/swarm/workspace/{id}` | ✅ 已实现 |
| 更新工作空间 | `PUT /api/swarm/workspace/{id}` | ✅ 已实现 |
| 删除工作空间 | `DELETE /api/swarm/workspace/{id}` | ✅ 已实现（级联删除） |
| 获取默认配置 | `GET /api/swarm/workspace/{id}/defaults` | ✅ 已实现 |

创建工作空间时自动创建：human Agent + assistant Agent + P2P 群 + 群成员关系。

### 3.2 Agent 管理

| 功能 | API | 状态 |
|------|-----|------|
| 列出 Agent | `GET /api/swarm/workspace/{wid}/agents` | ✅ 已实现 |
| 创建 Agent | `POST /api/swarm/workspace/{wid}/agents` | ✅ 已实现 |
| 获取 Agent 详情 | `GET /api/swarm/agent/{id}` | ✅ 已实现 |
| 停止 Agent | `POST /api/swarm/agent/{id}/stop` | ✅ 已实现 |
| 中断所有 Agent | `POST /api/swarm/agents/interrupt-all` | ⚠️ 标注 TODO P2 |

创建子 Agent 时自动创建三方群（parent + 子 Agent + human）。

### 3.3 群组与消息

| 功能 | API | 状态 |
|------|-----|------|
| 列出群组 | `GET /api/swarm/workspace/{wid}/groups` | ✅ 已实现（含 unreadCount、lastMessage） |
| 获取群消息 | `GET /api/swarm/group/{gid}/messages` | ✅ 已实现（支持 markRead） |
| 发送消息 | `POST /api/swarm/group/{gid}/messages` | ✅ 已实现 |
| 创建 P2P 群 | `POST /api/swarm/workspace/{wid}/groups/p2p` | ✅ 已实现（幂等） |

### 3.4 图谱与搜索

| 功能 | API | 状态 |
|------|-----|------|
| 获取拓扑图 | `GET /api/swarm/workspace/{wid}/graph` | ✅ 已实现（节点=Agent，边=parent关系，权重=消息数） |
| 搜索 | `GET /api/swarm/workspace/{wid}/search` | ✅ 已实现（按 role/name 模糊匹配） |

### 3.5 SSE 推送（双通道）

| 通道 | API | 事件类型 |
|------|-----|----------|
| UI Stream | `GET /api/swarm/workspace/{wid}/ui-stream` | `ui.agent.created`, `ui.message.created`, `ui.agent.stream.start/chunk/done`, `ui.agent.waiting/waiting.done` |
| Agent Stream | `GET /api/swarm/agent/{agentId}/stream` | `agent.stream`(reasoning/content/tool_calls/tool_result), `agent.done`, `agent.error` |

### 3.6 Agent 工具集

| 工具名 | 参数 | 说明 |
|--------|------|------|
| `create` | role, description | 创建子 Agent |
| `send` | agent_id, message | 向指定 Agent 发 P2P 消息 |
| `sendGroupMessage` | group_id, message | 向群组发消息 |
| `listAgents` | — | 列出工作空间内所有 Agent |
| `listGroups` | — | 列出可见群组 |
| `self` | — | 返回自身信息 |

---

## 四、核心流程

### 4.1 Agent 运行时循环（SwarmAgentRunner）

```
run() 循环:
  ┌─→ 等待 wakeSignal（CompletableFuture）
  │     ↓
  │   processTurn():
  │     1. 状态 → BUSY
  │     2. 拉取未读消息（按 human/agent 分组）
  │     3. 处理 human 消息（多轮 LLM + 工具调用，最多 maxRoundsPerTurn 轮）
  │     4. 处理 agent 消息（单轮 LLM）
  │     5. 标记已读，状态 → IDLE
  └─────────────────────────────────────┘
```

### 4.2 消息驱动的唤醒机制

```
sendMessage
  → 保存 SwarmMessage
  → 发布 SwarmMessageSentEvent（Spring Event）
  → @Async SwarmMessageEventListener
    → 遍历群成员（排除发送者）
    → wakeAgent(memberId)
      → runner.wake()（完成 CompletableFuture）
      → processTurn()
```

### 4.3 父子 Agent 协作链路

```
1. 人类发消息给 Assistant
2. Assistant 收到 → processHumanMessages → 多轮 LLM
3. Assistant 调用 create(role="researcher") → 创建子 Agent + 三方群
4. Assistant 调用 send(子agentId, "请调研XXX") → 触发子 Agent 唤醒
5. 子 Agent processTurn → 完成任务 → send(parentId, "结果是...")
6. Assistant 被唤醒 → processAgentMessages → 汇总回复人类
```

### 4.4 协调者防 ping-pong 机制

具有 human P2P 群的 Agent 被视为「协调者」。当协调者处理子 Agent 消息时：
- 如果 LLM 回复包含 `send` / `sendGroupMessage` 工具调用 → 跳过执行，将 message 内容直接投递到 human 群
- 如果回复有文本内容 → 也自动投递到 human 群
- 目的：避免协调者和子 Agent 之间的消息无限循环

---

## 五、前端实现

### 5.1 页面与路由

| 路由 | 页面 | 说明 |
|------|------|------|
| `/swarm` | `SwarmWorkspaceListPage` | 工作空间列表，创建/进入/删除 |
| `/swarm/:workspaceId` | `SwarmMainPage` | 协作主界面 |

### 5.2 SwarmMainPage 布局

```
┌────────────────────────────────────────────────────┐
│ 左侧 Sider (240px)       │ 右侧主区域               │
│                           │                          │
│ SwarmSidebar              │ 上半 (40%) — SwarmGraph   │
│ ├── SwarmSearchBar        │   GraphNode (圆形节点)    │
│ └── AgentTreeList         │   GraphEdge (连线+计数)   │
│     (树形，带状态灯/未读数) │                          │
│                           │ 下半 (60%) — SwarmChatPanel│
│                           │   SwarmMessageList        │
│                           │   WaitingCard (可选)      │
│                           │   SwarmComposer           │
└────────────────────────────────────────────────────┘
```

### 5.3 关键组件

| 组件 | 说明 |
|------|------|
| `AgentTreeList` | 按 parentId 建树，Ant Design Tree，human 用 👤，AI 用 🤖 |
| `SwarmMessageBubble` | 人类消息右对齐蓝色、Agent 左对齐灰色，支持 tool_call 展示 |
| `SwarmGraph` | @xyflow/react 画布，节点状态灯（IDLE 绿/BUSY 红/WAKING 黄/STOPPED 灰） |
| `WaitingCard` | 等待子 Agent 时显示，支持终止操作 |
| `ToolCallBadge` | 可展开的工具调用 Tag，展示工具名、参数、结果 |
| `AgentDetailDrawer` | Agent 详情抽屉（实时输出/工具调用/LLM History），**当前未启用** |

### 5.4 Hooks

| Hook | 说明 |
|------|------|
| `useSwarmWorkspace` | workspace 列表 CRUD |
| `useSwarmAgents` | workspace 内 Agent 列表 |
| `useSwarmMessages` | 群消息加载/发送/乐观更新 |
| `useSwarmGraph` | agents + edges → @xyflow/react 格式，树形布局计算 |
| `useUIStream` | 订阅 workspace 级 SSE，分发 UI 事件 |
| `useAgentStream` | 订阅单 Agent SSE（用于 AgentDetailDrawer） |

### 5.5 状态管理

Swarm 模块未使用 Zustand store，全部通过 `useState` + `useCallback` 管理本地状态，结合 SSE 回调更新。

---

## 六、已发现的问题

### 6.1 数据库 Schema 与代码不一致

| 严重度 | 描述 |
|--------|------|
| 🔴 高 | `SwarmWorkspaceAgentPO` 有 `description` 字段，但 `swarm_workspace_agent` 表中未定义此列。插入含 description 的记录会导致 SQL 错误。 |
| 🟡 中 | `ai_agent.sql`（项目根目录的 schema 文件）中不包含 swarm 表，实际 schema 在 `docker/init/mysql/01_init_schema.sql`，两处 schema 文件分裂。 |

### 6.2 MyBatis-Plus 联合主键问题

| 严重度 | 描述 |
|--------|------|
| 🟡 中 | `SwarmGroupMemberPO` 的 `@TableId(type = IdType.INPUT)` 仅标注在 `groupId` 上，实际是联合主键 `(group_id, agent_id)`。MyBatis-Plus 的 `BaseMapper` 不支持联合主键，`selectById` / `updateById` / `deleteById` 无法正确工作。当前代码通过自定义 `@Select` / `@Update` 注解绕过了这个问题，但语义上存在隐患。 |

### 6.3 代码重复与遗留

| 严重度 | 描述 |
|--------|------|
| 🟡 中 | `SwarmToolExecutor` 与 `SwarmTools` 功能完全重叠。`SwarmAgentRunner` 只使用 `SwarmTools`，`SwarmToolExecutor` 未被任何代码引用，属于遗留代码。 |
| 🟢 低 | `SwarmTools.send` 要求已有 P2P 群否则报错；`SwarmToolExecutor.executeSend` 无群时会自动创建。两者行为不一致，若未来切换实现可能引发 bug。 |

### 6.4 Domain 层纯洁性违规

| 严重度 | 描述 |
|--------|------|
| 🟡 中 | `SwarmDomainService` 使用了 `@Service` 注解，domain 层引入了 Spring 框架依赖，违反了项目 DDD 分层原则（domain 应保持纯净，不依赖任何框架）。 |

### 6.5 功能缺失 / TODO

| 严重度 | 描述 |
|--------|------|
| 🟡 中 | **LLM History 未加载**：`SwarmAgentRunner.buildMessages()` 中有 TODO，尚未从 `agent.getLlmHistory()` 加载历史对话上下文。当前每次 turn 都是无记忆的，Agent 无法回忆上一轮对话。 |
| 🟢 低 | **interruptAll 未完成**：`SwarmAgentController` 的 `interrupt-all` 接口标注 TODO P2。 |

### 6.6 前端问题

| 严重度 | 描述 |
|--------|------|
| 🟡 中 | **AgentDetailDrawer 未启用**：`SwarmMainPage` 中 `drawerOpen` 始终为 `false`，已渲染的 `AgentDetailDrawer` 永远不可见。用户无法查看 Agent 的实时推理过程和工具调用详情。 |
| 🟢 低 | **GraphBeam 未使用**：`GraphBeam.ts` 定义了边光束动画（`triggerBeam`），但未在任何组件中调用。 |

---

## 七、架构评估

### 7.1 优点

- **消息驱动架构**：通过 Spring Event + 异步唤醒实现 Agent 间松耦合通信
- **树状协作拓扑**：parentId 自引用支持任意深度的层级协作
- **防 ping-pong 机制**：协调者模式有效避免消息循环
- **双通道 SSE**：workspace 级 UI 事件 + Agent 级详情流，关注点分离
- **工具扩展性**：`SwarmToolRegistry` 统一注册 OpenAI Function Tool，易于扩展
- **虚拟线程**：Agent Runner 使用 Java 21 虚拟线程，高并发低开销

### 7.2 待改进

- **无持久化的运行时**：`SwarmAgentRuntimeService` 的 `runners` Map 存储在内存中，服务重启后所有运行中的 Agent 丢失
- **单机限制**：EventBus（`SwarmAgentEventBus`、`SwarmUIEventBus`）基于 `ConcurrentHashMap`，不支持多实例部署
- **缺乏上下文窗口管理**：`context_tokens` 字段已定义但未使用，长对话可能超出 LLM 上下文窗口
- **错误恢复**：Agent 执行异常时缺乏重试或降级机制
- **权限控制**：未校验 workspace 归属，任何已登录用户理论上可操作他人 workspace

---

## 八、代码路径速查

| 分类 | 路径 |
|------|------|
| **领域层** | `ai-agent-domain/.../domain/swarm/` |
| **应用层** | `ai-agent-application/.../application/swarm/` |
| **基础设施层** | `ai-agent-infrastructure/.../infrastructure/swarm/` |
| **接口层** | `ai-agent-interfaces/.../interfaces/swarm/` |
| **前端模块** | `ai-agent-foward/src/modules/swarm/` |
| **数据库 Schema** | `ai-agent-infrastructure/.../docker/init/mysql/01_init_schema.sql` |
| **Prompt 模板** | `ai-agent-application/.../swarm/prompt/SwarmPromptTemplate.java` |
| **LLM 调用** | `ai-agent-infrastructure/.../swarm/llm/SwarmLlmCaller.java` |
| **工具注册** | `ai-agent-infrastructure/.../swarm/tool/SwarmToolRegistry.java` |
| **SSE EventBus** | `ai-agent-infrastructure/.../swarm/sse/SwarmAgentEventBus.java`, `SwarmUIEventBus.java` |

---

## 九、需求与现状差距分析

### 9.1 需要新增

| 项目 | 说明 | 涉及层 |
|------|------|--------|
| `createAgent` 工具增加 graphJson 参数 | 合并 createAgent + createWorkflow，创建 Agent 时可选附带工作流图 | application, infrastructure |
| `executeWorkflow` 新工具 | 调用 SchedulerService 执行已发布 Agent 的工作流，将结果作为工具返回值 | application, infrastructure |
| 工具集区分（Root vs Sub） | 初始 Agent 拥有全部 5 个工具，子 Agent 只有 send + self | application (SwarmAgentRunner, SwarmPromptTemplate) |
| 禁止递归逻辑 | 子 Agent 的 Prompt 和工具集中移除 createAgent / executeWorkflow | application (SwarmPromptTemplate, SwarmTools) |

### 9.2 需要修改

| 项目 | 现状 | 目标 | 涉及层 |
|------|------|------|--------|
| SwarmTools | 6 个工具，无工作流相关 | Root: createAgent(+graphJson), executeWorkflow, send, listAgents, self；Sub: send, self | application, infrastructure |
| SwarmPromptTemplate | 统一 Prompt | 区分 Root Agent 和 Sub Agent 的 Prompt | application |
| SwarmAgentRunner | processHumanMessages 多轮均可调用所有工具 | 根据 agent 层级（root/sub）注入不同工具集 | application |
| SwarmToolRegistry | 注册 6 个固定 Function Tool | 按角色动态注册工具（Root 5个 / Sub 2个） | infrastructure |

### 9.3 需要删除

| 项目 | 说明 | 涉及层 |
|------|------|--------|
| `sendGroupMessage` 工具 | 需求中未涉及群组消息，P2P 通信即可 | application, infrastructure |
| `listGroups` 工具 | 同上 | application, infrastructure |
| Graph 可视化 | 前端去掉 SwarmGraph 及相关组件 | frontend |
| GraphBeam | 未使用的动画组件 | frontend |

### 9.4 现有问题（需一并修复）

| 优先级 | 问题 | 说明 |
|--------|------|------|
| P0 | Schema 缺 description 列 | `swarm_workspace_agent` 表需要加 `description` 列，否则创建 Agent 会报错 |
| P1 | LLM History 未加载 | Agent 每轮都丢失上下文，影响多轮协作 |
| P1 | SwarmToolExecutor 遗留代码 | 与 SwarmTools 重复，应清理 |
| P2 | Domain 层 Spring 依赖 | SwarmDomainService 使用 @Service |
| P2 | AgentDetailDrawer 未启用 | 去掉 graph 后此组件是否保留需决策 |

### 9.5 实施优先级建议

```
Phase 1 — 基础修复 & 工具扩展（后端）
  ├── 修复 Schema（加 description 列）
  ├── 新增 createAgent 的 graphJson 参数支持
  ├── 新增 executeWorkflow 工具（对接 SchedulerService）
  ├── 区分 Root / Sub Agent 工具集
  ├── 更新 SwarmPromptTemplate
  ├── 禁止递归逻辑
  └── 清理 SwarmToolExecutor / sendGroupMessage / listGroups

Phase 2 — 前端改造
  ├── 去掉 SwarmGraph 及相关组件
  ├── 调整 SwarmMainPage 布局（sidebar + chat）
  ├── 实现侧边栏 Agent 点击切换 P2P 对话
  └── 工具调用在聊天中的可视化优化

Phase 3 — 体验优化
  ├── LLM History 加载
  ├── 错误处理与重试
  └── 权限校验
```
