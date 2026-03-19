# ReviewPage.tsx 蓝图

## Metadata
- title: ReviewPage
- type: page
- summary: 待审核列表页，展示所有等待人工审核的工作流执行，支持通过/拒绝/查看详情操作

## 路由
- `/reviews`

## 核心逻辑

### 数据加载
- 调用 `getPendingReviews()` 获取待审核列表
- 数据源: Redis Set `human_review:pending` → 逐个查询 Execution

### 表格列
- Agent 名称、节点名称、审核阶段（执行前/执行后）、暂停时间、状态（固定"待审核"）、操作

### 操作按钮
- **查看详情**: 跳转 `/reviews/{executionId}`
- **通过**: Popconfirm → `resumeReview({ executionId, nodeId })`
- **拒绝**: Popconfirm + TextArea → `resumeReview({ executionId, nodeId, comment: "[拒绝] ..." })`

## 已知问题
- 拒绝操作实际调用 resume 接口，工作流仍会继续执行
- 列表页直接通过/拒绝时无法编辑节点数据，需进入详情页

## 变更记录
- 2026-03-17: 初始蓝图生成
