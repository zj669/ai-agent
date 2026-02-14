# Workflow Hooks 与 Store Slices Blueprint

## 职责契约
- **做什么**: 定义 workflow 交互 hooks 拆分方式与 Zustand slices 状态边界。
- **不做什么**: 不实现具体 UI，不承载页面路由与布局代码。

## Hooks 拆分（必须落地）
| Hook | 核心职责 | 可写状态 |
|---|---|---|
| `use-workflow-interactions` | 画布级交互编排（拖拽投放、缩放、模式切换） | layout/panel |
| `use-nodes-interactions` | 节点增删改、选中态、复制、节点快捷动作 | workflow/history |
| `use-edges-interactions` | 连线创建/删除、连接校验、边选择态 | workflow/history |
| `use-workflow-history` | undo/redo 管理、操作事件归档、历史回放 | history |
| `use-nodes-sync-draft` | 前端 draft 与后端 DSL 的显式同步、冲突收敛 | workflow/history |

## Store slices（必须落地）
| Slice | 状态范围 | 允许写入方 |
|---|---|---|
| `workflow-slice` | nodes/edges、只读态、运行态映射 | node/edge hooks |
| `layout-slice` | viewport、zoom、pointer/hand 模式、maximize | workflow interactions |
| `panel-slice` | 节点配置/环境变量/调试/历史面板开关与上下文 | workflow interactions + panel |
| `history-slice` | past/present/future、actionType、游标索引 | workflow history |

## 状态写入约束
1. hooks 是唯一写状态入口，组件层只消费 selector。
2. 关键图操作（节点、连线、布局）必须进入 history 栈。
3. `use-nodes-sync-draft` 只允许显式触发 `syncDraft()`，禁止页面隐式提交。
4. store 中禁止出现 fetch/axios/EventSource/DOM API。

## 事件与常量约束
- 事件名统一定义在 `workflow/constants.ts`。
- `isValidConnection` 由 `use-edges-interactions` 统一出口。
- SSE 运行事件通过 hook 转换后写入 store，不直接驱动页面局部状态。
- 连接与布局变更必须产生可追踪 actionType。

## 依赖拓扑
- 上游：`workflow/index.tsx`、`pages/WorkflowEditorPage.tsx`
- 下游：`workflow/store/index.ts`、`workflow/utils/*`、`services/workflowService`
- 禁止：hooks 反向依赖 pages；store 依赖 services。

## 历史系统最低能力
- 支持 `undo` / `redo` / `clearHistory`。
- 支持 actionType：`NODE_ADD`、`NODE_DELETE`、`EDGE_CONNECT`、`LAYOUT_APPLY`、`DRAFT_SYNC`。
- 历史快照与运行态状态解耦，避免执行中污染编辑历史。

## 草稿同步最低能力
- draft → DSL、DSL → draft 双向转换均走 `utils/graph-transformer.ts`。
- 支持版本号或时间戳防抖/冲突处理。
- 同步失败时保留当前编辑态并返回结构化错误。

## 变更摘要
- 从单体 `useWorkflowEditor` 拆分为 5 个 hooks，形成可测交互单元。
- 将编辑态收敛到 `workflow/layout/panel/history` 四个 slices。
- 明确历史系统与草稿同步的协同边界，保证可回退与可追踪。

## 变更日志
- [2026-02-13] 按 Dify 模式重写 hooks 与 slices 蓝图。
