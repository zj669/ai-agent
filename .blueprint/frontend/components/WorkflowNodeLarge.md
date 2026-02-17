## Metadata
- file: `.blueprint/frontend/components/WorkflowNodeLarge.md`
- version: `1.1`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowNode (MVP)
- 对应 `ai-agent-foward/src/components/workflow/WorkflowNode.tsx`，仅负责 MVP 基础节点渲染：
  1) 节点标题与类型图标展示（metadata 驱动）；
  2) 连接点（Handle）渲染，支持连线；
  3) 选中态视觉反馈。
- 注意：MVP 阶段不实现执行状态动画、配置预览、复杂视觉主题。

## 2) 核心方法 (MVP裁剪)
- `renderNodeBase(data)`
- `resolveHandles(nodeType)`
- `onNodeClick(nodeId)`

## 3) 具体方法
### 3.1 renderNodeBase(data)
- 函数签名: `renderNodeBase(data: WorkflowNodeData): ReactElement`
- 入参:
  - `data`: 节点数据对象，包含 `label`, `nodeType`（来自 metadata）
- 出参: 返回节点基础 JSX 元素
- 功能含义: MVP 仅渲染节点标题和类型图标，图标与颜色从 metadata 获取，不硬编码映射表。
- 链路作用: 最简节点视觉呈现，支持拖拽和连线即可。

### 3.2 resolveHandles(nodeType)
- 函数签名: `resolveHandles(nodeType: string): { inputs: HandleConfig[], outputs: HandleConfig[] }`
- 入参: `nodeType` 节点类型标识
- 出参: 输入/输出连接点配置数组
- 功能含义: 根据 metadata 定义的连接点规则渲染 Handle。MVP 阶段仅支持单输入单输出（CONDITION 节点支持多输出，但由 metadata 定义而非前端硬编码）。
- 链路作用: 支持节点连线的基础能力。

### 3.3 onNodeClick(nodeId)
- 函数签名: `onNodeClick(nodeId: string): void`
- 入参: `nodeId` 被点击节点 ID
- 出参: 无
- 功能含义: 节点点击事件，触发选中态变更并通知父组件打开卡片配置面板。
- 链路作用: 连接画布节点与配置面板的交互入口。


## 4) 关键协作契约（MVP裁剪）
- 节点渲染完全由 metadata 驱动，禁止前端硬编码节点类型样式映射。
- 仅保留最简视觉：标题 + 图标 + 选中态 + Handle 连接点。
- 不纳入本阶段：执行状态动画、配置预览摘要、复杂视觉主题、动态分支渲染。

## 5) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`。
- 2026-02-16: 按 MVP 目标收敛节点组件契约，仅保留基础渲染与连线能力，移除执行状态、配置预览等非必要功能。

## 6) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
