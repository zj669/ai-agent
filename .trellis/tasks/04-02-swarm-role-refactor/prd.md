# 重构 Swarm Agent 角色模型 — 移除 HUMAN Agent

## Goal

移除 Swarm 系统中的 `role=human` Agent 模型，对齐 Claude-Code 的 Coordinator/Worker 架构。初始 Agent 直接服务用户（COORDINATOR），而非作为 human 的子节点（ROOT）。

## 背景与问题

当前设计将人类建模为 Swarm 系统内的 Agent（role=human），初始 Agent 因 parent 为 human 而被判定为 ROOT，拥有全部工具，其中包括 `submit_result`。这导致：

1. **语义错误**：初始 agent 不是 Worker，不需要 `submit_result`
2. **humanAgentId 弥漫性污染**：大量代码传递 humanAgentId，却从未真正使用
3. **对不齐 Claude-Code**：Claude-Code 中人类是外部用户，初始 Agent 直接与用户交互 = COORDINATOR

## 需求

### R1: 移除 HUMAN 角色
- `SwarmRole` 枚举移除 `HUMAN`
- `SwarmToolFilter` 移除 `HUMAN` 分支
- `SwarmPromptSection` 移除"禁止对 role=human 使用 send"规则

### R2: 初始 Agent = COORDINATOR（不再是 ROOT）
- `SwarmWorkspaceService.createWorkspace()` 不再创建 human agent
- 初始 agent 的 parentId = null（表示直接服务用户）
- `SwarmAgentRuntimeService` 中 isRoot 判断改为 `parentId == null`
- `SwarmAgentRunner.resolveSwarmRole`：
  - `parentId == null` → `SwarmRole.COORDINATOR`
  - `parentId != null` → `SwarmRole.WORKER`
  - 移除 `isRoot` 逻辑

### R3: 移除 humanAgentId
- `SwarmPromptService` 移除 `humanAgentId` 参数
- `SwarmAgentRunner` 移除 `humanAgentId` 字段
- `SwarmAgentRuntimeService` 移除 humanAgentId 查询逻辑
- `WritingSessionService` 移除 `humanAgentId` 字段
- `WritingSessionPO` 移除 `humanAgentId` 字段
- `WritingSessionSummaryDTO` 移除 `humanAgentId` 字段
- `WorkspaceDefaultsDTO` 移除 `humanAgentId` 字段
- 数据库 schema 移除 `human_agent_id` 列（如有）
- 相关测试文件同步更新

### R4: 清理 SwarmPromptSection 提示词
- `BASE` Section：移除"父 Agent ID: null"等不再适用的描述
- `COORDINATOR` Section：描述为"直接服务用户的协调者"
- 所有 Section：移除"禁止对 role=human 的 agent 使用 send"

### R5: ROOT_TOOLS 不含 submit_result
- `SwarmToolFilter.ALL_TOOLS`（原 ROOT_TOOLS）移除 `TOOL_SUBMIT_RESULT`
- COORDINATOR 工具集 = create_worker / delegate_task / send / self / listAgents / executeWorkflow

## Acceptance Criteria

- [ ] `SwarmRole.HUMAN` 已移除
- [ ] 初始 Agent 启动时调用 `getCoordinatorPrompt`，拥有 COORDINATOR 工具集（不含 submit_result）
- [ ] 所有 `humanAgentId` 字段/参数已移除，编译通过
- [ ] `SwarmPromptSection` 中无"human"字样
- [ ] 单元测试通过：`SwarmToolFilterTest`、`SwarmPromptServiceTest`
- [ ] 原有 swarm 启动流程不受影响（workspace → initial agent → coordinator prompt）

## Technical Notes

- **SwarmRole 语义变更**：ROOT 角色完全移除，由 COORDINATOR 替代
- **影响范围**：domain 层（SwarmRole）、application 层（多个 Service）、infrastructure 层（PO）、interfaces 层（DTO）
- **向后兼容**：`WritingSession` 等已有数据中的 `humanAgentId` 字段可保留但设为 null，或通过数据迁移清理
