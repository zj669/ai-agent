## Metadata
- file: `.blueprint/frontend/_overview.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: _overview
- 该文件用于描述 _overview 的职责边界与协作关系。

## 2) 核心方法
- `resolveBlueprintSet(domain)`
- `classifyDocumentRole(filePath)`
- `validateUpdateOrder(changeScope)`

## 3) 具体方法
### 3.1 resolveBlueprintSet(domain)
- 函数签名: `resolveBlueprintSet(domain: string): string[]`
- 入参: `domain` 为领域名称（如 "workflow", "agent", "chat", "knowledge"）
- 出参: 返回该领域相关的蓝图文件路径数组
- 功能含义: 根据领域名称解析出需要阅读的蓝图文件集合。用于 Blueprint-First 协议的 Step 1，帮助开发者快速定位相关架构文档。
- MVP workflow 映射规则（精简后）：
  - `workflow` → `[pages/WorkflowEditorPage.md, hooks/useWorkflowEditor.md, services/workflowService.md, components/NodePanel.md, components/WorkflowNodeLarge.md, workflow/TargetStructure.md, workflow/LayeredModules.md, workflow/HooksAndStoreSlices.md, workflow/MigrationCompletionMatrix.md, workflow/LegacyMigrationMap.md]`
- 链路作用: 蓝图索引入口，确保修改前理解当前 MVP 范围边界。

### 3.2 classifyDocumentRole(filePath)
- 函数签名: `classifyDocumentRole(filePath: string): 'page' | 'hook' | 'service' | 'component' | 'store' | 'type'`
- 入参: `filePath` 为蓝图文件路径（如 `.blueprint/frontend/pages/WorkflowEditorPage.md`）
- 出参: 返回文档角色分类标签
- 功能含义: 根据文件路径自动识别文档在前端架构中的角色层级。用于验证依赖方向（page → hook → service，component → hook）和职责边界。
- 链路作用: 架构分层验证器，确保蓝图文档的组织结构符合前端分层规范。

### 3.3 validateUpdateOrder(changeScope)
- 函数签名: `validateUpdateOrder(changeScope: { pages?: string[], hooks?: string[], services?: string[], components?: string[] }): { valid: boolean, errors: string[] }`
- 入参: `changeScope` 包含本次修改涉及的各层文件列表
- 出参: 返回验证结果，包含是否合法和错误信息数组
- 功能含义: 验证蓝图更新顺序是否符合依赖规则（先更新底层 service/hook，再更新上层 page/component）。防止出现"页面已更新但依赖的 Hook 蓝图未同步"的不一致状态。
- 链路作用: Blueprint-First 协议的 Step 2 守护者，确保架构推演的顺序正确性。


## 4) 关键协作契约（MVP声明）
- 当前前端蓝图已按 MVP 目标收敛，聚焦：拖拉拽编辑、卡片配置、graphJson 对接、metadata 驱动。
- 以下能力明确不纳入 MVP：快捷键、撤销重做、多选框选、右键菜单、协作态、执行态 SSE。
- 修改 workflow 相关代码前，必须先阅读 workflow 领域全部蓝图（见 resolveBlueprintSet 映射）。
- workflow 索引必须与 `.blueprint/frontend/workflow/` 现存蓝图保持一一可达，增删文件需同步更新映射。

## 5) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构。
- 2026-02-16: 按 MVP 目标更新 workflow 领域映射，补充分层结构蓝图，明确非 MVP 能力边界。
- 2026-02-16: 修复必修问题：补全 workflow 索引文件集合，避免协议阅读漏项。

## 6) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
