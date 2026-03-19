# HumanReviewDTO.java 蓝图

## Metadata
- title: HumanReviewDTO
- type: dto
- summary: 人工审核相关 DTO 集合，包含待审核列表、审核详情、节点上下文、恢复/拒绝请求、恢复响应

## 内部类

### PendingReviewDTO
- 用途: 待审核列表项
- 字段: executionId, nodeId, nodeName, agentName, triggerPhase, pausedAt, userId

### ReviewDetailDTO
- 用途: 审核详情页数据
- 字段: executionId, nodeId, nodeName, triggerPhase, nodes (List<NodeContextDTO>)

### NodeContextDTO
- 用途: 节点上下文（输入输出快照）
- 字段: nodeId, nodeName, nodeType, status, inputs (Map), outputs (Map)
- 说明: 包含上游已成功节点和当前暂停节点

### ResumeExecutionRequest
- 用途: 提交审核（恢复执行）请求体
- 字段: executionId, nodeId, edits (Map), comment, nodeEdits (Map<nodeId, Map>)
- 说明: `nodeEdits` 支持多节点编辑，key 为 nodeId，value 为该节点的修改数据

### RejectExecutionRequest
- 用途: 拒绝审核请求体（已定义但未使用）
- 字段: executionId, nodeId, reason
- 已知问题: 后端无对应 reject 端点

### ResumeExecutionResponse
- 用途: 恢复执行响应体（已定义但未使用）
- 字段: success, message

## 变更记录
- 2026-03-17: 初始蓝图生成
