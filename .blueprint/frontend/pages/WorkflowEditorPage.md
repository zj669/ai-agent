## Metadata
- file: `.blueprint/frontend/pages/WorkflowEditorPage.md`
- version: `1.2`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowEditorPage (MVP)
- 对应 `ai-agent-foward/src/pages/WorkflowEditorPage.tsx`，仅负责四件事：
  1) 初始化单路径加载 `graphJson + node metadata + version`；
  2) 连接拖拉拽画布与卡片配置面板；
  3) 组织 `graphJson` 保存提交；
  4) 处理版本冲突与服务端回填。

## 2) 核心方法
- `initEditorContext(params)`
- `composeMetadataDrivenPanels()`
- `handleCanvasDrop(payload)`
- `handleSaveWorkflow()`

## 3) 具体方法
### 3.1 initEditorContext(params)
- 函数签名: `async initEditorContext(params: { agentId: string }): Promise<void>`
- 入参: `agentId` 路由参数
- 出参: Promise<void>
- 功能含义: 页面初始化仅调用 `workflowService.loadWorkflow(agentId)`，一次性获取 `graphJson + metadata + version` 并传递给 `useWorkflowEditor` 建立编辑态。
- 链路作用: 建立“后端定义 + 前端画布”的统一初始化入口。

### 3.2 composeMetadataDrivenPanels()
- 函数签名: `composeMetadataDrivenPanels(): { nodePanelProps: NodePanelProps; propertiesPanelProps: NodePropertiesPanelProps }`
- 入参: 无（读取页面状态）
- 出参: NodePanel 与 NodePropertiesPanel 的 props
- 功能含义: 使用同一份 metadata 组装节点目录与配置表单，保证“可拖入节点类型”与“可编辑字段”完全同源。
- 链路作用: 避免硬编码卡片类型与字段定义。

### 3.3 handleCanvasDrop(payload)
- 函数签名: `handleCanvasDrop(payload: { nodeType: string; templateId?: string }): void`
- 入参: 节点新增意图（节点类型与模板信息）
- 出参: 无
- 功能含义: 接收 NodePanel 新增事件并调用 `useWorkflowEditor.addNodeFromTemplate`。节点坐标由前端画布上下文自动补齐（拖拽落点或默认位置）。
- 链路作用: 串联“节点库拖拽 -> 画布新增”。

### 3.4 handleSaveWorkflow()
- 函数签名: `async handleSaveWorkflow(): Promise<void>`
- 入参: 无（依赖页面中的 `agentId/version/editorState`）
- 出参: Promise<void>
- 功能含义: 保存时依次执行：
  1. `validateBeforeSave`（包含 unknown nodeType 阻断）；
  2. `buildSavePayload` 构建标准 `graphJson`；
  3. 调用后端更新接口，携带 `version` 做乐观锁提交。
- 链路作用: MVP 保存链路总编排。

## 4) 关键协作契约（MVP裁剪）
- 页面只做编排，不负责节点模型转换。
- `graphJson` 是唯一持久化载荷，页面禁止保存其他并行结构。
- 回显坐标来源仅为后端 `graphJson.position`；页面禁止覆盖回放坐标。
- NodePanel 与 NodePropertiesPanel 必须同源 metadata。
- 初始化必须单路径：页面仅通过 `workflowService.loadWorkflow` 获取 `graphJson + metadata + version`。
- 页面连线门禁（`isValidConnection`）对 `source/target` 与节点 `id` 做字符串归一化比较，保证历史数据类型不一致时仍能正确命中节点。
- 连线约束保持不变：`END` 不能作为 source，`START` 不能作为 target。
- 仅保留拖拉拽编辑、卡片配置、保存对接三条主链路。
- 不纳入本阶段：快捷键、撤销重做、多选框选、右键菜单、协作态。

## 5) 变更记录
- 2026-02-16: 按 MVP 目标收敛页面契约，仅保留拖拽、卡片配置、graphJson 对接与 metadata 驱动。
- 2026-02-16: 明确排除非 MVP 交互（快捷键/undo-redo/协作等）。
- 2026-02-16: 修复必修问题：统一新增节点坐标由前端自动补齐，回显坐标由后端 graphJson 决定；初始化改为单路径加载。
- 2026-02-17: 补充连线校验 ID 归一化契约，修复历史节点与新增节点因 ID 类型不一致导致的连线失败，同时保持 START/END 连接约束不变。

## 6) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
