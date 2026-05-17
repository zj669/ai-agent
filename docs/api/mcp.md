# MCP API

## 概述

MCP 模块负责用户级 MCP server 配置、连接管理和工具发现。工作流 `TOOL` 节点通过工具名解析并调用 MCP 工具。

- Base URL: `/api/mcp`
- Controller: `McpServerController`
- 返回风格: 统一 `Response<T>`
- 认证: 需要登录态，server 相关接口使用 `UserContext`

## Server 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/mcp/servers` | 当前用户 server 列表 |
| `GET` | `/api/mcp/servers/{id}` | server 详情 |
| `POST` | `/api/mcp/servers` | 创建 server |
| `POST` | `/api/mcp/servers/{id}` | 更新 server |
| `DELETE` | `/api/mcp/servers/{id}` | 删除 server |
| `POST` | `/api/mcp/servers/{id}/delete` | 删除 server 的 POST 降级接口 |
| `POST` | `/api/mcp/servers/{id}/connect` | 连接并发现工具 |
| `POST` | `/api/mcp/servers/{id}/disconnect` | 断开连接 |
| `GET` | `/api/mcp/servers/{id}/status` | 查询状态 |
| `GET` | `/api/mcp/servers/{id}/tools` | 查询指定 server 工具 |
| `GET` | `/api/mcp/tools` | 查询所有已连接 server 的工具 |

## 创建/更新请求体

```json
{
  "name": "filesystem",
  "serverType": "stdio",
  "configJson": "{\"command\":\"npx\",\"args\":[\"-y\",\"@modelcontextprotocol/server-filesystem\"]}",
  "enabled": true,
  "description": "本地文件系统工具"
}
```

实现备注：

- 更新时如果 server 配置发生变化，服务会先断开旧连接。
- 删除前会尝试断开连接。
- `POST /servers/{id}/delete` 是为了兼容前端 HTTP client `delete` 可选时的降级方案。

## TOOL 节点约定

工作流 `TOOL` 节点当前通过 `ToolNodeExecutorStrategy` 调用 MCP 工具。工具名使用：

```text
mcp__{serverId}__{toolName}
```

策略会校验工具 schema 中的 required inputs，并把调用结果写入：

```text
tool_response
result
```

## 相关代码

| 位置 | 说明 |
|------|------|
| `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/mcp/McpServerController.java` | HTTP 接口 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/mcp/service/McpServerService.java` | server 应用服务 |
| `ai-agent-application/src/main/java/com/zj/aiagent/application/mcp/service/McpToolService.java` | tool 查询服务 |
| `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ToolNodeExecutorStrategy.java` | 工作流 TOOL 节点执行 |
