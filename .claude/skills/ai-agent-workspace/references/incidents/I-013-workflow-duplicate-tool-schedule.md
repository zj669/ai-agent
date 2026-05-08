# I-013 workflow-duplicate-tool-schedule

## 故障签名

- 单次 workflow execution 中，同一个 TOOL 节点出现两条成功执行日志。
- 后端日志出现两次 `Tool executed successfully`。
- 数据库 `workflow_node_logs` 中同一 execution 的同一 TOOL 节点存在重复成功记录。

## 根因

这不是 TOOL 工具名 null、MCP 配置缺失或 START 输入空串导致的失败。根因是调度器提交 ready 节点后，节点状态仍保持 `PENDING`。当另一个并行节点先完成并再次触发 `getReadyNodes()` 时，正在运行的 TOOL 仍会被判定为 ready，从而重复调度并重复外部调用。

## 修复

- `Execution.markRunning(Collection<Node> nodes)`：只接受状态仍为 `PENDING` 的 ready 节点，并把 accepted 节点标记为 `RUNNING`。
- `SchedulerService.scheduleNodes(...)`：提交异步执行前先调用 `markNodesRunning(...)`，后续只调度 accepted 节点。
- 如果 accepted 列表为空，调度器直接返回，不再提交异步执行。

## 验证记录

- Browser Relay 正向执行：executionId=`56cdbc83-4b5d-43e9-bcd7-df295fdd3bff`。
- 后端日志只有一次真正的 `Executing MCP tool: mcp__3__web_search_exa`。
- 数据库 `workflow_node_execution_log` 中该 execution 的 `tool-1778151979576-3` 只有一条记录。
- TOOL 节点输入 `query="周杰是谁？"`，输出 keys 为 `["result","tool_response"]`。

## 临时边界

涉及有副作用工具时，重复调度会造成重复外部调用、重复计费或重复副作用。修复后仍需要在真实 Browser Relay 链路里用数据库节点日志计数确认，而不是只看 SSE 文本，因为 SSE 正则解析容易跨事件误报。
