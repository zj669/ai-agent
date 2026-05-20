# 重构 WritingAgent — 合并到 SwarmAgent 消除冗余层

## Goal

消除 `writing_agent` 中间映射表，将 `sessionId` 和 `sortOrder` 直接合并到 `swarm_workspace_agent` 表。重构后协作面板状态和任务直接通过 `SwarmAgent` 查询，无需 `WritingAgent` 桥接。

## Background

当前架构中 `WritingAgent` 只做两件事：
1. 桥接 `swarmAgentId → sessionId`（让查询"某 Agent 属于哪个会话"不需要多表 Join）
2. 提供 `sortOrder` 控制卡片显示顺序

这两件事直接加到 `SwarmAgent` 上即可，`WritingAgent` 层完全冗余。去掉它可以：
- 简化数据模型，减少跨表查询
- 消除 `createWorkspace` 时需要为 Coordinator 创建占位 `WritingAgent` 的尴尬
- 让协作面板的状态跟踪直接绑定到 `SwarmAgent`

## Requirements

- [ ] **数据库变更**：在 `swarm_workspace_agent` 表增加 `session_id`、`sort_order` 字段
- [ ] **Entity 变更**：`SwarmAgent` 增加 `sessionId`、`sortOrder` 字段
- [ ] **删除 `writing_agent` 表及相关代码**：
  - 删除 `WritingAgent` entity、repository、service
  - 删除 `WritingAgentCoordinatorService` 中与创建/查询/更新 WritingAgent 相关的方法（只保留协调逻辑）
  - 删除前端 `types/swarm.ts` 中的 `WritingCollaborationCard` 等相关类型（改由后端直接返回 SwarmAgent + Task + Result 的合并投影）
- [ ] **更新 `SwarmAgentRunner`**：派发任务/更新状态时直接操作 `SwarmAgent`，不再经过 `WritingAgentCoordinatorService`
- [ ] **更新 `WritingProjectionService`**：查询协作卡片时直接 JOIN `SwarmAgent` + `WritingTask` + `WritingResult`，不再经过 `WritingAgent`
- [ ] **更新 `SwarmWorkspaceService.createWorkspace`**：创建 Workspace 时将 Coordinator 的 `sessionId` 和 `sortOrder` 直接写入 `SwarmAgent`
- [ ] **数据迁移**：提供 SQL 迁移脚本，将现有 `writing_agent` 数据合并到 `swarm_workspace_agent`
- [ ] **更新前端类型**：移除 `WritingCollaborationCard` 等类型，改用新的投影 DTO
- [ ] **更新 API 返回结构**：协作面板数据直接返回 `SwarmAgent[] + WritingTask[] + WritingResult[]` 的合并投影

## Acceptance Criteria

- [ ] 数据库表 `writing_agent` 可安全删除（确认无遗留引用）
- [ ] `SwarmAgent` 包含 `sessionId` 和 `sortOrder`，无需 `WritingAgent` 即可查询协作状态
- [ ] 协作面板正常显示 Agent 卡片状态（RUNNING/DONE 等）
- [ ] 子 Agent 提交结果后，协作面板能实时回显最新结果
- [ ] 单元测试通过（如果涉及）

## Technical Notes

### 新表结构（`swarm_workspace_agent` 增加字段）

```sql
ALTER TABLE swarm_workspace_agent
  ADD COLUMN session_id BIGINT(20) DEFAULT NULL COMMENT '所属写作会话 ID',
  ADD COLUMN sort_order INT(11) DEFAULT 0 COMMENT '协作面板排序',
  ADD INDEX idx_session_id (session_id);
```

### 数据迁移策略

```sql
-- 将 writing_agent 数据合并到 swarm_workspace_agent
UPDATE swarm_workspace_agent wa
  JOIN writing_agent w ON wa.id = w.swarm_agent_id
SET wa.session_id = w.session_id, wa.sort_order = w.sort_order;

-- 删除 writing_agent 表（确认迁移成功后执行）
DROP TABLE writing_agent;
```

