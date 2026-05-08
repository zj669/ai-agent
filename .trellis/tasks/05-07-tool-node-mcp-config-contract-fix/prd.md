# 修复 TOOL 节点 MCP 工具配置字段不一致导致调用失败

## Background

最近一次工作流执行出现节点调用失败：

- 时间：2026-05-07 19:08:07 到 19:08:09
- executionId：`b484ed5f-6c89-4b5e-9994-09067754b2fe`
- agentId：`1`
- 失败节点：`tool-1778151979576-3`
- 节点类型：`TOOL`
- 直接错误：`mcpToolName not configured`

日志显示执行器调度 TOOL 节点时工具名为空：

```text
19:08:08.459 Dispatching node: tool-1778151979576-3 (type: TOOL)
19:08:08.481 Executing MCP tool: null
19:08:08.484 Node tool-1778151979576-3 completed with status: FAILED
19:08:08.489 Execution b484ed5f-6c89-4b5e-9994-09067754b2fe finished with status: FAILED
```

数据库中的节点执行日志也记录：

```text
node_id: tool-1778151979576-3
node_type: TOOL
status: 3
error_message: mcpToolName not configured
outputs: null
```

## Root Cause

前端和后端对 TOOL 节点工具配置字段的契约不一致：

- 前端保存：`userConfig.selectedTool.fullName`
- 后端执行器读取：`node.getConfig().getString("mcpToolName")`

当前 `agent_info.id=1` 的图中，TOOL 节点配置实际为：

```json
{
  "userConfig": {
    "selectedTool": {
      "fullName": "mcp__3__web_search_exa",
      "serverId": 3,
      "toolName": "web_search_exa",
      "serverName": "exa"
    }
  }
}
```

但执行器只读取 `mcpToolName`，因此拿到 `null` 并直接失败。

相关代码：

- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ToolNodeExecutorStrategy.java`
- `ai-agent-foward/src/modules/workflow/components/NodeConfigTabs.tsx`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/converter/NodeConfigConverter.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/WorkflowGraphFactoryImpl.java`

## Non-Goals

- 不重构 MCP 连接池或 MCP 传输协议。
- 不调整 Knowledge 节点、LLM 节点、人审节点的业务语义。
- 不把 SSE `ResponseBodyEmitter has already completed` 作为本任务主修复目标；该问题是执行失败后的推送清理噪声，可单独建任务处理。
- 不修复 LLM 配置测试中的第三方接口 DNS、404 或连接重置问题；这些不是本次 TOOL 节点失败根因。

## Requirements

### 1. 后端兼容历史图配置

`ToolNodeExecutorStrategy` 获取 MCP 工具名时必须兼容以下配置：

1. 标准字段：`mcpToolName`
2. 历史/前端当前字段：`selectedTool.fullName`

优先级：

```text
mcpToolName > selectedTool.fullName
```

如果两者都缺失，保留明确失败语义，但错误信息应便于定位：

```text
MCP 工具未配置，请在 TOOL 节点选择工具
```

### 2. 前端保存标准字段

在 `NodeConfigTabs.tsx` 中，用户选择工具时除保存 `selectedTool` 外，还应同步保存：

```ts
mcpToolName: tool.fullName
```

清空工具时，需要同步清空：

```ts
selectedTool: null
mcpToolName: null
```

### 3. 图转换层保持向后兼容

`NodeConfigConverter` 当前会保留 userConfig 中额外字段。实现时不要破坏该行为。

如选择在转换层补充映射，也必须满足：

- 不覆盖已有非空 `mcpToolName`
- 仅在 `mcpToolName` 为空且 `selectedTool.fullName` 存在时补齐
- 保持其他节点类型行为不变

### 4. 执行输出字段契约校正

当前 TOOL 节点输出 schema 为：

```json
{
  "key": "tool_response"
}
```

但执行器成功时输出为：

```java
outputs.put("result", result.getContent());
```

需要统一输出字段，优先保持前端已暴露字段：

```text
tool_response
```

兼容性要求：

- 成功结果至少包含 `tool_response`
- 如需兼容旧引用，可同时保留 `result`
- LLM 模板中 `{{tool-xxx.output.tool_response}}` 必须能拿到工具返回内容

### 5. 调度失败后不继续误导性执行

当前日志显示 TOOL 节点失败后，另一个并行 Knowledge 节点仍完成，这是可接受的并行 in-flight 行为。但任务实现应确认：

- 工作流最终状态仍为 `FAILED`
- 失败节点错误可在节点执行日志中查询
- 不因后续并行节点成功覆盖失败状态

## Acceptance Criteria

- [ ] 已有历史图仅包含 `selectedTool.fullName` 时，TOOL 节点可以解析出 `mcp__3__web_search_exa` 并发起 MCP 调用。
- [ ] 新保存的 TOOL 节点同时包含 `selectedTool.fullName` 和 `mcpToolName`。
- [ ] TOOL 节点缺少工具配置时，返回明确业务错误，而不是只显示 `mcpToolName not configured`。
- [ ] TOOL 节点执行成功时输出包含 `tool_response`。
- [ ] LLM 节点引用 `{{tool-*.output.tool_response}}` 时可以读取工具返回内容。
- [ ] 回归 `agentId=1` 当前图或等价测试图，不再出现 `Executing MCP tool: null`。
- [ ] 后端相关单元测试或集成测试覆盖 `mcpToolName` 与 `selectedTool.fullName` 两种配置。
- [ ] 前端相关测试覆盖选择工具后写入 `mcpToolName`。
- [ ] 运行必要的后端/前端验证命令并记录结果。

## Suggested Implementation Plan

1. 后端先修执行器：
   - 在 `ToolNodeExecutorStrategy` 中增加 `resolveMcpToolName(Node node)`。
   - 支持读取 `mcpToolName` 和 `selectedTool.fullName`。
   - 成功输出加入 `tool_response`。

2. 前端补齐保存字段：
   - 修改 `ToolSection.handleSelectTool`。
   - 选择工具时同步 `mcpToolName`。
   - 清空工具时同步清空 `mcpToolName`。

3. 测试：
   - 后端测试覆盖历史图兼容。
   - 前端测试覆盖 `selectedTool` 与 `mcpToolName` 同步。
   - 如测试基础设施不足，至少补充最小可运行测试或记录无法运行原因。

4. 手工验证：
   - 使用当前 `agentId=1` 图或构造等价图执行。
   - 检查日志不再出现 `Executing MCP tool: null`。
   - 检查节点执行日志中 TOOL 输出包含 `tool_response`。

## Verification Commands

按实际变更范围选择执行：

```bash
# 后端
mvn -pl ai-agent-infrastructure -am test
mvn -pl ai-agent-interfaces -am test

# 前端
cd ai-agent-foward
npm test -- --run
npm run typecheck
```

如果项目脚本名称与上述命令不一致，以 `package.json` 和 Maven 模块实际配置为准。

