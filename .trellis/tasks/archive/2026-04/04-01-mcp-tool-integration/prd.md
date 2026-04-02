# MCP 工具接入模块 — 全栈实现

> 创建时间：2026-04-01
> 来源：Claude-Code MCP 架构分析 + ai-agent 集成设计讨论

---

## 一、背景与目标

### 背景

- Claude-Code 的 MCP（Model Context Protocol）工具接入采用**适配器模式 + 配置驱动**，实现了"配置即插件"的扩展机制
- ai-agent 目前有两套 Agent 执行体系（Swarm Agent + Workflow Engine），均无 MCP 工具支持
- 两套 Agent **共用同一 MCP 连接池**，且**可能并发调用工具**

### 目标

实现一套共享的 MCP 工具模块：
1. **前端**：用户可自定义配置 MCP 服务器（CRUD 页面）
2. **后端**：Swarm Agent 和 Workflow Engine 均通过统一接口调用 MCP 工具
3. **按需发现**：工具在首次被调用时才连接 MCP 服务器（懒连接）
4. **并发安全**：两 Agent 并发调用时互不干扰

---

## 二、需求确认

### 来自用户确认

| 需求 | 确认结果 |
|------|---------|
| MCP 服务器配置 | 用户自定义，前端 CRUD 页面 |
| 工具发现时机 | **动态按需发现**，不启动时全连接 |
| 连接池共享 | 两个 Agent 共用同一 MCP 连接池 |
| 并发调用 | 两个 Agent 可能同时调用工具 |
| MCP 接入现状 | 从零接入，`ToolNodeExecutorStrategy` 目前是空壳 |

### 待确认（执行前需确认）

1. **MCP Java SDK 选型**：主流选项待 research 后推荐
2. **服务器状态推送**：SSE 实时推送 vs 前端轮询

---

## 三、架构设计

### 全局分层架构

```
ai-agent-foward (Frontend)
  /mcp  →  MCPListPage → AddServerModal / EditServerModal
  └── shared/api/adapters/mcpAdapter.ts

ai-agent-interfaces (REST Controller)
  McpServerController: CRUD /api/mcp/servers
  McpToolController: GET /api/mcp/tools

ai-agent-application
  McpServerService: 业务编排
  McpToolRegistryService: 工具注册表

ai-agent-domain (Pure Java, no framework)
  McpServer (聚合根)
  McpToolDefinition (值对象)
  IMcpToolRegistry (端口)
  IMcpConnectionManager (端口)

ai-agent-infrastructure
  McpServerRepositoryImpl (MyBatis Plus)
  McpConnectionPool (连接池管理)
  McpToolRegistryAdapter (IMcpToolRegistry 实现)
  McpToolExecutorAdapter (工具调用执行)
  McpClientFactory (MCP SDK Client 工厂)
```

### 消费者接入点

```
SwarmAgentRunner
  └→ SwarmTools (@Tool 方法)
      └→ McpDynamicToolCallback (McpToolRegistry → FunctionTool)

ToolNodeExecutorStrategy
  └→ McpToolExecutor.execute(mcp__server__tool, args)
```

---

## 四、数据库设计

### 表结构

```sql
CREATE TABLE IF NOT EXISTS `mcp_server_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '服务器ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `name` varchar(100) NOT NULL COMMENT '服务器名称（用户自定义）',
  `server_type` varchar(20) NOT NULL COMMENT '类型: stdio | sse | http',
  `config_json` json NOT NULL COMMENT 'MCP 服务器配置（JSON）',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `status` varchar(20) DEFAULT 'DISCONNECTED' COMMENT '状态: DISCONNECTED|CONNECTING|CONNECTED|ERROR',
  `description` text COMMENT '描述',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '是否删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MCP 服务器配置表';
```

### config_json 结构

```json
// stdio 类型
{ "command": "npx", "args": ["-y", "@anthropic/mcp-server"], "env": { "API_KEY": "xxx" } }

// sse 类型
{ "url": "https://mcp.example.com/sse", "headers": { "Authorization": "Bearer xxx" } }

