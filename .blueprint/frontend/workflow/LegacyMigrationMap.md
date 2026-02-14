# Workflow 旧文件删除映射 Blueprint

## 职责契约
- **做什么**: 维护“旧路径 -> 新模块路径”的唯一迁移映射，用于删除旧实现与验收。
- **不做什么**: 不描述具体实现代码，不替代模块职责蓝图。

## 删除映射表（旧路径 -> 新模块路径）
| 旧路径 | 新模块路径 | 迁移说明 |
|---|---|---|
| `ai-agent-foward/src/hooks/useWorkflowEditor.ts` | `workflow/hooks/use-workflow-interactions.ts` + `use-nodes-interactions.ts` + `use-edges-interactions.ts` + `use-workflow-history.ts` + `use-nodes-sync-draft.ts` | 单体 Hook 拆分为 5 个职责 Hook |
| `ai-agent-foward/src/components/WorkflowEdge.tsx` | `workflow/custom-edge.tsx` | 边渲染与边交互统一入口 |
| `ai-agent-foward/src/components/WorkflowNode.tsx` | `workflow/nodes/_base/node.tsx` + `workflow/nodes/*/node.tsx` | 节点基类与类型节点分离 |
| `ai-agent-foward/src/components/WorkflowNodeLarge.tsx` | `workflow/nodes/*/node.tsx` | 尺寸分叉并入类型目录化节点 |
| `ai-agent-foward/src/components/NodePanel.tsx` | `workflow/panel/node-library-panel.tsx` | 节点库面板迁移至 panel 子域 |
| `ai-agent-foward/src/components/NodePropertiesPanel.tsx` | `workflow/panel/node-config-panel.tsx` | 节点配置面板统一命名 |
| `ai-agent-foward/src/components/WorkflowConfigPanel.tsx` | `workflow/panel/workflow-config-panel.tsx` | 工作流配置面板归并 |
| `ai-agent-foward/src/components/ExecutionLogPanel.tsx` | `workflow/features/run-history/log-panel.tsx` | 运行日志进入 features 子域 |
| `ai-agent-foward/src/pages/WorkflowEditorPage.tsx`（含编排逻辑） | `pages/WorkflowEditorPage.tsx`（装配） + `workflow/index.tsx`（容器） | 页面逻辑剥离到 workflow 域 |

## 删除前置条件
1. 新模块已在蓝图定义且具备等价职责。
2. 页面入口已切换至 `workflow/index.tsx` 装配。
3. 历史、连线、同步链路均由新 hooks 接管。
4. 回归通过拖拽、连线、撤销重做、自动布局、运行联调。

## 删除顺序建议
1. 删除单体 Hook（`useWorkflowEditor.ts`）
2. 删除 edge/node 旧组件
3. 删除 panel 旧组件
4. 删除 features 旧组件
5. 最后清理页面内遗留编排代码

## 风险与回退
- 风险：删除顺序错误导致功能断链。
- 回退策略：按映射表逐项替换，确保每步有新模块托底后再删除旧文件。

## 变更摘要
- 新增独立迁移映射蓝图，统一记录旧文件删除去向。
- 明确删除前置条件与推荐顺序，降低重构断链风险。
- 将“删除映射”从结构蓝图中解耦，便于独立验收。

## 变更日志
- [2026-02-13] 新增旧路径删除映射与迁移策略。
