# 重构多智能体写作工具 → 通用多智能体协作框架

## 状态

**当前阶段**: 实现完成

## 已确认的实现决策（2026-04-01）

| 决策 | 选项 | 选择 |
|------|------|------|
| 实体重命名 | 仅重构 SwarmTools / 包含领域实体重命名 | **仅重构 SwarmTools**（Out of Scope 优先） |
| 工具数量 | 12个 / 6个 / 3个 | **3个核心 + 3个保留** |
| 新工具实现 | 独立实现 / 复用旧服务 | **复用底层 WritingSessionService 等** |

### 最终工具清单（6个）

| # | 工具名 | 角色 | 内部实现 |
|---|--------|------|----------|
| 1 | `create_worker` | Coordinator | AgentApplicationService + WritingAgentCoordinatorService |
| 2 | `delegate_task` | Coordinator | WritingTaskService + send 消息 |
| 3 | `submit_result` | Worker | WritingResultService.recordTaskResultByUuid |
| 4 | `executeWorkflow` | Coordinator/Worker | SchedulerService（保留） |
| 5 | `send` | Coordinator/Worker | MessageService（保留） |
| 6 | `self` | Coordinator/Worker | SwarmAgentRepository（保留） |
| 6 | `listAgents` | Coordinator/Worker | WorkspaceService（保留） |

### 修改的文件

- `SwarmTools.java` - 重写，12→6个工具
- `SwarmAgentRuntimeService.java` - 移除 writingDraftService 参数
- `SwarmAgentRunner.java` - 更新 SUB_AGENT_FORBIDDEN_TOOLS

### 删除的工具（不再暴露给 LLM）

- `createAgent` → 已被 `create_worker` 替代
- `writing_session` → 已被 `create_worker/delegate_task` 替代
- `writing_agent` → 已被 `create_worker` 替代
- `writing_task` → 已被 `delegate_task` 替代
- `writing_result` → 已被 `submit_result` 替代
- `writing_result_by_task` → 已被 `submit_result` 替代
- `writing_result_by_task_uuid` → 已被 `submit_result` 替代
- `writing_draft` → 已移除（未在核心流程中使用）

## Goal

将现有的12个工具从"写作专用"重构为**通用多智能体协作框架**：
- 抽象掉写作场景依赖
- 保留通用Agent协作能力
- 参考Claude-Code的dispatch+worker模式
- 工具数量：12个 → 5-6个核心工具

## What I already know

### 当前工具清单（12个）

**通用Agent管理（5个）：**
1. `createAgent` - 创建可执行工作流的Agent
2. `executeWorkflow` - 执行工作流Agent并等待结果
3. `send` - 向指定Agent发送消息
4. `self` - 返回自身信息
5. `listAgents` - 列出所有Agent

**写作会话管理（3个）：**
6. `writing_session` - 记录写作任务会话
7. `writing_agent` - 创建写作子Agent
8. `writing_task` - 记录主Agent拆分的子任务

**写作结果记录（3个）：**
9. `writing_result` - 记录任务结果（含sessionId/taskId/writingAgentId）
10. `writing_result_by_task` - 按taskId记录结果
11. `writing_result_by_task_uuid` - 按taskUuid记录结果（强烈推荐）

**写作草稿管理（1个）：**
12. `writing_draft` - 保存主Agent汇总的草稿版本

### 冗余分析

1. **结果记录冗余**：`writing_result`/`writing_result_by_task`/`writing_result_by_task_uuid` 三个工具做同一件事，只是ID查找方式不同
2. **ID类型混用**：sessionId/writingAgentId/swarmAgentId 容易混淆
3. **职责不够清晰**：通用Agent工具和写作专用工具混在一起

### Claude-Code参考模式

从 `.claude/agents/` 目录看到的多智能体模式：
- **dispatch** - 纯调度器，按阶段顺序调用子Agent
- **plan** - 分析需求并规划
- **research** - 研究代码库
- **implement** - 实现功能
- **check** - 检查质量
- **debug** - 调试修复

特点：职责单一、工具简洁、流程清晰

## Assumptions (temporary)

- 工具重构后仍需要支持现有的 WritingSession/WritingTask/WritingResult/WritingDraft 领域实体
- 保持向后兼容，前端API接口不变
- 工具数量目标：从12个减少到6-8个

## Open Questions

### Q1: 协作模式选择（关键）
当前有2种模式各有优劣，请选择：

**A. Coordinator + Workers（类Claude-Code）**
```
用户 → Coordinator（规划）→ Workers（执行）→ Coordinator（整合）→ 用户
```
- 1个Coordinator + N个Workers
- Coordinator负责任务分解和结果整合
- Workers只管执行
- **工具**: create_worker, send_task, submit_result, list_workers, get_context

**B. Peer-to-Peer（对等协作）**
```
Agent A ↔ Agent B ↔ Agent C
```
- 所有Agent平等
- 通过消息传递协作
- 更灵活但协调更复杂
- **工具**: create_agent, send_message, get_context, execute_task

**C. Pipeline（流水线）**
```
Agent A → Agent B → Agent C → 结果
```
- 每个Agent处理后传递给下一个
- 适合固定流程
- **工具**: create_agent, set_next, execute_pipeline, get_result

**你的偏好是？**

## Requirements (evolving)

* 协作模式：**Coordinator + Workers**
* 存储抽象：**完全通用化**
* 前端API：**需要适配变化**

### 决策已确定

- [x] 协作模式: Coordinator + Workers（类Claude-Code）
- [x] 存储抽象: 完全通用化
- [x] 核心工具: **3个**
- [x] 命名风格: CollaborationSession/Task/Result/TeamMember

