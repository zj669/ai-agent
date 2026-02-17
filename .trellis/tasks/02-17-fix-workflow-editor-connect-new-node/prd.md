# 修复工作流编辑器新增节点后无法连线

## Goal
修复 Workflow 编辑页中的连线故障，确保在新增节点后，原有节点与新节点之间可以正常建立连线，同时保持现有 START/END 约束不变。

## Requirements
- 复现并定位“新增节点后无法与原有节点连线”的根因。
- 在不改变页面分层职责的前提下修复连线逻辑（页面编排 + hook 映射边界保持不变）。
- 对 graphJson -> ReactFlow 映射链路进行最小修复，保证节点 ID 与边 source/target 的一致性。
- 保持现有连接合法性约束：END 不能作为 source，START 不能作为 target。
- 同步更新相关 Blueprint 文档，保持蓝图与代码一致。

## Acceptance Criteria
- [ ] 从历史已有节点拖拽连线到新加节点可以成功创建边。
- [ ] 从新加节点拖拽连线到历史已有节点可以成功创建边。
- [ ] START/END 连线限制仍然生效（END 不能出边，START 不能入边）。
- [ ] 不引入新的数据结构或跨层调用，仍由 useWorkflowEditor 负责映射边界。
- [ ] Blueprint 与代码同步更新，契约描述与实现一致。

## Technical Notes
- 修复优先在 `ai-agent-foward/src/hooks/useWorkflowEditor.ts` 的映射函数处理 ID 归一化。
- 页面层 `ai-agent-foward/src/pages/WorkflowEditorPage.tsx` 仅保留连线编排与门禁校验，必要时补充归一化比较。
- 遵循 KISS：只修复连线失败相关路径，不扩展 undo/redo、多选等非本次范围功能。
