## Metadata
- file: `.blueprint/frontend/_overview.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
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
- 功能含义: 根据领域名称解析出需要阅读的蓝图文件集合。用于 Blueprint-First 协议的 Step 1，帮助开发者快速定位相关架构文档。映射规则：`workflow` → `[WorkflowEditorPage.md, useWorkflowEditor.md, workflowService.md, NodePanel.md, WorkflowNodeLarge.md]`
- 链路作用: 蓝图索引入口，支持按领域快速导航到相关文档，确保修改前先理解架构边界。

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


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，定义蓝图索引、文档角色分类、更新顺序验证的完整签名与语义，支持 Blueprint-First 协议的工具化实现。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
