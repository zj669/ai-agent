## Metadata
- file: `.blueprint/frontend/pages/WorkflowEditorPage.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowEditorPage
- 该文件用于描述 WorkflowEditorPage 的职责边界与协作关系。

## 2) 核心方法
- `initEditorContext(params)`
- `mountWorkflowCanvas(opts)`
- `handleTopActions(action)`
- `handleNodeDrop(payload)`

## 3) 具体方法
### 3.1 initEditorContext(params)
- 函数签名: `async initEditorContext(params: { agentId: string }): Promise<void>`
- 入参: `params` 包含 `agentId` 路由参数
- 出参: Promise<void>，异步初始化完成后更新组件状态
- 功能含义: 页面挂载时的初始化逻辑，通过 `agentService.getAgent` 获取 Agent 详情，解析 `graphJson` 字段并调用 `loadGraph` 加载到编辑器。设置工作流名称、描述等元信息。
- 链路作用: 编辑器页面的启动入口，连接路由参数与编辑器状态，实现"编辑现有工作流"场景。

### 3.2 mountWorkflowCanvas(opts)
- 函数签名: `mountWorkflowCanvas(opts: { reactFlowWrapper: RefObject<HTMLDivElement> }): ReactElement`
- 入参: `opts` 包含 React Flow 容器的 ref 引用
- 出参: 返回 ReactFlow 组件的 JSX 元素
- 功能含义: 渲染 React Flow 画布，配置 `nodeTypes`, `edgeTypes`, `onNodesChange`, `onEdgesChange`, `onConnect` 等核心属性。集成 Background, Controls, MiniMap 等辅助组件。处理拖拽添加节点、节点选中、画布缩放等交互。
- 链路作用: 视图层的核心渲染逻辑，将 Hook 状态映射为可交互的画布 UI。

### 3.3 handleTopActions(action)
- 函数签名: `handleTopActions(action: 'save' | 'execute' | 'back' | 'undo' | 'redo'): void`
- 入参: `action` 为顶部工具栏的操作类型
- 出参: 无返回值，触发对应副作用
- 功能含义: 处理工具栏按钮点击事件。`save` 调用 `agentService.updateAgent` 保存图结构；`execute` 打开执行模态框；`back` 导航回列表页；`undo/redo` 调用历史管理 Hook。
- 链路作用: 用户操作的分发中心，协调编辑、保存、执行等高层业务流程。

### 3.4 handleNodeDrop(payload)
- 函数签名: `handleNodeDrop(payload: { type: NodeType, position: XYPosition }): void`
- 入参: `payload` 包含节点类型和画布坐标
- 出参: 无返回值，直接添加节点到图中
- 功能含义: 响应从 NodePanel 拖拽节点到画布的操作，调用 `addNode` 创建新节点实例，使用 `screenToFlowPosition` 转换坐标，触发自动布局。
- 链路作用: 拖拽交互的处理器，实现"从侧边栏添加节点"的核心 UX。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，基于历史实现提供页面初始化、画布挂载、工具栏操作、拖拽处理的完整签名与语义。说明其在迁移链路中的定位：原独立编辑器页面已删除，工作流执行已简化为 ChatPage 内的 SSE 流式对话。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