// http 类型
{ "url": "https://mcp.example.com/mcp", "headers": { "Authorization": "Bearer xxx" } }
```

---

## 五、Domain 层设计

### 包结构

```
ai-agent-domain/src/main/java/com/zj/aiagent/domain/mcp/
├── entity/
│   └── McpServer.java              # 聚合根
├── valobj/
│   ├── McpServerId.java            # 值对象
│   ├── McpServerConfig.java        # 值对象：command/url/headers/env
│   ├── McpServerStatus.java        # 值对象：状态枚举
│   └── McpToolDefinition.java      # 值对象：单个工具定义
└── port/
    ├── IMcpServerRepository.java    # 仓储端口
    ├── IMcpToolRegistry.java       # 工具注册表端口
    └── IMcpConnectionManager.java  # 连接生命周期端口
```

### 核心接口

```java
// IMcpToolRegistry — 两套 Agent 都通过它获取 MCP 工具
public interface IMcpToolRegistry {
    List<McpToolDefinition> getAllTools();                          // 获取所有已连接服务器的工具
    List<McpToolDefinition> getToolsByServer(Long serverId);       // 获取指定服务器的工具
    McpToolResult execute(Long serverId, String toolName, Map<String, Object> args, String executionId);
    void abort(String executionId);                                 // 中止指定 executionId 的所有调用
    List<OpenAiApi.FunctionTool> toFunctionTools();                // 转为 Spring AI FunctionTool schema
    CompletableFuture<Void> refreshServer(Long serverId);           // 刷新指定服务器
}

// IMcpConnectionManager — 连接生命周期
public interface IMcpConnectionManager {
    CompletableFuture<List<McpToolDefinition>> connectAndDiscover(Long serverId);  // 懒连接
    boolean isConnected(Long serverId);
    void disconnect(Long serverId);
    void disconnectAll();                                            // 应用关闭时
}
```

---

## 六、Infrastructure 层设计

### 连接池核心：McpConnectionPool

```java
@Component
public class McpConnectionPool implements IMcpConnectionManager {
    // serverId → (状态, MCP Client实例, 工具缓存)
    private final ConcurrentHashMap<Long, ServerConnection> pool = new ConcurrentHashMap<>();
    // serverId → connectAndDiscover() 的 Promise（防止并发重复连接）
    private final ConcurrentHashMap<Long, CompletableFuture<List<McpToolDefinition>>> pendingConnections = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<List<McpToolDefinition>> connectAndDiscover(Long serverId) {
        return pendingConnections.computeIfAbsent(serverId, id ->
            doConnect(serverId).whenComplete((r, e) -> pendingConnections.remove(id))
        );
    }
}
```

### 并发安全机制

| 场景 | 解决方案 |
|------|---------|
| 两 Agent 同时调用同一 MCP 服务器 | MCP SDK `Client` 内部 JSON-RPC `id` 匹配响应，天然并发安全 |
| Agent A 的中止信号不影响 Agent B | 每个 `execute(executionId)` 独立，`abort()` 按 `executionId` 取消 |
| 多个调用同时触发同一服务器连接 | `pendingConnections.computeIfAbsent()` 共享同一 `CompletableFuture` |
| 工具列表变更通知 | MCP SDK 支持 `ToolListChangedNotification` → 刷新内存缓存 |

---

## 七、Application 层接入点

### Swarm Agent 接入

```java
// SwarmTools.java — 注入 IMcpToolRegistry，MCP 工具并入 @Tool 列表
@Slf4j
public class SwarmTools {
    private final IMcpToolRegistry mcpToolRegistry;
    private final SwarmToolExecutor swarmToolExecutor;  // 原有蜂群内置工具

    public List<OpenAiApi.FunctionTool> getAllMcpFunctionTools() {
        return mcpToolRegistry.toFunctionTools();
    }
    // LLM 请求时：ToolCallbacks.from(swarmTools) + mcpToolRegistry.toFunctionTools()
    // → ChatModel 收到完整工具列表（包括 MCP 工具）
}
```

### Workflow Engine 接入

```java
// ToolNodeExecutorStrategy.java — 实现 MCP 工具节点执行
@Component
public class ToolNodeExecutorStrategy implements NodeExecutorStrategy {
    private final IMcpToolRegistry mcpToolRegistry;

