## Metadata
- file: `.blueprint/frontend/components/WorkflowNodeLarge.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WorkflowNodeLarge
- 该文件用于描述 WorkflowNodeLarge 的职责边界与协作关系。

## 2) 核心方法
- `renderNodeHeader(data, status)`
- `renderConfigPreview(nodeType, config)`
- `resolveHandles(nodeType, branchCount)`

## 3) 具体方法
### 3.1 renderNodeHeader(data, status)
- 函数签名: `renderNodeHeader(data: WorkflowNodeData, status?: NodeExecutionStatus): ReactElement`
- 入参:
  - `data`: 节点数据对象，包含 `label`, `nodeType`, `config`
  - `status`: 可选的执行状态（RUNNING, SUCCEEDED, FAILED）
- 出参: 返回节点头部的 JSX 元素
- 功能含义: 渲染节点的头部区域，包括节点类型图标、名称、状态指示器。根据 `NODE_CONFIG` 映射节点类型到对应的图标（PlayCircle, MessageSquare, GitBranch 等）和颜色主题。执行状态显示为动画加载器（RUNNING）或成功/失败图标。
- 链路作用: 节点视觉呈现的核心组件，提供类型识别和状态反馈。

### 3.2 renderConfigPreview(nodeType, config)
- 函数签名: `renderConfigPreview(nodeType: NodeType, config?: NodeConfig): ReactElement | null`
- 入参:
  - `nodeType`: 节点类型枚举
  - `config`: 可选的节点配置对象（LLM 的 prompt, HTTP 的 url/method, CONDITION 的 expression 等）
- 出参: 返回配置预览的 JSX 元素，无配置时返回 null
- 功能含义: 根据节点类型渲染配置摘要。LLM 节点显示 prompt 前 50 字符；HTTP 节点显示 method + url；CONDITION 节点显示条件表达式；TOOL 节点显示工具名称。使用 Ant Design Tag 组件展示关键配置项。
- 链路作用: 节点内容区的信息展示，帮助用户快速识别节点配置而无需打开详情面板。

### 3.3 resolveHandles(nodeType, branchCount)
- 函数签名: `resolveHandles(nodeType: NodeType, branchCount?: number): { inputs: HandleConfig[], outputs: HandleConfig[] }`
- 入参:
  - `nodeType`: 节点类型枚举
  - `branchCount`: 可选的分支数量（用于 CONDITION 节点）
- 出参: 返回输入/输出连接点配置数组，每个配置包含 `id`, `position`, `type`
- 功能含义: 根据节点类型计算连接点布局。START 节点仅有输出；END 节点仅有输入；CONDITION 节点根据 `branchCount` 动态生成多个输出（默认 2 个）；其他节点有单输入单输出。使用 React Flow 的 Handle 组件渲染连接点。
- 链路作用: 节点拓扑结构的定义层，决定节点间的连接规则和视觉布局。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，基于历史实现提供节点头部渲染、配置预览、连接点解析的完整签名与语义。说明其在迁移链路中的定位：原大尺寸节点组件已删除，节点渲染已简化为 React Flow 默认样式。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
