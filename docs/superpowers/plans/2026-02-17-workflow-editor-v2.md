# Workflow Editor V2 实施计划（方案3：一次性重写）

> **For Claude:** REQUIRED: Use superpowers:subagent-driven-development to implement this plan in bounded chunks, and report progress after each chunk with verification evidence.

## Goal
在一次性切换窗口内完成 Workflow Editor V2 重写，替换旧编辑器核心实现；确保 `graphJson` 完全兼容，连线失败显式反馈，可通过统一门禁发布。

## Architecture
- 分层边界：`page / canvas / state / mapping / validation / panels / feedback`
- 状态设计：编辑器主状态机 + 连线子状态机
- 兼容策略：`graphJson <-> editorState` 双向映射 + ID 归一化 + 历史样本回放
- 风险控制：一次性切换门禁 + 可执行回退预案

## Tech Stack
- Frontend: React 19 + TypeScript + @xyflow/react + Zustand + Zod
- Test: Vitest / React Testing Library / Playwright（或现有 E2E 框架）
- Blueprint: `.blueprint/` 文档先行更新

---

## Chunk 1: 蓝图先行（Blueprint 更新与对齐）

### Task 1: 更新蓝图总览与 Workflow Editor V2 模块边界

**Files**
- **Modify:** `E:/WorkSpace/repo/ai-agent/.blueprint/_overview.md`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/.blueprint/frontend/workflow-editor/WorkflowEditorV2.md`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/.blueprint/frontend/workflow-editor/ValidationAndMapping.md`
- **Test:** `E:/WorkSpace/repo/ai-agent/.blueprint/frontend/workflow-editor/WorkflowEditorV2.md`

**Steps**
- [ ] 在 `_overview.md` 增加 Workflow Editor V2 索引入口与依赖边界说明。
- [ ] 新增/更新 `WorkflowEditorV2.md`，写明 page/canvas/state/panels/feedback 职责与边界。
- [ ] 新增/更新 `ValidationAndMapping.md`，定义统一校验入口、ID 归一化、graphJson 兼容契约。
- [ ] 在蓝图中记录“方案3一次性重写”与“graphJson 完全兼容”决策。

**验证命令与期望**
- 命令：`npm run lint --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward"`
- 期望：无新增 lint 错误（文档变更通常不影响代码，但用于守门）。

---

## Chunk 2: V2 状态层与校验层

### Task 2: 建立编辑器主状态机与连线子状态机

**Files**
- **Create:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/stores/workflowEditorV2Store.ts`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/types/workflowEditorV2.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/stores/__tests__/workflowEditorV2Store.test.ts`

**Steps**
- [ ] 定义主状态机状态与事件（BOOTSTRAPPING/READY/EDITING/VALIDATING/SAVING/SAVE_FAILED）。
- [ ] 定义连线子状态机状态与事件（IDLE/DRAGGING/CHECKING/COMMITTED/REJECTED）。
- [ ] 编写 store 原子动作：初始化、增删边、更新节点配置、dirty 标记、错误复位。
- [ ] 添加状态机流转单测（成功链路、失败回退链路）。

**验证命令与期望**
- 命令：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- workflowEditorV2Store`
- 期望：状态机流转测试全部通过。

### Task 3: 建立统一校验层 API

**Files**
- **Create:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/services/workflowEditorValidation.ts`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/types/workflowValidation.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/services/__tests__/workflowEditorValidation.test.ts`

**Steps**
- [ ] 实现 `validateConnection(...)`（禁止 silent return，输出错误码+消息）。
- [ ] 实现 `validateNodeConfig(...)`（节点必填/类型约束）。
- [ ] 实现 `validateBeforeSave(...)`（保存前基础校验聚合）。
- [ ] 建立校验矩阵单测：合法连线、重复边、handle 缺失、类型不匹配、必填缺失。

**验证命令与期望**
- 命令：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- workflowEditorValidation`
- 期望：校验层单测通过，失败场景均返回结构化错误。

---

## Chunk 3: Canvas 连线链路与显式反馈

### Task 4: 重建连线事件链路并强制 Handle 显式 id

**Files**
- **Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/components/workflow/WorkflowNode.tsx`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/components/workflow/WorkflowCanvasV2.tsx`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/hooks/useWorkflowConnect.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/components/workflow/__tests__/WorkflowCanvasV2.connect.test.tsx`

**Steps**
- [ ] 为所有 Source/Target Handle 添加稳定显式 `id`。
- [ ] 统一 `onConnectStart/onConnect/onConnectEnd` 事件流到 V2 action。
- [ ] 接入 `validateConnection(...)`，失败时禁止状态变更。
- [ ] 输出失败反馈事件（toast/inline），并覆盖关键错误场景测试。

**验证命令与期望**
- 命令：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- WorkflowCanvasV2.connect`
- 期望：连线成功可提交，失败路径均有可见反馈。

