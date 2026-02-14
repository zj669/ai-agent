# Workflow 迁移完成矩阵 Blueprint

## 职责契约
- **做什么**: 给出 workflow 重构的模块级迁移状态、完成度与可执行下一步。
- **不做什么**: 不替代模块职责蓝图，不描述具体业务实现细节。

## 生效范围
- 生效目录：`E:\WorkSpace\repo\ai-agent\ai-agent-foward\src\components\workflow\**`
- 生效页面：`E:\WorkSpace\repo\ai-agent\ai-agent-foward\src\pages\WorkflowEditorPage.tsx`
- 生效蓝图：`workflow/*.md` + `pages/WorkflowEditorPage.md`

## 模块完成矩阵（按模块）
| 模块 | 目标结构 | 当前实现状态 | 完成度 | 下一步 |
|---|---|---|---|---|
| workflow root | 建立 `index/constants/types/style/custom-edge/custom-connection-line` | 目录与核心文件均已存在并由页面容器引用 | 100% | 维持与 agent.md 一致性巡检 |
| hooks | 拆分 5 个交互 hooks | 5 个 hooks 文件均已落地并完成主链路接线 | 95% | 将 hook 与 store slice 的事件映射补齐到蓝图 |
| store slices | 建立 workflow/layout/panel/history slices | 4 个 slice 文件与 store index 已落地并已接线 | 100% | 增加 slice 与事件映射说明 |
| nodes | 类型目录化节点（start/end/llm/tool/if-else） | 目录化节点已落地，含 default/types/schema/node/panel | 100% | 按新增节点类型继续扩展同模板 |
| panel | 节点库/节点配置/工作流配置统一面板层 | 当前以 `panel/index.tsx` 收口，子面板边界已定义 | 85% | 依据既定边界拆分为命名明确的子面板文件 |
| operator | 头部/控制/缩放组件 | `header/control/zoom` 已落地并接线 | 100% | 保持与页面装配层边界稳定 |
| features | debug-and-preview / run-history 子域 | 两个 feature 目录已建立，接口已导出 | 80% | 细化 feature 内部蓝图与状态契约 |
| utils | layout/validation/graph-transformer 纯函数层 | 核心工具文件已落地并接线 | 100% | 增补输入输出约束说明 |
| page 装配 | `WorkflowEditorPage` 仅装配 | 页面蓝图已改为装配层，工作流容器负责编排 | 100% | 仅维护装配契约，不回流业务逻辑 |

## 旧路径 -> 新路径 -> 当前实现状态
| 旧路径 | 新路径 | 当前实现状态 |
|---|---|---|
| `src/hooks/useWorkflowEditor.ts` | `src/components/workflow/hooks/use-workflow-interactions.ts` + `use-nodes-interactions.ts` + `use-edges-interactions.ts` + `use-workflow-history.ts` + `use-nodes-sync-draft.ts` | 已接线 |
| `src/components/WorkflowEdge.tsx` | `src/components/workflow/custom-edge.tsx` | 已接线 |
| `src/components/WorkflowNode.tsx` | `src/components/workflow/nodes/_base/node.tsx` + `nodes/*/node.tsx` | 已接线 |
| `src/components/WorkflowNodeLarge.tsx` | `src/components/workflow/nodes/*/node.tsx` | 已接线 |
| `src/components/NodePanel.tsx` | `src/components/workflow/panel/index.tsx` | 已建骨架 |
| `src/components/NodePropertiesPanel.tsx` | `src/components/workflow/panel/index.tsx`（后续拆分） | 已建骨架 |
| `src/components/WorkflowConfigPanel.tsx` | `src/components/workflow/panel/index.tsx`（后续拆分） | 已建骨架 |
| `src/components/NodeInlineConfigPanel.tsx` | `src/components/WorkflowNode.tsx`（节点内展开配置） | 已清理（未采用右侧面板方案） |
| `src/components/ExecutionLogPanel.tsx` | `src/components/workflow/features/run-history/index.ts` | 已建骨架 |
| `src/pages/WorkflowEditorPage.tsx`（含编排逻辑） | `src/pages/WorkflowEditorPage.tsx`（装配） + `src/components/workflow/index.tsx` | 已接线 |

## 冲突蓝图处置结论
- `components/WorkflowNodeLarge.md`：历史保留（非生效）。
- `components/NodePanel.md`：历史保留（非生效）。
- `hooks/useWorkflowEditor.md`：历史保留（非生效）。
- `services/workflowService.md`：历史保留（非生效，服务契约仍可参考）。
- `BLUEPRINT_REFACTORING.md`：历史保留（重构过程记录）。

## 验收口径
1. 唯一生效集合以 `frontend/_overview.md` 中“唯一生效蓝图集合”为准。
2. 迁移状态以本矩阵表格为准，不再以旧蓝图章节描述为准。
3. 旧蓝图保留仅用于历史追溯，不参与新改动设计决策。

## 变更摘要
- 新增模块级迁移完成矩阵，覆盖目标、状态、完成度、下一步。
- 新增旧路径到新路径的实现状态表，支持逐项验收。
- 补充 `NodeInlineConfigPanel.tsx` 去向，明确节点配置已收敛到节点内展开方案。
- 固化冲突蓝图处置结论，避免双轨文档歧义。

## 变更日志
- [2026-02-13] 新增迁移完成矩阵并与当前代码结构对齐。
- [2026-02-13] 同步节点内展开配置落地状态并记录未采用组件清理结果。
