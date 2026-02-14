# WorkflowEditorPage Blueprint（装配层 / 画布壳层）

## 职责契约
- **做什么**: 作为页面路由入口完成参数解析、初始数据拉取、ReactFlow 画布壳层装配与页面级工具栏编排。
- **不做什么**: 不实现领域执行策略，不新增后端交互协议，不承担跨页面共享状态管理。

## 页面职责范围
| 责任 | 说明 |
|---|---|
| 路由参数处理 | 读取 `agentId/workflowId` 与用户上下文 |
| 初始数据装配 | 获取 workflow graph 草稿并注入编辑器 hook |
| 画布壳层装配 | 挂载 ReactFlow、自定义 nodeTypes/edgeTypes、背景网格与浮层 |
| 页面级动作 | 处理保存、运行、停止、回退、快捷键等页面动作 |

## UI 壳层目标（Dify/ComfyUI 风格）
1. 全屏无限画布（ReactFlow）。
2. 点阵背景：`<Background variant="dots" gap={12} size={1} />`。
3. 左侧浮动配置卡（半透明 + blur）。
4. 底部浮动 Dock 节点栏（点击快速添加节点，不使用基础 HTML 拖拽 API）。
5. 自定义白卡节点（Header 图标 + Body 输入框样式）。
6. 平滑曲线边（`smoothstep`）+ 淡紫色 + 选中高亮。

## 非职责（强约束）
- 不引入新的 workflow 领域模型。
- 不改写 execution/event 协议。
- 不在页面内实现后端 DSL 语义转换规则。

## 依赖拓扑
- 上游：Router、AuthStore。
- 下游：`useWorkflowEditor`、`WorkflowNode`、`WorkflowEdge`、`WorkflowConfigPanel`、`NodeQuickAddRail`。
- 间接依赖：`agentService`、`workflowService`（经 hook 调用）。

## 装配流程（四步）
1. 解析路由参数与用户上下文。
2. 拉取并加载 graph 到 `useWorkflowEditor`。
3. 装配 ReactFlow 与浮层（left panel / bottom dock）。
4. 响应保存、执行、停止与快捷键行为。

## 与 TargetStructure 对齐说明
- 页面作为壳层，节点渲染与边渲染由组件模块承担。
- 交互主链复用 `useWorkflowEditor` 及 workflow hooks，不新增平行状态源。
- 样式主要落在 Tailwind，`workflow-enhanced.css` 仅保留必要补充。

## 迁移落点
- 页面布局：`ai-agent-foward/src/pages/WorkflowEditorPage.tsx`
- 自定义节点：`ai-agent-foward/src/components/WorkflowNode.tsx`
- 自定义边：`ai-agent-foward/src/components/WorkflowEdge.tsx`
- 左侧浮层：`ai-agent-foward/src/components/WorkflowConfigPanel.tsx`
- 底部 Dock：`ai-agent-foward/src/components/NodeQuickAddRail.tsx`
- 样式补充：`ai-agent-foward/src/styles/workflow-enhanced.css`

## 验收标准
- 页面为全屏画布，且不再使用左右固定分栏。
- 左侧为 floating panel，底部为 floating dock。
- 节点添加方式不依赖基础 HTML 拖拽 API。
- 节点视觉为白卡 `rounded-2xl shadow-md border-gray-200`。
- 连线为淡紫 smoothstep，选中时明显高亮。
- 点击节点可在节点内部展开配置表单，并实时更新节点数据。
- 页面提供 graphJson 保存与加载入口，加载时可恢复画布节点与连线。
- 页面提供模拟运行入口，节点状态可按 `RUNNING/SUCCEEDED/FAILED` 变化。
- `npm run build` 可通过。

## 变更摘要
- 页面布局从“三栏固定布局”重构为“全屏画布 + 双浮层（左 panel / 底 dock）”。
- 统一节点外观为 Dify/ComfyUI 风格白卡，节点主体改为输入框式内容区。
- 连线统一为 smoothstep 淡紫色风格，补齐 hover/selected 高亮语义。
- 新增节点点击展开能力：通过 `selectedNodeId` 注入 `isExpanded/onToggleExpand/onUpdateNodeData` 到节点数据。
- 新增页面级加载与模拟执行动作，补齐“保存/加载 + 运行态可视化”闭环。
- `workflow-enhanced.css` 从大而全样式集收敛为页面必要补充。

## 变更日志
- [2026-02-13] 按 Dify/ComfyUI 视觉目标重构 WorkflowEditorPage 页面壳层与浮层布局。
- [2026-02-13] 节点投放方式收敛为 React Flow 画布 + Dock 点击添加，移除基础 HTML 拖拽依赖。
- [2026-02-13] 补充节点内展开配置、graphJson 加载入口与模拟执行状态流转。