---

## Chunk 4: Mapping 兼容层（graphJson 完全兼容）

### Task 5: 实现 graphJson 双向映射与 ID 归一化

**Files**
- **Create:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/services/workflowEditorMapping.ts`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/types/workflowGraphJson.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/services/__tests__/workflowEditorMapping.test.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/services/__tests__/workflowEditorCompatibilityReplay.test.ts`

**Steps**
- [ ] 实现 `deserializeGraphJson(...)`：输入兼容解析 + ID 归一化。
- [ ] 实现 `serializeEditorState(...)`：输出保持现有 `graphJson` 协议。
- [ ] 增加 round-trip 测试：反序列化 -> 序列化 -> 关键语义一致。
- [ ] 加入历史样本回放测试集（至少覆盖复杂分支、条件节点、异常样本）。

**验证命令与期望**
- 命令：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- workflowEditorMapping workflowEditorCompatibilityReplay`
- 期望：兼容性回放通过率 100%，关键字段无丢失。

---

## Chunk 5: 属性面板联动与页面替换

### Task 6: 完成属性面板联动（选中态 -> 编辑 -> 回写）

**Files**
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/components/workflow/panels/NodeConfigPanelV2.tsx`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/hooks/useNodeConfigPanel.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/components/workflow/panels/__tests__/NodeConfigPanelV2.test.tsx`

**Steps**
- [ ] 从 store 读取选中节点快照并渲染面板。
- [ ] 表单提交走 `updateNodeConfig` + `validateNodeConfig`。
- [ ] 非法输入展示可见错误，且不 silent 丢弃。
- [ ] 增加联动测试：选中切换、字段修改、错误提示与状态一致性。

**验证命令与期望**
- 命令：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- NodeConfigPanelV2`
- 期望：面板编辑路径全部通过，错误反馈可见。

### Task 7: 页面切换到 V2 并替换旧实现入口

**Files**
- **Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/pages/WorkflowEditorPage.tsx`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/pages/WorkflowEditorV2Page.tsx`
- **Modify:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/hooks/useWorkflowEditor.ts`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/src/pages/__tests__/WorkflowEditorV2Page.integration.test.tsx`

**Steps**
- [ ] 在页面层接入 V2 初始化链路（load -> mapping -> state）。
- [ ] 在保存入口接入 `validateBeforeSave`，通过后再调用保存。
- [ ] 将默认路由入口切到 V2，保留旧实现可回退入口。
- [ ] 补充集成测试：加载、编辑、保存、失败反馈。

**验证命令与期望**
- 命令：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- WorkflowEditorV2Page.integration`
- 期望：页面主链路稳定，旧入口可控保留。

---

## Chunk 6: 测试收敛、一次性切换门禁与回退演练

### Task 8: 门禁验证与切换检查清单

**Files**
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/docs/superpowers/specs/2026-02-17-workflow-editor-v2-design.md`
- **Create/Modify:** `E:/WorkSpace/repo/ai-agent/docs/superpowers/plans/2026-02-17-workflow-editor-v2.md`
- **Test:** `E:/WorkSpace/repo/ai-agent/ai-agent-foward/e2e/workflow-editor-v2.e2e.spec.ts`

**Steps**
- [ ] 执行前端 lint + typecheck + 单测全量。
- [ ] 执行 Workflow Editor V2 交互/E2E 套件。
- [ ] 执行兼容性回放并记录通过率（目标 100%）。
- [ ] 按门禁清单给出 Go/No-Go 结论与回退触发条件。

**验证命令与期望**
- 命令1：`npm run lint --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward"`
- 期望1：通过，无新增 lint 错误。
- 命令2：`npm run typecheck --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward"`
- 期望2：通过，无新增类型错误。
- 命令3：`npm run test --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward"`
- 期望3：通过，关键测试覆盖连线失败显式反馈与兼容性回放。
- 命令4：`npm run e2e --prefix "E:/WorkSpace/repo/ai-agent/ai-agent-foward" -- workflow-editor-v2`
- 期望4：主流程与失败流程均通过。

---

## 提交建议步骤（仅文本建议，不在本计划内执行）
1. 按 Chunk 完成后逐块提交，确保每个提交可独立回滚。
2. 提交信息建议包含：`feat(workflow-editor): v2 rewrite chunk-x`。
3. 合并前附门禁报告：测试结果、兼容回放结果、回退演练记录。

## 验收标准（整体）
- 已完成 Blueprint 先行更新并通过评审。
- V2 状态层、校验层、连线链路、映射层、面板联动全部落地。
- 默认页面入口切至 V2，旧实现可回退。
- 门禁全通过：lint/typecheck/unit/interaction/e2e/compat replay。
- `graphJson` 完全兼容（读写与语义一致）。