### 核心工具清单（最终版）

| # | 工具名 | 角色 | 职责 | 参数 |
|---|--------|------|------|------|
| 1 | `create_worker` | Coordinator | 创建Worker | `role`, `description` |
| 2 | `delegate_task` | Coordinator | 派发任务 | `targetAgentId`, `taskUuid`, `instruction`, `expectedOutput?` |
| 3 | `submit_result` | Worker | 提交结果 | `taskUuid`, `resultType`, `summary`, `content`, `metadata?` |

> Worker状态通过`send`消息传递获取，不单独设工具

### 领域实体重命名

| 当前 | 新名称 | 用途 |
|------|--------|------|
| `WritingSession` | `CollaborationSession` | 协作会话 |
| `WritingTask` | `Task` | 任务 |
| `WritingResult` | `Result` | 结果 |
| `WritingAgent` | `TeamMember` | 团队成员 |
| `WritingDraft` | `Draft` | 草稿（可选保留） |

### 简化对比

| 维度 | 重构前 | 重构后 |
|------|--------|--------|
| 工具数量 | 12个 | 3个 |
| 命名风格 | writing_* | 通用协作 |
| 场景绑定 | 写作专用 | 完全通用 |
| 模式抽象 | 无 | Coordinator/Worker |

## Research Notes

### How tools are currently used (from prompt templates)

**Coordinator (协调者) uses:**
- `createAgent` - 创建 Worker Agent
- `executeWorkflow` - 执行工作流 Agent
- `send` - 给 Worker 派发任务
- `writing_session / writing_agent / writing_task / writing_result / writing_draft` - 写作协作工具
- `self()` - 返回自身信息
- `listAgents()` - 列出当前所有 Agent

**Worker (执行者) uses:**
- `executeWorkflow` - 执行工作流
- `send` - 向 Coordinator 返回结果
- `writing_result_by_task_uuid` - 记录任务结果（强烈推荐）
- `writing_result_by_task` - 记录任务结果
- `self()` - 返回自身信息

### Key observations

1. **实际只用了8个核心工具**：prompt模板中明确说明了Worker优先使用 `writing_result_by_task_uuid`
2. **MCP工具已采用适配器模式**：`McpToolCallbackAdapter` 统一将外部工具转换为 `ToolCallback`
3. **Claude-Code模式**：dispatch+plan+research+implement+check 的职责链非常清晰
4. **Phase-Based工作流**：Coordinator负责规划，Worker负责执行

### Redundancy issues

| 冗余 | 问题 |
|------|------|
| `writing_result` + `writing_result_by_task` + `writing_result_by_task_uuid` | 三个工具做同一件事，只是ID查找方式不同 |
| `writing_session/writing_agent/writing_task` | 可以合并为更简洁的写作会话API |
| `self` + `listAgents` | 都是有用的，但可以精简描述 |

## Proposed Refactoring (Draft)

### Option A: 合并同类工具（推荐）

**通用工具（4个）：**
1. `create_agent` - 创建子Agent（合并createAgent）
2. `execute_workflow` - 执行工作流
3. `send_message` - 发送消息（重命名send）
4. `get_context` - 获取上下文（合并self+listAgents）

**写作工具（4个）：**
5. `start_writing_session` - 开始写作会话（合并session+agent）
6. `create_task` - 创建任务（合并writing_task）
7. `submit_result` - 提交结果（合并3个result工具）
8. `save_draft` - 保存草稿

**总计：8个工具**

### Option B: 职责分层

**Coordinator专用（5个）：**
- `create_worker` / `send_task` / `list_workers` / `get_status` / `save_draft`

**Worker专用（3个）：**
- `submit_result` / `send_report` / `get_task`

**总计：8个工具，按角色分配**

## Acceptance Criteria (evolving)

* [ ] 工具数量从12个减少到6-8个
* [ ] 保留核心功能：创建Agent、发送消息、记录结果
* [ ] 向后兼容现有API

## Definition of Done (team quality bar)

* Tests added/updated
* Lint / typecheck / CI green
* 文档/注释更新

## Out of Scope (explicit)

* 不改变底层领域实体（WritingSession/WritingTask等）
* 不改变前端API接口
* 不改变数据库Schema

## Technical Notes

### Files to examine:
- `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/runtime/SwarmTools.java` - 当前12个工具，需重写
- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/entity/` - 需重命名
  - `WritingSession.java` → `CollaborationSession.java`
  - `WritingTask.java` → `Task.java`
  - `WritingResult.java` → `Result.java`
  - `WritingAgent.java` → `TeamMember.java`
  - `WritingDraft.java` → `Draft.java`
- `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/prompt/SwarmPromptTemplates.java` - 需更新为通用协作引导
- `ai-agent-application/src/main/java/com/zj/aiagent/application/writing/` - 需重命名包

### 实现步骤建议

1. **Phase 1**: 重命名领域实体（Writing* → Collaboration*）
   - 重命名domain层entity
   - 重命名repository
   - 更新infrastructure层mapper/po
   - 更新application层service

2. **Phase 2**: 重写SwarmTools.java
   - 删除12个旧工具
   - 实现3个新工具: create_worker, delegate_task, submit_result

3. **Phase 3**: 更新Prompt模板
   - SwarmPromptTemplates改为通用协作引导

4. **Phase 4**: 前端适配
   - 更新TypeScript类型定义
   - 更新API服务

### Constraints:
- Spring AI Tool注解方式
- 向后兼容考虑：可保留旧的工具方法作为deprecated
- 保持send消息传递机制
