# reviewAdapter.ts 蓝图

## Metadata
- title: reviewAdapter
- type: api-adapter
- summary: 人工审核前端 API 适配层，封装待审核列表、审核详情、提交审核三个接口调用

## 类型定义

### PendingReview
- 字段: executionId, nodeId, nodeName, agentName, triggerPhase, pausedAt, userId, content?

### NodeContext
- 字段: nodeId, nodeName, nodeType, status, inputs?, outputs?

### ReviewDetail
- 字段: executionId, nodeId, nodeName, triggerPhase, nodes (NodeContext[])

### ResumeReviewInput
- 字段: executionId, nodeId, edits?, comment?, nodeEdits?

## API 方法

### getPendingReviews
- 端点: `GET /api/workflow/reviews/pending`
- 返回: `PendingReview[]`

### getReviewDetail
- 端点: `GET /api/workflow/reviews/{executionId}`
- 返回: `ReviewDetail`

### resumeReview
- 端点: `POST /api/workflow/reviews/resume`
- 入参: `ResumeReviewInput`
- 说明: 通过和拒绝都调用此方法，拒绝时在 comment 中加 `[拒绝]` 前缀

## 变更记录
- 2026-03-17: 初始蓝图生成
