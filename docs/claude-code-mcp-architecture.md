# Claude-Code MCP 工具接入架构分析

> 基于 Claude-Code 源码探索，日期：2026-04-01

---

## 一、核心架构概览

Claude-Code 的 MCP (Model Context Protocol) 工具接入采用**适配器模式 + 配置驱动**的设计：

```
┌─────────────────────────────────────────────────────────────────┐
│                     MCP Server (外部服务)                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │  stdio   │  │   SSE    │  │   HTTP   │  │    WS    │        │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘        │
└───────┼─────────────┼─────────────┼─────────────┼───────────────┘
        │             │             │             │
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────┐
│              MCP Client (client.ts)                              │
│  connectToServer() → createMcpClient()                          │
│  fetchToolsForClient() → ListToolsResult                         │
│  fetchCommandsForClient() → ListPromptsResult                    │
└───────────────────────────────┬─────────────────────────────────┘
                                │  Tool[] + Command[] + Resource[]
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│           useManageMCPConnections (React Hook)                   │
│  1. 从 config.ts 加载所有 mcpServers 配置                        │
│  2. 批量连接服务器，更新 AppState.mcp                           │
│  3. 管理重连逻辑 (指数退避)                                     │
│  4. 监听工具变更通知                                            │
└───────────────────────────────┬─────────────────────────────────┘
                                │  AppState.mcp.tools
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              assembleToolPool() (tools.ts)                       │
│  builtInTools = getTools(permissionContext)                      │
│  mergedMcpTools = filterToolsByDenyRules(mcpTools)              │
│  return uniqBy([...builtInTools, ...mergedMcpTools])            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 二、关键设计模式

### 2.1 适配器模式：MCP Tool → Unified Tool

所有 MCP 工具通过 `MCPTool.ts` 模板 + 动态属性覆盖转换为统一 `Tool` 接口：

```typescript
// client.ts:1766-1988 - MCP 工具转换为统一 Tool 接口
toolsToProcess.map((tool): Tool => ({
  ...MCPTool,                              // 基础模板
  name: buildMcpToolName(client.name, tool.name),  // 工具名: mcp__server__tool
  mcpInfo: { serverName, toolName },        // MCP 元信息
  isMcp: true,
  description() { return tool.description },
  inputJSONSchema: tool.inputSchema,        // 直接透传 JSON Schema
  async call(args, context, ...) {
    // 调用 MCP: client.request({ method: 'tools/call', params: { name, arguments: args } })
  }
}))
```

### 2.2 配置驱动的多层级作用域

四层配置优先级（就近优先）：

| 作用域 | 配置文件 | 说明 |
|--------|---------|------|
| `local` | 当前工作目录配置 | 项目特定 |
| `project` | `.mcp.json` | 项目共享 |
| `user` | 全局用户配置 | 用户级别 |
| `claudeai` | Claude.ai 云端配置 | 云端同步 |
| `enterprise` | 企业托管配置 | 企业集中管理 |

```typescript
// config.ts - getAllMcpConfigs() 合并所有层级配置
export async function getAllMcpConfigs() {
  // enterprise 独占模式优先
  if (doesEnterpriseMcpConfigExist()) {
    return getClaudeCodeMcpConfigs()
  }
  // 合并各层级配置，后者覆盖前者
  const servers = Object.assign({}, claudeaiMcpServers, claudeCodeServers)
  return { servers, errors }
}
```

### 2.3 拒绝规则前置过滤

工具拒绝规则（deny rules）在**工具池组装时**生效，而非调用时：

```typescript
// tools.ts - assembleToolPool()
export function assembleToolPool(permissionContext, mcpTools) {
  const builtInTools = getTools(permissionContext)
  // 前置过滤：MCP 工具在注册时就应用拒绝规则
  const allowedMcpTools = filterToolsByDenyRules(mcpTools, permissionContext)
  return uniqBy([...builtInTools, ...allowedMcpTools], 'name')
}
```

---

## 三、核心代码路径

| 步骤 | 文件 | 核心函数 |
|------|------|---------|
| 配置读取 | `services/mcp/config.ts` | `getAllMcpConfigs()` |
| 服务器连接 | `services/mcp/client.ts` | `connectToServer()` |
| 工具获取 | `services/mcp/client.ts` | `fetchToolsForClient()` |
| 工具适配 | `services/mcp/client.ts:1766` | 动态映射逻辑 |
| 状态管理 | `services/mcp/useManageMCPConnections.ts` | React Hook |
| 工具池组装 | `tools.ts` | `assembleToolPool()` |
| 工具命名 | `services/mcp/mcpStringUtils.ts` | `buildMcpToolName()` |

---

## 四、传输协议支持

| 协议 | 类型 | 用途 | 重连支持 |
|------|------|------|---------|
| `stdio` | 子进程 | 本地 MCP 服务器 | ❌ 不支持 |
| `sse` | HTTP + SSE | 远程 MCP 服务器 | ✅ 指数退避 |
| `http` | HTTP POST | 远程 MCP 服务器 | ❌ 不支持 |
| `ws` | WebSocket | 实时双向通信 | ❌ 不支持 |
| `sdk` | 内联 | IDE 内部 MCP | ❌ 不支持 |

### 重连机制（仅 SSE）

```typescript
// useManageMCPConnections.ts - 指数退避重连
const MAX_RECONNECT_ATTEMPTS = 5
const INITIAL_BACKOFF_MS = 1000
const MAX_BACKOFF_MS = 30000

