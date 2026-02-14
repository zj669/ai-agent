## Metadata
- file: `.blueprint/frontend/components/NodePanel.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: NodePanel
- 该文件用于描述 NodePanel 的职责边界与协作关系。

## 2) 核心方法
- `renderNodeGroups(groups, keyword)`
- `handleAddNode(type)`
- `toggleCollapsed(collapsed)`

## 3) 具体方法
### 3.1 renderNodeGroups(groups, keyword)
- 函数签名: `renderNodeGroups(groups: NodeGroup[], keyword: string): ReactElement[]`
- 入参:
  - `groups`: 节点分组配置数组，每组包含 `key`, `label`, `icon`, `nodes`（节点类型列表）
  - `keyword`: 搜索关键词，用于过滤节点
- 出参: 返回 React 元素数组，渲染为 Ant Design Collapse 面板
- 功能含义: 根据分组配置和搜索关键词渲染节点列表。每个节点显示图标、名称、描述，支持拖拽到画布。使用 `NODE_GROUPS` 常量定义基础节点（START/END）、AI 节点（LLM）、逻辑节点（CONDITION）、集成节点（HTTP/TOOL）的分类。
- 链路作用: 侧边栏节点库的视图层，提供节点选择和拖拽源。

### 3.2 handleAddNode(type)
- 函数签名: `handleAddNode(type: NodeType): void`
- 入参: `type` 为节点类型枚举（START, END, LLM, HTTP, CONDITION, TOOL）
- 出参: 无返回值，触发父组件的 `onAddNode` 回调
- 功能含义: 响应节点卡片的点击或拖拽开始事件，通知父组件（WorkflowEditorPage）添加节点。支持两种交互模式：点击直接添加到画布中心，拖拽到指定位置。
- 链路作用: 节点添加的事件桥接层，连接 UI 交互与编辑器状态更新。

### 3.3 toggleCollapsed(collapsed)
- 函数签名: `toggleCollapsed(collapsed: boolean): void`
- 入参: `collapsed` 布尔值，表示面板是否折叠
- 出参: 无返回值，更新内部状态和触发父组件回调
- 功能含义: 控制侧边栏的展开/折叠状态，折叠时仅显示图标栏，展开时显示完整节点列表。通过 `onCollapsedChange` 回调通知父组件调整布局。
- 链路作用: 侧边栏布局控制器，优化画布空间利用率。


## 4) 变更记录
- 2026-02-14: 统一重构为 Blueprint-Lite 最小结构，状态基线设为 `正常`，并保留原文关键语义摘要。
- 2026-02-14: 补全"具体方法"细节，基于历史实现提供节点分组渲染、添加处理、折叠控制的完整签名与语义。说明其在迁移链路中的定位：原侧边栏组件已删除，节点添加功能已简化为 ChatPage 的预设工作流选择。
- 2026-02-14: 移除重复方法占位条目，保留唯一契约定义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
