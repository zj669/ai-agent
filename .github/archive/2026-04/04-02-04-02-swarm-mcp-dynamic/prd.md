# 修复 Swarm MCP 工具集成 — 动态工具策略

## Goal

修复 MCP 工具在 Swarm 多智能体协作中无法被发现和调用的三重阻断问题，并实现**动态工具策略**：
- MCP 工具总数 **< 20** → 直接内嵌到 Prompt 工具列表
- MCP 工具总数 **≥ 20** → 不内嵌，改为提供 `query_mcp_tools` 查询工具，LLM 按需查询

## Problem Statement（三重阻断）

当前 MCP 工具集成在 Swarm 层存在三个阻断点：

1. **Prompt 无 MCP 工具列表**：`SwarmPromptService.getPrompt()` 只输出 `SwarmToolFilter` 白名单内的内置工具，MCP 工具从不进入 system prompt，LLM 根本不知道存在
2. **执行层白名单拒绝**：`SwarmAgentRunner.executeToolCall()` 调用 `toolFilter.isAllowed(role, toolName)`，MCP 工具名格式为 `mcp__{serverId}__{toolName}`，永远不在白名单，返回"不允许使用"错误
3. **全局 MCP 池无 workspace 隔离**：`IMcpToolRegistry.getAllTools()` 返回所有已连接服务器的全局工具，未按 workspace/userId 隔离

## Requirements

### R1: 修复 MCP 工具白名单阻断

`SwarmToolFilter.isAllowed()` 对 `mcp__*` 前缀工具默认放行：

```java
public boolean isAllowed(SwarmRole role, String toolName) {
    // MCP 工具由 McpToolCallbackAdapter 动态注入，默认允许
    if (toolName != null && toolName.startsWith("mcp__")) {
        return true;
    }
    return getAllowedToolNames(role).contains(toolName);
}
```

### R2: 动态工具策略

`McpToolCallbackAdapter` 增加动态策略方法：

```java
/**
 * 判断当前 MCP 工具集是否应内嵌到 Prompt（< 20 工具）或使用查询模式（≥ 20）
 */
public boolean shouldEmbedInPrompt(List<McpToolDefinition> tools);

/**
 * 构建内嵌到 Prompt 的工具描述文本（< 20 工具时使用）
 */
public String buildEmbeddableToolSection(List<McpToolDefinition> tools);

/**
 * 获取查询工具的 @Tool 描述（≥ 20 工具时，追加到 SwarmTools）
 * 工具名：query_mcp_tools
 * 功能：按关键词查询 MCP 工具列表，返回工具名 + 描述
 */
public ToolCallback createQueryMcpToolsCallback(Long userId, Long workspaceId);
```

### R3: PromptService 集成动态工具

修改 `SwarmPromptService.getPrompt()`：

```java
// 4. Tool List — 内嵌 MCP 工具（< 20）或查询工具（≥ 20）
if (mcpToolCallbackAdapter.shouldEmbedInPrompt(workspaceId)) {
    joiner.add(toolFilter.buildToolSection(role));
    joiner.add(mcpToolCallbackAdapter.buildEmbeddableToolSection(workspaceId));
} else {
    joiner.add(toolFilter.buildToolSection(role));
    joiner.add(mcpToolCallbackAdapter.buildQueryToolSection(role));
}
```

### R4: Workspace 隔离

`IMcpToolRegistry` / `McpToolRegistryAdapter` 增加 workspace(userId) 维度的工具查询：

```java
/**
 * 获取指定用户的所有已连接 MCP 服务器工具（用于 workspace 隔离）
 */
List<McpToolDefinition> getToolsByUserId(Long userId);
```

### R5: SwarmAgentRunner 适配

- `buildAllToolCallbacks()` 改为每次 LLM 调用前重新构建（已是动态，但需确保传入正确的 userId）
- `executeToolCall()` 的 MCP 工具拦截逻辑已由 R1 修复

## Acceptance Criteria

- [ ] `SwarmToolFilter.isAllowed()` 对 `mcp__*` 工具返回 true
- [ ] MCP 工具 < 20 时，Prompt 中包含完整工具名 + 描述列表
- [ ] MCP 工具 ≥ 20 时，Prompt 中包含 `query_mcp_tools` 工具，LLM 可调用查询
- [ ] `query_mcp_tools` 工具可按关键词搜索 MCP 工具，返回匹配的工具列表
- [ ] `IMcpToolRegistry.getToolsByUserId(userId)` 仅返回该用户的 MCP 服务器工具
- [ ] 多 workspace 场景下，A workspace 的 Agent 无法看到 B workspace 的 MCP 工具
- [ ] 现有 Swarm 单元测试通过（`SwarmPromptServiceTest`, `SwarmToolFilterTest`）

## Technical Notes

### 文件修改清单

| 文件 | 修改内容 |
|------|---------|
| `SwarmToolFilter.java` | `isAllowed()` 增加 `mcp__*` 前缀放行 |
| `McpToolCallbackAdapter.java` | 新增动态策略方法（`shouldEmbedInPrompt`, `buildEmbeddableToolSection`, `createQueryMcpToolsCallback`, `buildQueryToolSection`）|
| `SwarmPromptService.java` | 注入 `McpToolCallbackAdapter`，集成 R2/R3 逻辑 |
| `IMcpToolRegistry.java` | 新增 `getToolsByUserId(Long userId)` 接口 |
| `McpToolRegistryAdapter.java` | 实现 `getToolsByUserId()` |
| `McpConnectionPool.java` | `getCachedToolsByUserId()` 按 userId 过滤 |
| `SwarmAgentRuntimeService.java` | 构造 `SwarmTools` 时传入 `userId`（已有），确认无其他修改 |

### 不修改的内容（避免过度设计）

- `SwarmAgentRunner` 的 ReAct 循环结构保持不变
- `SwarmTools` 的现有工具方法保持不变（`create_worker`, `send` 等）
- Domain 层实体（`SwarmAgent`, `SwarmMessage` 等）保持不变
- 不引入新的 Repository 或新的状态管理机制

### 阈值常量

工具内嵌 vs 查询模式的阈值：`MAX_EMBED_TOOLS = 20`，在 `McpToolCallbackAdapter` 中定义为 `private static final int`。