    @Override
    public CompletableFuture<NodeExecutionResult> executeAsync(
            Node node, Map<String, Object> resolvedInputs, StreamPublisher stream) {
        String toolFullName = node.getConfig().getString("mcpToolName"); // mcp__server__tool
        String executionId = node.getExecutionId();
        return mcpToolRegistry.execute(serverId, toolName, args, executionId)
            .thenApply(result -> NodeExecutionResult.success(Map.of("result", result)));
    }
}
```

---

## 八、REST API 契约

| Method | Path | 说明 |
|--------|------|------|
| GET | `/api/mcp/servers` | 获取当前用户所有服务器 |
| POST | `/api/mcp/servers` | 新建服务器配置 |
| PUT | `/api/mcp/servers/{id}` | 更新配置 |
| DELETE | `/api/mcp/servers/{id}` | 删除（逻辑删除） |
| POST | `/api/mcp/servers/{id}/connect` | 手动触发连接 |
| POST | `/api/mcp/servers/{id}/disconnect` | 断开连接 |
| GET | `/api/mcp/servers/{id}/tools` | 获取该服务器的工具列表 |
| GET | `/api/mcp/tools` | 获取所有已连接服务器的工具汇总 |

---

## 九、前端页面设计

### 页面结构

```
ai-agent-foward/src/modules/mcp/                    # 【NEW】
  pages/
    McpServerListPage.tsx                          # 列表 + Add/Edit Modal
    McpServerDetailPage.tsx                        # 【NEW】服务器详情页：显示工具列表
  api/
    mcpAdapter.ts                                  # REST API 调用
  components/
    ServerForm.tsx                                 # Add/Edit 共用表单
    ServerStatusTag.tsx                            # 连接状态标签
    ToolListPanel.tsx                              # 【NEW】工具列表面板
  types/
    mcp.ts                                         # TypeScript 类型
```

### McpServerDetailPage 路由与功能

**路由**：`/mcp/:id`（点击服务器卡片 → 进入详情页）

**功能**：
- 显示服务器基本信息（名称、类型、状态、描述）
- 连接状态：DISCONNECTED / CONNECTING / CONNECTED / ERROR
- **CONNECTED 状态**：展示该服务器下所有工具（`GET /api/mcp/servers/{id}/tools`）
- 每个工具卡片展示：工具名称、描述、输入参数
- 支持刷新工具列表
- 支持手动 disconnect

**交互流程**：
```
列表页 → 点击"连接" → 状态变为 CONNECTING → CONNECTED
     → 点击服务器名称/卡片 → 进入详情页 → 查看工具列表
```

### 路由

新增以下路由，挂载在 `RequireAuth` 下的 `AppShell` 中：

| Path | Component | 说明 |
|------|-----------|------|
| `/mcp` | `McpServerListPage` | MCP 服务器列表 |
| `/mcp/:id` | `McpServerDetailPage` | 服务器详情 + 工具列表 |

---

## 十、实现进度

> ✅ = 已完成 | 🔧 = 进行中 | ⬜ = 待开始

| 步骤 | 内容 | 状态 |
|------|------|------|
| 1 | 数据库层 — `mcp_server_config` 表 + MyBatis Plus Mapper | ✅ |
| 2 | Domain 层 — 聚合根、值对象、端口接口 | ✅ |
| 3 | Infrastructure 层 — `McpConnectionPool`、JSON-RPC 实现、`IMcpToolRegistry` 实现 | ✅ |
| 4 | Application 层 — `McpServerService`、REST Controller | ✅ |
| 5 | Frontend 层 — MCP 服务器管理页面 | ✅ |
| 6 | Swarm 接入 — `McpToolCallbackAdapter` 创建 + 合并入 `SwarmAgentRuntimeService` | ✅ |
| 7 | Swarm 接入 — BugFix：`FunctionToolCallback<Map, String>` 类型修复 | ✅ |
| 8 | Swarm 接入 — BugFix：动态 `buildAllToolCallbacks()` + 动态查找 map | ✅ |
| 9 | Workflow 接入 — `ToolNodeExecutorStrategy.executeAsync()` | ✅ |
| 10 | 测试 & 联调 | ⬜ |

---

## 十一、待确认事项

- [x] **MCP Java SDK 选型**：✅ 已确认 **Spring AI MCP Starter**（`spring-ai-starter-mcp-client`，BOM 管控），三期传输协议全覆盖
- [x] **服务器状态推送**：✅ 已确认 **一期用轮询**（前端 `GET /api/mcp/servers/{id}` 每 5-10s），二期再上 SSE
- [x] **OAuth 支持**：✅ 已确认 **二期再做**，一期仅支持 stdio（命令+环境变量）、sse/streamable-http（URL+headers）

---

---

## 十三、Bug Fix：SwarmAgent 无法看到 MCP 工具（根因修复）

> 发现时间：2026-04-01
> 根因：通过代码链路追踪定位

### 问题现象

用户在 MCP 管理页面配置了服务器并成功连接，但 SwarmAgent 在对话中无法感知到这些工具。

### 根因分析

```
SwarmAgentRunner 构造（第143行）
  └→ toolCallbacks = ToolCallbacks.from(swarmTools)
       └→ 仅扫描 @Tool 注解方法 → 找到 2 个兜底工具：
             ├─ list_mcp_tools()    ← 返回 JSON 字符串，LLM 难以解析
             └─ call_mcp_tool()    ← 需传 serverId+toolName+argsJson，LLM 不知道有哪些工具

