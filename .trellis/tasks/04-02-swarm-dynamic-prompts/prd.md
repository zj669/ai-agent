# Swarm 动态提示词系统

## 状态

**当前阶段**: 规划中

## 背景问题

### 现状痛点

1. **提示词硬编码**：所有提示词以 `String` 常量直接写在 `SwarmPromptTemplate.java` / `SwarmPromptTemplates.java` 中
2. **废弃工具残留**：提示词中仍引用已删除的工具（如 `writing_session` / `writing_agent` 等）
3. **写作绑定语言**：`ROOT_TEMPLATE` 和 `COORDINATOR_TEMPLATE` 中包含大量写作场景术语
4. **工具与提示词不同步**：工具列表变化时，提示词描述不会自动更新

### Claude-Code 参考模式

从 [Claude-Code/src/](Claude-Code/src/) 中提炼的 4 个核心设计：

| 模式 | Claude-Code 实现 | ai-agent 当前实现 | 差距 |
|------|-----------------|-----------------|------|
| **工具过滤** | `COORDINATOR_MODE_ALLOWED_TOOLS` Set，按角色过滤 | 无 | ❌ 缺失 |
| **提示词分层** | `buildEffectiveSystemPrompt()` 多层叠加 | 单层 String.format | ❌ 需改进 |
| **工具描述分离** | 每个工具有独立 `prompt.ts` | 工具描述混杂在 SwarmTools 的 @Tool 注解 | ⚠️ 不足 |
| **团队通信模板** | `teammatePromptAddendum.ts` 附加通信规则 | send 格式散落在 COORDINATOR_TEMPLATE | ⚠️ 需抽取 |

### Claude-Code 关键源码

```typescript
// 1. 工具白名单（按角色过滤）
// src/constants/tools.ts
export const COORDINATOR_MODE_ALLOWED_TOOLS = new Set([
  AGENT_TOOL_NAME,
  TASK_STOP_TOOL_NAME,
  SEND_MESSAGE_TOOL_NAME,
  SYNTHETIC_OUTPUT_TOOL_NAME,
])

// 2. 工具过滤（组装时生效，非调用时）
// src/utils/toolPool.ts
export function applyCoordinatorToolFilter(tools: Tools): Tools {
  return tools.filter(t =>
    COORDINATOR_MODE_ALLOWED_TOOLS.has(t.name) ||
    isPrActivitySubscriptionTool(t.name),
  )
}

// 3. 分层提示词构建（优先级：override > coordinator > agent > custom > default）
// src/utils/systemPrompt.ts
export function buildEffectiveSystemPrompt({ overrideSystemPrompt, ... }): SystemPrompt {
  if (overrideSystemPrompt) return [overrideSystemPrompt]
  if (isCoordinatorMode()) return [getCoordinatorSystemPrompt(), ...appendSystemPrompt]
  // agent 模式：APPEND 到 default 而非替换（proactive 模式）
  if (agentSystemPrompt && isProactiveActive()) {
    return [...defaultSystemPrompt, `\n# Custom Agent Instructions\n${agentSystemPrompt}`, ...]
  }
  return [...(agentSystemPrompt ?? customSystemPrompt ?? defaultSystemPrompt), ...]
}

// 4. 团队通信附加模板（所有 teammate 共享）
// src/utils/swarm/teammatePromptAddendum.ts
export const TEAMMATE_SYSTEM_PROMPT_ADDENDUM = `
IMPORTANT: You are running as an agent in a team. To communicate with anyone:
- Use SendMessage tool with \`to: "<name>"\` for direct messages
- Use SendMessage tool with \`to: "*"\` for team-wide broadcasts
Just writing a response in text is not visible to others - you MUST use the SendMessage tool.
`
```

## Goal

实现一个**动态加载 + 角色过滤 + 模块化组合**的 Swarm Agent 提示词系统：

1. 提示词从配置/DB 读取，支持模板变量替换
2. 按 Agent 角色过滤可用工具（Coordinator 少工具 / Worker 多工具 / Human 极简）
3. 移除所有写作专用术语，支持通用多智能体协作
4. 工具描述与工具定义分离，避免描述过时

## What I already know

### 当前提示词文件

- `SwarmPromptTemplate.java` - 旧版写作模式（ROOT_TEMPLATE / SUB_TEMPLATE）
- `SwarmPromptTemplates.java` - 新版 Phase-Based 模式（COORDINATOR_TEMPLATE / WORKER_TEMPLATE）

### 当前 SwarmTools（已重构至 6 个工具）

```
create_worker  - 创建 Worker
delegate_task  - 派发任务
submit_result  - 提交结果
executeWorkflow - 执行工作流
send           - 发消息
self           - 自身信息
listAgents     - 列表
```

### 问题提示词片段（需清理）

```java
// SwarmPromptTemplates.java - COORDINATOR_TEMPLATE 中残留的写作工具引用：
"4. writing_session / writing_agent / writing_task / writing_result / writing_draft - 写作协作工具（可选）"

