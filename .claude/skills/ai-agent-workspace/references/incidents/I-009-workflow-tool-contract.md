# I-009: TOOL 节点 MCP 工具配置字段契约不一致

## 故障签名

典型日志：

```text
[Scheduler] Dispatching node: tool-... (type: TOOL)
[Tool Node tool-...] Executing MCP tool: null
[Scheduler] Node tool-... completed with status: FAILED
```

节点执行日志常见错误：

```text
mcpToolName not configured
```

或修复后的明确业务错误：

```text
MCP 工具未配置，请在 TOOL 节点选择工具
```

## 触发条件

历史或当前 graphJson 中 TOOL 节点只保存：

```json
{
  "userConfig": {
    "selectedTool": {
      "fullName": "mcp__3__web_search_exa"
    }
  }
}
```

但后端执行器只读取：

```text
mcpToolName
```

这会导致 MCP 工具名解析为 `null`，节点还没真正调用 MCP 服务就失败。

## 排查顺序

1. 从 `ai-agent-interfaces/logs/log_info.log` 定位 executionId 和失败节点。
2. 查找是否存在 `Executing MCP tool: null`。
3. 查询 `workflow_node_execution_log` 中失败节点的 `error_message`。
4. 查询 `agent_info.graph_json` 中对应 TOOL 节点的 `userConfig`。
5. 检查 `userConfig` 是否含：
   - `mcpToolName`
   - `selectedTool.fullName`
6. 如果 `selectedTool.fullName` 存在而 `mcpToolName` 缺失，属于本事故签名。

## 正确契约

- 标准字段：`userConfig.mcpToolName`
- 兼容字段：`userConfig.selectedTool.fullName`
- 解析优先级：`mcpToolName > selectedTool.fullName`
- fullName 格式：`mcp__{serverId}__{toolName}`

## 修复要求

后端：

- `ToolNodeExecutorStrategy` 必须兼容 `mcpToolName` 和 `selectedTool.fullName`。
- 缺少工具配置时返回明确业务错误。
- 成功输出至少包含 `tool_response`。
- 可保留 `result` 字段兼容旧引用。

前端：

- `NodeConfigTabs` 选择工具时同步保存：

  ```text
  selectedTool
  mcpToolName = tool.fullName
  ```

- 清空工具时同步清空：

  ```text
  selectedTool = null
  mcpToolName = null
  ```

## 验证命令

```bash
mvn -pl ai-agent-infrastructure -am -Dtest=ToolNodeExecutorStrategyTest -Dsurefire.failIfNoSpecifiedTests=false test

cd ai-agent-foward
npm test -- --run src/modules/workflow/components/__tests__/NodeConfigTabs.test.tsx
npm run typecheck
```

## 已知非根因

- MCP 服务 `CONNECTED` 但工具名为 null 时，不要先怀疑 MCP 传输层；先查 graphJson 字段契约。
- `ResponseBodyEmitter has already completed` 是失败后的 SSE 关闭/清理噪声，不是 TOOL 节点调用失败根因。
- LLM 配置测试的 DNS、404、connection reset 日志与本事故无直接关系，除非失败链路实际进入 LLM 节点。