【缺失】McpToolDefinition[] → ToolCallback[] 转换从未发生
  └→ LLM 看到的工具列表 = { list_mcp_tools, call_mcp_tool }
  └→ 真正配置的 MCP 工具 = 完全不可见！
```

### 修复方案

#### 1. 补充 `IMcpToolRegistry.toFunctionTools()`

```java
// IMcpToolRegistry.java — 新增方法
List<OpenAiApi.FunctionTool> toFunctionTools();

// McpToolRegistryAdapter.java — 实现
@Override
public List<OpenAiApi.FunctionTool> toFunctionTools() {
    return getAllTools().stream()
        .map(def -> new OpenAiApi.FunctionTool(
            def.getFullName(),  // "mcp__1__read_file"
            FunctionToolMetadata.builder()
                .description(def.getDescription())
                .parameters(JsonSchema.of(def.getInputSchema()))
                .build()))
        .collect(toList());
}
```

#### 2. 新增 `McpToolCallbackAdapter`（桥接 McpToolDefinition → ToolCallback）

每个 `McpToolDefinition` 对应一个 `FunctionToolCallback`，`call()` 时委托给 `McpToolRegistry.execute()`：

```java
// ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/mcp/adapter/McpToolCallbackAdapter.java
@Component
public class McpToolCallbackAdapter {

    private final IMcpToolRegistry mcpToolRegistry;
    private final ObjectMapper objectMapper;

    /**
     * 获取所有已连接 MCP 服务器的工具回调
     */
    public List<ToolCallback> getAllMcpToolCallbacks() {
        return mcpToolRegistry.getAllTools().stream()
            .map(this::toFunctionToolCallback)
            .collect(Collectors.toList());
    }

    /**
     * 获取指定服务器的工具回调
     */
    public List<ToolCallback> getToolCallbacksByServer(Long serverId) {
        return mcpToolRegistry.getToolsByServer(serverId).stream()
            .map(this::toFunctionToolCallback)
            .collect(Collectors.toList());
    }