const backoffMs = Math.min(
  INITIAL_BACKOFF_MS * Math.pow(2, attempt - 1),
  MAX_BACKOFF_MS
)
```

---

## 五、用户可扩展性分析

### 5.1 扩展点矩阵

| 扩展点 | 当前实现 | 可扩展性 |
|--------|---------|---------|
| 内置工具 | `tools.ts` 硬编码 | ❌ 需修改源码 |
| MCP 服务器 | `config.ts` 读取配置 | ✅ 用户配置即可 |
| MCP 工具名规则 | `mcpStringUtils.ts` | ❌ 标准化无需改 |
| 传输协议 | `client.ts` | ✅ 可扩展新协议 |

### 5.2 用户扩展 MCP 服务器标准流程

**Step 1: 实现 MCP 服务器**

```typescript
// 使用 @modelcontextprotocol/sdk
import { Server } from '@modelcontextprotocol/sdk/server.js'

const server = new Server(
  { name: "my-tools", version: "1.0.0" },
  { capabilities: { tools: {} } }
)

server.setRequestHandler({ method: 'tools/list' }, async () => ({
  tools: [
    {
      name: "hello",
      description: "Say hello to the user",
      inputSchema: {
        type: "object",
        properties: {
          name: { type: "string" }
        }
      }
    }
  ]
}))
```

**Step 2: 注册到配置文件**

```json
// .mcp.json (项目级别)
{
  "mcpServers": {
    "my-tools": {
      "type": "stdio",
      "command": "node",
      "args": ["/path/to/server.js"],
      "env": {
        "API_KEY": "xxx"
      }
    }
  }
}
```

**Step 3: Claude Code 自动发现**

```
启动 → getAllMcpConfigs() → connectToServer()
     → fetchToolsForClient() → 工具自动注册
```

---

## 六、与 ai-agent 集成的参考价值

Claude-Code 的 MCP 接入设计为 ai-agent 工作流引擎的 Tool 节点扩展提供了参考：

### 6.1 适配器模式

```typescript
// 伪代码：ai-agent ToolNodeExecutorStrategy 扩展 MCP 支持
class MCPToolAdapter {
  constructor(serverConfig: McpServerConfig) {
    this.client = createMcpClient(serverConfig)
  }

  async listTools() {
    const result = await this.client.request(
      { method: 'tools/list' },
      ListToolsResultSchema
    )
    return result.tools.map(tool => this.adaptTool(tool))
  }

  adaptTool(mcpTool): ToolDefinition {
    return {
      name: `mcp__${this.serverName}__${mcpTool.name}`,
      description: mcpTool.description,
      inputSchema: mcpTool.inputSchema,
      async execute(args) {
        const result = await this.client.request({
          method: 'tools/call',
          params: { name: mcpTool.name, arguments: args }
        })
        return this.processResult(result)
      }
    }
  }
}
```

### 6.2 配置驱动的工具注册

```typescript
// ToolNodeExecutorStrategy 支持动态 MCP 工具配置
interface ToolNodeConfig {
  type: 'builtin' | 'mcp'
  mcpServers?: Record<string, McpServerConfig>
}
```

---

## 七、总结

| 维度 | 评估 |
|------|------|
| 架构设计 | 适配器模式 + 配置驱动，解耦良好 |
| 扩展性 | MCP 协议标准化，用户零代码扩展 |
| 可维护性 | 模块化清晰，类型安全 |
| 局限性 | 内置工具仍需修改源码 |

**核心洞察**：Claude-Code 通过 MCP 协议实现了"配置即插件"的扩展机制，工具提供方只需遵循 MCP 协议，无需了解 Claude Code 内部实现。

---

## 参考文件

- `Claude-Code/src/services/mcp/client.ts` - MCP 客户端核心
- `Claude-Code/src/services/mcp/config.ts` - 配置管理
- `Claude-Code/src/services/mcp/types.ts` - 类型定义
- `Claude-Code/src/services/mcp/useManageMCPConnections.ts` - React Hook
- `Claude-Code/src/tools.ts` - 工具池组装
- `Claude-Code/src/tools/MCPTool/MCPTool.ts` - MCP 工具模板
- `Claude-Code/src/services/mcp/mcpStringUtils.ts` - 工具命名