// SwarmPromptTemplate.java - ROOT_TEMPLATE 中：
"2. writing_agent(sessionId, role, description, skillTagsJson?, sortOrder?) - 动态创建子 Agent"
"7. writing_result(sessionId, taskId, writingAgentId, resultType, summary, content, structuredPayloadJson?) - 兼容旧格式"
```

## Architecture

### 设计原则

1. **配置驱动**：提示词模板存储在 `SwarmPromptTemplate` 实体（可运行时修改）
2. **角色过滤**：通过 `SwarmRole` 枚举定义每种角色的工具白名单
3. **模块化组合**：提示词由独立 section 组成，按角色选择性包含
4. **模板变量**：提示词中使用 `%s` / `{variable}` 占位，运行时替换

### 分层提示词结构

```
[Base Section]        ← 所有角色共享（身份信息、通信协议）
[Role Section]        ← 按角色包含（Coordinator: 调度规则 / Worker: 执行规则）
[Tool Section]        ← 按角色白名单生成（只列出该角色可用的工具）
[Custom Section]      ← 可选的附加提示（来自 SwarmWorkspace.customPrompt）
```

### 工具过滤逻辑

| 角色 | 可用工具 | 说明 |
|------|---------|------|
| `coordinator` | create_worker, delegate_task, send, self, listAgents | 调度者，不需要 submit_result/executeWorkflow |
| `worker` | submit_result, send, self | 执行者，不需要调度工具 |
| `root` | 所有 6 个工具 + executeWorkflow | 根 Agent，全功能 |
| `human` | 无工具 | 纯用户代理 |

### 目录结构（新增/修改）

```
ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/
├── prompt/
│   ├── SwarmPromptService.java          # [NEW] 提示词服务：加载、组合、变量替换
│   ├── SwarmPromptTemplate.java         # [REFACTOR] 移除硬编码，改为从 service 获取
│   ├── SwarmPromptTemplates.java        # [DELETE] 废弃
│   └── SwarmPromptSection.java          # [NEW] 提示词 section 枚举定义
├── tool/
│   └── SwarmToolFilter.java             # [NEW] 工具过滤：按角色返回白名单工具
└── runtime/
    └── SwarmTools.java                  # [MINOR] 增加 @Tool(description) 动态化

ai-agent-domain/src/main/java/com/zj/aiagent/domain/swarm/
├── entity/
│   └── SwarmPromptTemplate.java         # [NEW] 提示词模板实体（可选，支持 DB 存储）
└── valobj/
    └── SwarmRole.java                   # [REFACTOR] 增加 COORDINATOR / WORKER / ROOT 角色
```

## Prompt Section 设计

### Base Section（所有角色共享）

```
【身份信息】
- agent_id: {agentId}
- workspace_id: {workspaceId}
- 角色: {role}
- 描述: {description}
- 父 Agent ID: {parentAgentId}（无则为 null）

【通信协议】
所有 Agent 间通信使用 send(tool) 发送结构化消息。
禁止对 role=human 的 agent 使用 send（那是人类用户）。
```

### Coordinator Section

```
【核心职责】
你是 Coordinator（协调者），负责：
1. 分解复杂任务为独立子任务
2. 创建 Worker Agent 并派发任务
3. 整合 Worker 的执行结果
4. 向用户返回最终结果

你不是执行者，而是指挥官——专注于规划、调度和整合。

【工作模式】
使用 Phase 工作流：RESEARCH → SYNTHESIS → IMPLEMENTATION → VERIFICATION
- 并行派发独立任务，而非串行等待
- 优先在同一次协调中派发所有子任务
- 收集到足够结果后先落 draft，再回复用户

【Continue vs. Spawn 策略】
| 情况 | 策略 | 原因 |
| 调研命中精确文件 | Continue | 上下文有价值 |
| 调研广泛、实现狭窄 | Spawn | 避免探索噪声 |
| 验证他人代码 | Spawn | 避免偏见 |
```

### Worker Section

```
【核心职责】
你是 Worker（执行者），只负责执行 Coordinator 分配的单一任务。
完成后向 Coordinator 汇报结果。

【执行规则】
1. 先完成任务
2. 调用 submit_result 记录结果
3. 调用 send 向 Coordinator 汇报
4. 不要创建子 Agent、不要派发任务
5. 不要自己扩展任务范围
```

### Tool Section（动态生成）

```java
// 按角色白名单生成工具描述列表
// Coordinator: create_worker, delegate_task, send, self, listAgents
// Worker: submit_result, send, self
// Root: 所有 6 个 + executeWorkflow

【可用工具】
1. create_worker(role, description, taskUuid?, instruction?, expectedOutputSchema?) - 创建 Worker Agent
2. delegate_task(targetAgentId, taskUuid, instruction, expectedOutputSchema?) - 派发任务
3. submit_result(taskUuid, resultType, summary, content, metadata?) - 提交结果（Worker 专用）
4. send(agentId, message) - 发送消息
5. self() - 返回自身信息
6. listAgents() - 列出所有 Agent
7. executeWorkflow(agentId, input?) - 执行工作流

