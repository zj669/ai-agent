# Writing API

## 概述

Writing 模块提供 Swarm 写作协作的业务投影查询。它不直接承载 Agent runtime；runtime 在 Swarm 中运行，Writing 聚合 session、worker、task、result、draft，供前端协作面板展示。

- Base URL: `/api/writing`
- Controller: `WritingController`
- 返回风格: 统一 `Response<T>`

## 当前数据模型

当前初始化 SQL 使用：

```text
writing_session
writing_task
writing_result
writing_draft
swarm_workspace_agent.session_id
swarm_workspace_agent.sort_order
```

历史设计里的 `writing_agent` 表已删除。当前协作者卡片由 `swarm_workspace_agent` 直接关联 `writing_session` 后聚合出来。

## 接口列表

### 1. 查询工作空间下的写作会话

```http
GET /api/writing/workspace/{workspaceId}/sessions
```

返回：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "workspaceId": 100,
      "topic": "写一份产品方案",
      "status": "RUNNING",
      "currentDraftId": 3,
      "createdAt": "2026-05-14T10:00:00",
      "updatedAt": "2026-05-14T10:20:00"
    }
  ]
}
```

具体字段以 `WritingSessionSummaryDTO` 为准。

### 2. 查询写作会话 overview

```http
GET /api/writing/session/{sessionId}/overview
```

返回当前写作会话聚合视图，包含：

1. session 摘要。
2. 协作 Agent 卡片。
3. 写作任务摘要。
4. 结果摘要。
5. 草稿摘要。
6. 面向前端展示的消息视图。

具体字段以 `WritingSessionOverviewDTO` 为准。

## 运行链路

```text
SwarmWorkspaceService.createWorkspace(...)
  -> 创建 workspace / coordinator agent / p2p group
  -> WritingSessionService.createSession(...)
  -> coordinator agent 写入 sessionId + sortOrder

SwarmTools.create_worker(...)
  -> 创建 swarm agent
  -> 设置 sessionId / sortOrder

SwarmTools.delegate_task(...)
  -> 创建 writing_task
  -> 发送 swarm message

SwarmTools.submit_result(...)
  -> 创建 writing_result
  -> 标记 task 完成

WritingProjectionService.getSessionOverview(...)
  -> 聚合 session / swarm agents / task / result / draft
```

## 相关代码

| 位置 | 说明 |
|------|------|
| `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/writing/WritingController.java` | HTTP 接口 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/writing/WritingProjectionService.java` | overview 聚合 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/writing/*Service.java` | session/task/result/draft 服务 |
| `ai-agent-domain/src/main/java/com/zj/aiagent/domain/writing/entity/**` | 写作领域实体 |
| `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/writing/**` | MyBatis PO/Mapper/Repository |
| `docker/init/mysql/01_init_schema.sql` | 当前表结构 |
