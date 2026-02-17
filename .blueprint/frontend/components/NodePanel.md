## Metadata
- file: `.blueprint/frontend/components/NodePanel.md`
- version: `1.2`
- status: 修改完成
- updated_at: 2026-02-16
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: NodePanel (MVP)
- 对应 `ai-agent-foward/src/components/workflow/NodePanel.tsx`，负责 metadata 驱动节点目录展示与拖拽新增意图分发。

## 2) 核心方法
- `buildNodeCatalog(metadataList, keyword)`
- `renderNodeGroups(groups)`
- `handleDragStart(type, templateId)`
- `emitAddNodeIntent(type, templateId)`

## 3) 具体方法
### 3.1 buildNodeCatalog(metadataList, keyword)
- 函数签名: `buildNodeCatalog(metadataList: NodeMetadata[], keyword: string): NodeGroup[]`
- 入参:
  - `metadataList`: 后端返回的节点模板定义
  - `keyword`: 搜索关键词
- 出参: 分组过滤后的节点目录
- 功能含义: 基于 metadata 生成节点目录，不允许硬编码节点清单。
- 链路作用: 节点库动态化来源。

### 3.2 renderNodeGroups(groups)
- 函数签名: `renderNodeGroups(groups: NodeGroup[]): ReactElement[]`
- 入参: `groups` 节点分组
- 出参: React 渲染元素
- 功能含义: 渲染节点卡片（名称、图标、描述），仅展示 metadata 声明可新增的类型。
- 链路作用: 拖拽源列表视图。

### 3.3 handleDragStart(type, templateId)
- 函数签名: `handleDragStart(type: string, templateId?: string): DragPayload`
- 入参: 节点类型与可选模板 ID
- 出参: 标准拖拽载荷
- 功能含义: 统一节点拖拽数据格式，供画布 drop 解析。
- 链路作用: NodePanel -> 画布 drop 数据桥。

### 3.4 emitAddNodeIntent(type, templateId)
- 函数签名: `emitAddNodeIntent(type: string, templateId?: string): void`
- 入参: 节点类型与模板 ID
- 出参: 无
- 功能含义: 非拖拽场景（点击新增）触发同一新增事件，复用页面链路。
- 链路作用: 保持新增路径一致性。

## 4) 关键协作契约（MVP裁剪）
- NodePanel 与 NodePropertiesPanel 必须同源 metadata。
- NodePanel 只负责“可拖入节点类型”展示与新增事件，不负责保存与业务校验。
- 新增节点事件最小载荷：`nodeType + templateId(optional)`。
- NodePanel 不传递节点坐标；坐标由画布编辑上下文自动补齐。
- 仅保留拖拉拽与点击新增两类入口；不纳入快捷键、批量操作、右键菜单。

## 5) 变更记录
- 2026-02-16: 按 MVP 目标收敛 NodePanel，仅保留 metadata 驱动节点目录与拖拽新增。
- 2026-02-16: 明确排除非 MVP 交互能力。
- 2026-02-16: 修复必修问题：明确新增事件不携带 position，统一由前端画布补齐。

## 6) Temp缓存区
- 本次任务流转: `待修改 -> 修改中 -> 修改完成`
- 当前状态: `修改完成`
