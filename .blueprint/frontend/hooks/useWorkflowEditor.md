## Metadata
- file: `.blueprint/frontend/hooks/useWorkflowEditor.md`
- version: `1.2`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: useWorkflowEditor (MVP)
- 对应 `ai-agent-foward/src/hooks/useWorkflowEditor.ts`，负责最小编辑闭环：
  1) 维护 ReactFlow 运行态；
  2) 维护 graphJson 持久化映射；
  3) 处理拖拉拽新增/连线；
  4) 保存前执行 metadata 契约校验。

## 2) 核心方法
- `useWorkflowEditor(initialGraphJson?)`
- `mapGraphJsonToReactFlow(graphJson, templatesByType)`
- `mapReactFlowToGraphJson(nodes, edges)`
- `addNodeFromTemplate(payload)`
- `validateBeforeSave(nodes, edges, metadataMap)`

## 3) 具体方法
### 3.1 useWorkflowEditor(initialGraphJson?)
- 函数签名: `function useWorkflowEditor(initialGraphJson?: GraphJson): UseWorkflowEditorReturn`
- 入参: `initialGraphJson`（可选）
- 出参: `nodes`, `edges`, `onNodesChange`, `onEdgesChange`, `onConnect`, `addNodeFromTemplate`, `replaceGraph`, `buildSavePayload`, `validation`
- 功能含义: 统一暴露画布编辑状态与最小编辑动作，不直接发请求。
- 链路作用: 页面唯一编辑状态入口。

### 3.2 mapGraphJsonToReactFlow(graphJson, templatesByType)
- 函数签名: `mapGraphJsonToReactFlow(graphJson: GraphJson, templatesByType: Record<string, NodeMetadata>): { nodes: RFNode[]; edges: RFEdge[] }`
- 入参: 后端 graphJson 与模板映射
- 出参: ReactFlow `nodes/edges`
- 功能含义: 初始化画布节点时，节点位置优先使用 `graphJson` 中持久化坐标进行回放；metadata 仅用于补齐节点展示、输入/输出定义与默认配置，不覆盖回放坐标。
- ID归一化契约: 在映射边界统一将 `nodeId/edgeId/source/target` 归一化为 `string`（`String(value).trim()`），避免历史数据存在 number/string 混用时连线校验失败。
- 链路作用: 后端定义驱动画布渲染。

### 3.3 mapReactFlowToGraphJson(nodes, edges)
- 函数签名: `mapReactFlowToGraphJson(nodes: RFNode[], edges: RFEdge[]): GraphJson`
- 入参: 当前画布节点与边
- 出参: 标准 `graphJson`
- 功能含义: 将前端编辑态回写为后端可持久化结构，并保持 ID 在保存载荷中为字符串，保证与运行态比较一致。
- 链路作用: 保存前构建请求体。

### 3.4 addNodeFromTemplate(payload)
- 函数签名: `addNodeFromTemplate(payload: { nodeType: string; templateId?: string; position?: { x: number; y: number } }): void`
- 入参: 节点类型、模板 ID、可选落点坐标
- 出参: 无
- 功能含义: 依据 metadata 模板创建节点初始配置并插入画布；当未传 `position` 时由前端自动补齐默认坐标。
- 链路作用: 拖拽新增主链路。

### 3.5 validateBeforeSave(nodes, edges, metadataMap)
- 函数签名: `validateBeforeSave(nodes: RFNode[], edges: RFEdge[], metadataMap: Record<string, NodeMetadata>): ValidationResult`
- 入参: 当前图结构与模板映射
- 出参: `ValidationResult`（`ok`, `errors`, `unknownNodeTypes`）
- 功能含义: 校验 metadata required 字段、节点类型合法性、图结构基础约束。默认节点结构为三部分：`inputSchema`、`outputSchema`、`userConfig`。
- 链路作用: 保存门禁。

## 4) 关键协作契约（MVP裁剪）
- Hook 是 ReactFlow <-> graphJson 唯一映射边界。
- 节点创建必须走 metadata 模板，不允许前端硬编码默认配置。
- 新增节点位置由前端自动生成；已保存节点位置只由 graphJson 回放，二者路径分离且不可互相覆盖。
- 节点模型默认由三段组成：输入（`inputSchema`）、输出（`outputSchema`）、节点配置（`userConfig`），并由后端 metadata 驱动。
- 保存只输出 graphJson，不输出额外编辑态结构。
- 仅保留拖拽、连线、卡片配置、保存校验能力。
- 不纳入本阶段：undo/redo、快捷键、多选、右键、协作态。

## 5) 变更记录
- 2026-02-16: 按 MVP 目标收敛 Hook 契约，仅保留拖拽编辑与 graphJson 对接核心链路。
- 2026-02-16: 强化“metadata 驱动节点初始配置”约束。
- 2026-02-16: 修复必修问题：节点位置回放与新增坐标补齐策略拆分；补充节点三段结构契约（input/output/config）。
- 2026-02-17: 补充 ID 归一化契约：映射边界统一归一化 `nodeId/edgeId/source/target` 为字符串，修复新增节点与历史节点连线时的类型不一致问题。

## 6) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