    private FunctionToolCallback toFunctionToolCallback(McpToolDefinition def) {
        return FunctionToolCallback.builder(
            def.getFullName(),  // name = "mcp__1__read_file"
            (Function<String, String>) input -> {
                // input 是 JSON 字符串参数
                Map<String, Object> args = parseArgs(input);
                String executionId = "swarm-" + System.currentTimeMillis();
                McpToolResult result = mcpToolRegistry
                    .execute(def.getServerId(), def.getToolName(), args, executionId)
                    .join();
                return result.isSuccess()
                    ? result.getContent()
                    : "Error: " + result.getErrorMessage();
            }
        )
        .description("[MCP:" + def.getServerName() + "] " + def.getDescription())
        .inputType(String.class)  // MCP 工具参数是原始 JSON，不做 POJO 映射
        .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArgs(String input) {
        if (input == null || input.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(input, Map.class);
        } catch (Exception e) {
            log.warn("[McpToolCallbackAdapter] Failed to parse tool args: {}", input);
            return Collections.emptyMap();
        }
    }
}
```

#### 3. 修改 `SwarmAgentRuntimeService.startAgent()`

将 MCP ToolCallback 合并到 Agent 的工具列表中：

```java
// SwarmAgentRuntimeService.startAgent() — 修改
@Autowired
private McpToolCallbackAdapter mcpToolCallbackAdapter;

public void startAgent(SwarmAgent agent) {
    // ...
    // 为每个 agent 创建独立的 SwarmTools 实例
    SwarmTools swarmTools = new SwarmTools(..., mcpToolRegistry, ...);

    // 获取所有 MCP 工具回调
    List<ToolCallback> mcpCallbacks = mcpToolCallbackAdapter.getAllMcpToolCallbacks();
    ToolCallback[] swarmToolCallbacks = ToolCallbacks.from(swarmTools);

    // 合并：原有内置工具 + MCP 工具
    ToolCallback[] allToolCallbacks = ArrayUtils.addAll(swarmToolCallbacks,
        mcpCallbacks.toArray(new ToolCallback[0]));

    SwarmAgentRunner runner = new SwarmAgentRunner(
        agent, ..., swarmTools, allToolCallbacks, ...
    );
}
```

### 修复后的完整链路

```
LLM 请求工具列表
  ↓
SwarmAgentRunner.toolCallbacks = {
    ToolCallbacks.from(swarmTools),          // list_mcp_tools + call_mcp_tool
    mcpToolCallbackAdapter.getAllMcpToolCallbacks()  // mcp__1__read_file, ...
}
  ↓
LLM 收到完整工具 schema：
{
  "type": "function",
  "function": {
    "name": "mcp__1__read_file",
    "description": "[MCP:my-server] Read file contents",
    "parameters": { "type": "object", "properties": {...} }
  }
}
  ↓
LLM 直接调用 mcp__1__read_file
  ↓
FunctionToolCallback.call('{"path": "/tmp/test.txt"}')
  ↓
McpToolRegistry.execute(serverId=1, toolName="read_file", args={path:...})
```

### 修复后的剩余工作

- [x] ~~新建 `McpToolCallbackAdapter.java`~~ → ✅ 已创建
- [x] ~~修改 `SwarmAgentRuntimeService`，注入并使用 `McpToolCallbackAdapter`~~ → ✅ 已修改
- [x] ~~修改 `SwarmAgentRunner`，接受外部传入 `toolCallbacks`~~ → ✅ 已修改
- [x] ~~BugFix：`FunctionToolCallback<String, String>` → `FunctionToolCallback<Map, String>`~~ → ✅ 已修复
  - 根因：`inputType(String.class)` 时 Spring AI 将 JSON 对象 token 反序列化为 String 失败
  - 修复：`inputType(Map.class)` + `Map<String, Object>` 输入类型，Spring AI 自动完成 JSON→Map 转换
  - 相关文件：`McpToolCallbackAdapter.java` 已重写，移除 `parseArgs()` 方法
- [x] ~~BugFix：MCP 工具对 LLM 不可见（工具列表快照问题）~~ → ✅ 已修复（见下方根因③）
- [ ] 端到端测试：配置 MCP 服务器 → 连接 → SwarmAgent 对话中直接调用工具

---

## 十五、Bug Fix：MCP 工具对 LLM 完全不可见（动态工具列表）

> 发现时间：2026-04-01
> 根因：工具列表在 Agent 启动时一次性快照，MCP 服务器可能在运行后才连接

### 问题现象

用户已连接 Tavily MCP 服务器，但 LLM 回复中只提到 SwarmTools 内置工具（`writing_session` 等），看不到任何 `mcp__1__xxx` 工具。

### 根因分析

存在**两个**导致 MCP 工具不可见的问题：

**问题 A — 启动时连接池为空（快照无工具）**

```
SwarmAgentRuntimeService.startAgent()
  └→ mcpToolCallbackAdapter.getAllMcpToolCallbacks()  ← 启动时调用
       └→ mcpToolRegistry.getAllTools()
            └→ connectionPool.getAllCachedTools()  ← pool 为空！
                 └→ return []  （服务器尚未连接）
  └→ toolCallbacks = [SwarmTools 内置工具]  ← MCP 工具丢失
```

用户先启动 Agent，后连接 MCP 服务器 → `pool` 始终为空。

**问题 B — toolCallbackMap 不含 MCP 工具（执行时查找失败）**

即使 A 修复，`toolCallbackMap` 在构造时只填充了 SwarmTools。当 LLM 调用 `mcp__1__tavily_research` 时：

```
executeToolCall("mcp__1__tavily_research", ...)
  └→ toolCallbackMap.get("mcp__1__tavily_research") → null！
  └→ return "Unknown tool"
```

### 修复方案

#### 1. SwarmAgentRunner 接收 `McpToolCallbackAdapter` 而非静态数组

```java
// SwarmAgentRunner 构造器签名变更
public SwarmAgentRunner(
    ...,                          // 其他参数不变
    SwarmTools swarmTools,        // 新增：SwarmTools 实例
    McpToolCallbackAdapter mcpToolCallbackAdapter  // 新增：动态 MCP 适配器
) {
    this.swarmTools = swarmTools;
    this.mcpToolCallbackAdapter = mcpToolCallbackAdapter;
    this.swarmToolCallbacks = ToolCallbacks.from(swarmTools);  // 仅 SwarmTools 静态构建
    // toolCallbackMap 仅用于日志记录，不再用于执行查找
}

// 每次 LLM 调用前动态合并
private ToolCallback[] buildAllToolCallbacks() {
    List<ToolCallback> mcpCallbacks = mcpToolCallbackAdapter.getAllMcpToolCallbacks();
    return ArrayUtils.addAll(swarmToolCallbacks, mcpCallbacks.toArray(ToolCallback[]::new));
}
```

#### 2. LLM 调用使用动态合并后的工具列表

```java
// processHumanMessages / processAgentMessages 中的 LLM 调用
SwarmLlmResponse response = llmCaller.callStreamWithTools(
    messages,
    buildAllToolCallbacks(),  // 每次调用前实时构建
    ...
);
```

#### 3. executeToolCall 查找改用动态完整 map

```java
private String executeToolCall(String toolName, String toolArgs) {
    // 构建当前所有可用工具的 map（MCP 可能随时加入）
    Map<String, ToolCallback> allCallbacks = new HashMap<>();
    for (ToolCallback cb : buildAllToolCallbacks()) {
        allCallbacks.put(cb.getToolDefinition().name(), cb);
    }
    ToolCallback callback = allCallbacks.get(toolName);
    if (callback == null) {
        return errorJson("Unknown tool: " + toolName);
    }
    return callback.call(toolArgs);
}
```

### 修复后的完整链路

```
Agent 启动时（MCP 服务器尚未连接）
  └→ buildAllToolCallbacks() → [SwarmTools]  ← 只有内置工具，LLM 看不到 MCP 工具

用户在前端连接 Tavily MCP → connectionPool.connectAndDiscover()
  └→ pool.get(serverId).tools = [tavily_search, tavily_get_json...]
  └→ getAllCachedTools() → 返回 tavily 工具

LLM 下一次调用前
  └→ buildAllToolCallbacks() → [SwarmTools, mcp__1__tavily_search, ...]  ← MCP 工具出现！
  └→ LLM 收到完整工具 schema
  └→ LLM 调用 mcp__1__tavily_search
  └→ executeToolCall() 从 allCallbacks 找到 callback → call() 成功
```

### 修复后的剩余工作

- [x] ~~MCP 工具快照问题 → 动态 buildAllToolCallbacks()~~ → ✅ 已修复
- [x] ~~executeToolCall 查找失败 → 动态构建 allCallbacks~~ → ✅ 已修复
- [ ] 端到端测试：配置 MCP 服务器 → 连接 → SwarmAgent 对话中直接调用工具

---

## 十四、参考文档

- Claude-Code MCP 架构分析：`docs/claude-code-mcp-architecture.md`
- Claude-Code 源码位置：`Claude-Code/src/services/mcp/`
- 现有 `ToolNodeExecutorStrategy`：`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ToolNodeExecutorStrategy.java`
- 现有 `SwarmTools`：`ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/runtime/SwarmTools.java`
