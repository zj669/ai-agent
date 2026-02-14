## Metadata
- file: `.blueprint/frontend/hooks/useWorkflowEditor.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: useWorkflowEditor
- 该文件用于描述 useWorkflowEditor 的职责边界与协作关系。

## 2) 核心方法
- `useWorkflowEditor(initialGraph?)`
- `onNodesChange(changes)`
- `startExecutionFlow(request)`
- `onConnect(connection)`

## 3) 具体方法
### 3.1 useWorkflowEditor(initialGraph?)
- 函数签名: `function useWorkflowEditor(initialGraph?: WorkflowGraph): UseWorkflowEditorReturn`
- 入参: `initialGraph` 可选的初始工作流图结构（包含 nodes, edges, edgeDetails）
- 出参: 返回对象包含 `nodes`, `edges`, `onNodesChange`, `onEdgesChange`, `onConnect`, `addNode`, `loadGraph`, `convertToWorkflowGraph`, `isExecuting`, `startExecution`, `stopExecution`, `updateNodeData`, `clearExecutionStatus`
- 功能含义: React Hook，封装工作流编辑器的完整状态管理和交互逻辑。整合 React Flow 的节点/边操作、执行控制、历史记录（undo/redo）、自动布局等功能。内部组合 `useWorkflowInteractions`, `useNodesInteractions`, `useEdgesInteractions`, `useWorkflowHistory`, `useNodesSyncDraft` 等子 Hook。
- 链路作用: WorkflowEditorPage 的核心业务逻辑层，连接 UI 组件与 workflowService。历史实现已删除，功能已迁移至 ChatPage 的简化执行流程。

### 3.2 onNodesChange(changes)
- 函数签名: `(changes: NodeChange[]) => void`
- 入参: `changes` 为 React Flow 的节点变更数组（position, selection, remove 等）
- 出参: 无返回值，直接更新内部状态
- 功能含义: 响应 React Flow 的节点变更事件，同步更新 Zustand store 中的节点状态。触发自动保存和历史快照记录。
- 链路作用: React Flow 与状态管理的桥接层，确保画布操作与数据模型同步。

### 3.3 startExecutionFlow(request)
- 函数签名: `async (request: StartExecutionRequest) => Promise<void>`
- 入参: `request` 包含 `agentId`, `conversationId`, `userMessage`, `executionMode`
- 出参: Promise<void>，执行过程通过 SSE 回调异步通知
- 功能含义: 调用 `workflowService.startExecution`，建立 SSE 连接，监听执行事件并更新节点状态（RUNNING → SUCCEEDED/FAILED）。维护 `isExecuting` 标志和 `executionId` 状态。
- 链路作用: 编辑器内的执行触发器，将静态图转换为动态执行流，实时反馈节点状态到画布。

### 3.4 onConnect(connection)
- 函数签名: `(connection: Connection) => void`
- 入参: `connection` 包含 `source`, `target`, `sourceHandle`, `targetHandle`
- 出参: 无返回值，直接更新 edges 状态
- 功能含义: 响应用户在画布上连接两个节点的操作，验证连接合法性（避免循环、类型匹配），创建新边并添加到图中。触发自动布局调整。
- 链路作用: 交互层的连接处理器，确保图的拓扑结构符合工作流语义约束。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，基于历史实现提供 Hook 完整签名、状态管理逻辑、执行控制、连接处理的详细语义。说明其在迁移链路中的定位：原独立编辑器 Hook 已废弃，功能简化后整合至 ChatPage。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