【工具调用格式】
- 工具参数必须是合法 JSON 对象
- 每次工具调用都必须一次性给出完整 JSON
- 工具参数第一字符必须是 {，最后字符必须是 }
```

## 实现步骤

### Phase 1: 数据结构定义

1. 在 `SwarmRole` 枚举增加 `COORDINATOR` / `WORKER` / `ROOT`
2. 创建 `SwarmToolFilter.java`：
   - 定义每种角色的工具白名单 Set
   - 提供 `getAllowedToolNames(role)` 方法
3. 创建 `SwarmPromptSection.java`：定义所有 section 枚举

### Phase 2: 提示词服务

1. 创建 `SwarmPromptService.java`：
   - `getPrompt(agent, role, workspace)` - 组合完整提示词
   - `buildToolSection(role)` - 按角色生成工具描述
   - `resolveVariables(template, context)` - 替换变量（agentId / workspaceId 等）
   - `composeSections(role, context)` - 按角色组合 section
2. 从 `SwarmPromptTemplates.java` 提取各 section 内容到 service

### Phase 3: 运行时集成

1. 修改 `SwarmAgentRunner.java`：
   - 调用 `SwarmPromptService.getPrompt()` 替代现有的 `String.format()` 调用
2. 修改 `SwarmTools.java`：
   - `@Tool(description = ...)` 中的工具描述改为动态获取
   - 增加工具过滤：Runner 初始化时只传递白名单工具

### Phase 4: 清理废弃内容

1. 删除 `SwarmPromptTemplates.java`
2. 从 `SwarmPromptTemplate.java` 删除所有 `writing_*` 工具引用
3. 删除所有 `ROOT_TEMPLATE` / `COORDINATOR_TEMPLATE` 常量

## Open Questions

### Q1: 提示词存储位置

**A. 硬编码常量（当前方式）**
- 优点：简单、无需 DB 变更
- 缺点：修改需重新编译

**B. 数据库表 `swarm_prompt_template`**
- 优点：支持运行时修改
- 缺点：增加复杂度、需要迁移

**C. 配置文件（YAML/JSON）**
- 优点：比 DB 简单、可版本控制
- 缺点：修改需重启

**推荐**：方案 C（配置文件）+ 接口编辑支持

### Q2: 工具过滤时机

**A. 构建时过滤（SwarmTools 实例化时）**
- 优点：简单、LLM 只能看到白名单工具
- 缺点：所有工具类实例需传递 role 参数

**B. 调用时过滤（ToolCallback 执行前）**
- 优点：灵活、可动态调整
- 缺点：LLM 可能看到不在白名单的工具

**推荐**：方案 A（构建时过滤），与 Claude-Code 的 `assembleToolPool()` 一致

### Q3: 向后兼容

当前已重构工具数量从 12 个到 6 个，是否需要保留旧版提示词模板？

**推荐**：不保留，直接迁移到新版动态系统

## Acceptance Criteria

- [ ] `SwarmRole` 枚举包含 COORDINATOR / WORKER / ROOT / HUMAN 角色
- [ ] `SwarmToolFilter` 提供 `getAllowedToolNames(role)` 方法
- [ ] `SwarmPromptService.getPrompt()` 返回组合后的提示词
- [ ] Coordinator 提示词中只包含调度工具描述
- [ ] Worker 提示词中只包含执行工具描述
- [ ] 所有 `writing_*` 工具引用从提示词中移除
- [ ] 提示词支持变量替换：`{agentId}` / `{workspaceId}` / `{role}` 等
- [ ] 单元测试覆盖 `SwarmPromptService` 核心方法
- [ ] 集成测试验证实际 Agent 运行时使用正确提示词

## Definition of Done

- [ ] 所有新增类/方法有 Javadoc 注释
- [ ] 单元测试覆盖核心逻辑
- [ ] Lint / CI green
- [ ] 更新相关文档（如果需要）

## Out of Scope

- 不改变 `SwarmTools` 的工具实现逻辑（已重构）
- 不改变数据库 Schema（提示词存代码或配置文件）
- 不改变前端 API 接口
- 不实现提示词的在线编辑器（仅支持配置文件方式）

## 关键文件参考

| Claude-Code 文件 | 对应 ai-agent 实现 |
|-----------------|-------------------|
| `src/constants/tools.ts` (COORDINATOR_MODE_ALLOWED_TOOLS) | `SwarmToolFilter.java` |
| `src/utils/systemPrompt.ts` (buildEffectiveSystemPrompt) | `SwarmPromptService.java` |
| `src/utils/swarm/teammatePromptAddendum.ts` | `SwarmPromptSection.java` (Base Section) |
| `src/utils/toolPool.ts` (applyCoordinatorToolFilter) | `SwarmAgentRunner.java` (工具过滤) |