### 核心查询变化

**Before（经过 WritingAgent 桥接）**：
```
SwarmAgent → WritingAgent (by swarmAgentId) → WritingTask (by writingAgentId) → WritingResult
```

**After（直接关联）**：
```
SwarmAgent (with sessionId) → WritingTask (by swarmAgentId) → WritingResult
```

### 涉及文件清单（共 20+ 文件）

#### Domain 层
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/swarm/entity/SwarmAgent.java` — 增加 `sessionId`、`sortOrder` 字段
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/entity/WritingAgent.java` — **删除**
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/entity/WritingTask.java` — 删除 `writingAgentId` 字段
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/entity/WritingResult.java` — 删除 `writingAgentId` 字段
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/swarm/repository/SwarmAgentRepository.java` — 增加 `findBySessionId()`
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/repository/WritingAgentRepository.java` — **删除**
- [ ] `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/repository/WritingTaskRepository.java` — 删除 `findByWritingAgentId()`

#### Infrastructure 层
- [ ] `01_init_schema.sql` — `swarm_workspace_agent` 增加字段；`writing_task`/`writing_result` 删除 `writing_agent_id` 列；**删除 `writing_agent` 表**
- [ ] `SwarmWorkspaceAgentPO.java` — 增加 `sessionId`、`sortOrder` 字段
- [ ] `SwarmAgentRepositoryImpl.java` — 增加 `findBySessionId()` 实现，更新 `toPO`/`toDomain`
- [ ] `WritingAgentRepositoryImpl.java` — **删除**
- [ ] `WritingTaskRepositoryImpl.java` — 删除 `findByWritingAgentId()`
- [ ] `WritingResultRepositoryImpl.java` — 删除 `writingAgentId` 相关代码
- [ ] `WritingAgentMapper.java` — **删除**

#### Application 层
- [ ] `SwarmAgentRuntimeService.java` — 移除 `WritingAgentCoordinatorService` 依赖
- [ ] `SwarmAgentRunner.java` — 所有 `writingAgentCoordinatorService.updateStatus()` 改为 `agentRepository.updateStatus()`
- [ ] `SwarmTools.java` — `create_worker` 不再创建 `WritingAgent`，直接记录 sessionId
- [ ] `SwarmWorkspaceService.java` — `createWorkspace` 时将 Coordinator 的 sessionId/sortOrder 写入 `SwarmAgent`
- [ ] `WritingAgentCoordinatorService.java` — **删除整个文件**
- [ ] `WritingProjectionService.java` — `listAgents()` 改为 `swarmAgentRepository.findBySessionId()`
- [ ] `WritingTaskService.java` — `createTask` 不再需要 writingAgentId 参数

#### Frontend 层
- [ ] `types/swarm.ts` — 删除 `WritingCollaborationCard`、`WritingSessionOverview` 等类型
- [ ] `pages/SwarmMainPage.tsx` — 更新 API 返回类型
- [ ] `components/panel/CollaborationPanel.tsx` — 适配新 DTO
- [ ] `api/swarmService.ts` — 适配新 API 返回

### 重构核心逻辑

#### 1. SwarmAgent 增加字段
```java
// SwarmAgent.java
private Long sessionId;    // 所属写作会话
private Integer sortOrder; // 排序
```

#### 2. create_worker 简化
```java
// SwarmTools.create_worker — Before: 调用 writingAgentCoordinatorService.createWritingAgent()
// SwarmTools.create_worker — After: 直接创建 SwarmAgent 并设置 sessionId
```

#### 3. 状态更新简化
```java
// SwarmAgentRunner — Before: writingAgentCoordinatorService.updateStatus(id, "RUNNING")
// SwarmAgentRunner — After: agentRepository.updateStatus(id, "RUNNING")
```

#### 4. 协作面板查询简化
```java
// WritingProjectionService — Before: listAgents() → writingAgentCoordinatorService.listAgents()
// WritingProjectionService — After: swarmAgentRepository.findBySessionId(sessionId)
```
