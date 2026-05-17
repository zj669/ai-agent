# Swarm API

## 概述

Swarm 模块提供多 Agent 工作空间、Agent、群组消息、拓扑图、搜索和 SSE 事件。当前 Writing 协作依赖 Swarm runtime，但写作业务投影由 `/api/writing` 提供。

- Base URL: `/api/swarm/*`
- 返回风格: 统一 `Response<T>`；SSE 接口返回 `SseEmitter`
- 认证: 受 `/api/**` 拦截器保护，用户 ID 通常来自 `UserContext`

## Workspace

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/swarm/workspace` | 创建工作空间 |
| `GET` | `/api/swarm/workspace` | 当前用户工作空间列表 |
| `GET` | `/api/swarm/workspace/{id}` | 工作空间详情 |
| `PUT` | `/api/swarm/workspace/{id}` | 更新工作空间 |
| `DELETE` | `/api/swarm/workspace/{id}` | 删除工作空间 |
| `GET` | `/api/swarm/workspace/{id}/defaults` | 工作空间默认配置 |

创建工作空间时，当前应用服务会：

1. 创建 `swarm_workspace`。
2. 创建 Coordinator/assistant `swarm_workspace_agent`。
3. 创建 P2P group。
4. 创建 `writing_session`。
5. 把 `sessionId` 和 `sortOrder` 写回 Coordinator agent。

## Agent

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/swarm/workspace/{wid}/agents` | 工作空间 Agent 列表 |
| `POST` | `/api/swarm/workspace/{wid}/agents` | 创建 Agent |
| `GET` | `/api/swarm/agent/{id}` | Agent 详情 |
| `POST` | `/api/swarm/agent/{id}/stop` | 停止单个 Agent runtime |
| `POST` | `/api/swarm/agents/interrupt-all` | 当前 Controller 仅记录日志并返回成功，TODO P2 |

创建 Agent 请求体：

```json
{
  "role": "researcher",
  "parentId": 1,
  "description": "负责资料检索和事实核对"
}
```

## Group / Message

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/swarm/workspace/{wid}/groups` | 群组列表，可传 `agentId` |
| `GET` | `/api/swarm/group/{gid}/messages` | 群消息，可传 `markRead` 和 `readerId` |
| `POST` | `/api/swarm/workspace/{wid}/groups/p2p` | 创建或获取 P2P 群 |
| `POST` | `/api/swarm/group/{gid}/messages` | 发送消息 |

P2P 创建请求：

```json
{
  "agentId1": 1,
  "agentId2": 2
}
```

## Graph / Search

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/swarm/workspace/{wid}/graph` | 工作空间 Agent 拓扑图 |
| `GET` | `/api/swarm/workspace/{wid}/search?q=xxx` | Agent 搜索 |

## SSE

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/swarm/agent/{agentId}/stream` | Agent 级事件流 |
| `GET` | `/api/swarm/workspace/{workspaceId}/ui-stream` | Workspace UI 事件流 |

Agent 级事件包括 reasoning、content、tool_calls、tool_result、done、error 等。UI 级事件用于前端工作空间整体刷新和协作状态展示。

## 数据结构重点

当前初始化 SQL 中，`swarm_workspace_agent` 包含：

```text
description
session_id
sort_order
```

这意味着历史文档中提到的 `writing_agent` 桥接表已不是当前结构；当前写作协作者直接由 `swarm_workspace_agent` 承载，并通过 `session_id/sort_order` 参与写作 overview 聚合。

## 相关代码

| 位置 | 说明 |
|------|------|
| `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/swarm/**` | HTTP/SSE 接口 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/SwarmWorkspaceService.java` | Workspace/Agent/Graph 编排 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/SwarmAgentRuntimeService.java` | Agent runtime |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/runtime/SwarmTools.java` | Swarm 工具 |
| `ai-agent-foward/src/modules/swarm/**` | 前端页面与 hooks |
