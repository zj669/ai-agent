# ReviewDetailPage.tsx 蓝图

## Metadata
- title: ReviewDetailPage
- type: page
- summary: 审核详情页，展示上游节点和当前暂停节点的输入输出，支持编辑节点输出（JSON 格式）后提交审核

## 路由
- `/reviews/:executionId`

## 核心逻辑

### 数据加载
- 调用 `getReviewDetail(executionId)` 获取审核详情
- 返回: executionId, nodeId, nodeName, triggerPhase, nodes[]

### 节点分类
- `getCurrentNode()`: `nodes.find(n => n.nodeId === detail.nodeId)` — 当前暂停节点
- `getUpstreamNodes()`: `nodes.filter(n => n.nodeId !== detail.nodeId)` — 上游已完成节点

### 编辑功能
- 每个有 outputs 的节点显示"编辑输出"按钮
- 点击打开 Modal，以 JSON TextArea 编辑输出数据
- 编辑内容暂存在 `nodeEdits` state（`Record<nodeId, Record<string, unknown>>`）
- 已编辑的节点显示绿色"已编辑"标签，输出区域绿色边框

### 提交审核
- **通过**: `resumeReview({ executionId, nodeId, nodeEdits })` — 携带所有编辑
- **拒绝**: `resumeReview({ executionId, nodeId, comment: "[拒绝] ...", nodeEdits })` — 同样携带编辑

### 阶段提示
- BEFORE_EXECUTION: 蓝色 info Alert — "节点尚未执行"
- AFTER_EXECUTION: 橙色 warning Alert — "节点已执行完成，可编辑输出"

## UI 组件
- Ant Design: Card, Descriptions, Tag, Button, Modal, Form, TextArea, Alert, Spin
- 图标: ArrowLeftOutlined, CheckOutlined, CloseOutlined, EditOutlined

## 变更记录
- 2026-03-17: 初始蓝图生成
