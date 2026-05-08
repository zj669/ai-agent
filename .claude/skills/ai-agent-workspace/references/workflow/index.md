# 工作流引擎业务域索引

本文件为工作流引擎业务域提供二级路由，不重复根级全局安全规则。

## 业务范围

工作流执行引擎：Execution 聚合根、WorkflowGraph、ExecutionContext、节点执行策略、
条件分支、人工审核、SSE 流式推送。

## 典型触发

- 用户提到"执行工作流"、"workflow execution"、"节点"、"条件分支"、"SSE"
- 排查工作流卡住、节点失败、条件路由不正确
- 需要添加新节点类型或修改执行逻辑

## 核心执行流

```text
WorkflowController → SchedulerService.startExecution
  → Execution.start() → 返回 ready 节点
  → hydrateMemory (VectorStore LTM + ConversationRepository STM)
  → NodeExecutorStrategy → StreamPublisher → RedisSsePublisher → SSE → 前端
  → Execution.advance(nodeId, result) → 更新状态/条件分支 → 返回下一批 ready 节点
  → onExecutionComplete → 提取最终响应 → ChatApplicationService.completeAssistantMessage
```

## START 节点输入契约

- START 节点输出必须以用户启动输入为最高优先级。
- Chat UI 启动 workflow 的用户输入字段必须是 `inputs.inputMessage`；如果真实 UI 仍发送 `inputs.query`，下游引用 `start.output.inputMessage` 会按正确规则失败。
- 合并顺序固定为：先写入 `inputSchema` 解析出的默认值，再写入 `ExecutionContext.inputs` 中的用户显式输入。
- 不允许 `inputSchema.defaultValue=""` 覆盖用户启动输入；否则下游引用 `start.output.inputMessage` 的 TOOL/KNOWLEDGE/LLM 节点会拿到空串。
- 当看到 MCP 工具请求体为空、`query` 为空或节点日志中 START 输出 `inputMessage=""`，优先检查 `StartNodeExecutorStrategy` 的输入合并顺序。

## 值引用标准

- 前端 UI、图 JSON、后端解析器必须共用同一套引用格式：
  - `inputs.<key>`：引用启动输入。
  - `<nodeId>.output.<key>`：引用上游节点输出。
  - `sharedState.<key>`：引用共享状态。
- 新保存的 `sourceRef` 必须使用上述标准格式；不要再新增 `nodes.<nodeId>.<key>` 或 SpEL `#{...}` 作为节点入参契约。
- 历史格式只允许兼容读取：
  - `nodes.<nodeId>.<key>` 归一为 `<nodeId>.output.<key>`。
  - `state.<key>` 归一为 `sharedState.<key>`。
  - `#{...}` 仅为旧图兼容，不作为新图生成格式。
- 引用不存在、路径不存在、必填参数解析为空都必须显式失败；不要回退到原始表达式、空串、任意用户输入或上下文中的近似字段。
- TOOL 节点在外部 MCP 调用前必须校验 JSON Schema `required` 字段；必填为空时失败为 `TOOL 入参解析失败`，不能把空参数发给 MCP。
- KNOWLEDGE 节点只读取解析后的 `query`，不允许从 `user_input`、第一个非系统字符串或 `context.inputs.*` 兜底补值。
- 条件分支 EXPRESSION 模式的引用也使用同一标准，前端变量选择值保存为 `<nodeId>.output.<key>`。

## 执行状态机

- Execution: `PENDING → RUNNING → PAUSED/PAUSED_FOR_REVIEW → SUCCEEDED/FAILED/CANCELLED`
- Node: `PENDING → RUNNING → SUCCEEDED/FAILED/SKIPPED`
- 调度器提交节点执行前必须先把 accepted ready 节点从 `PENDING` 标记为 `RUNNING`；后续并行节点完成时，`RUNNING` 节点不能再次进入 ready 列表。
- 人工审核 `BEFORE_EXECUTION` 恢复时，paused node 必须先回到 `PENDING`，再由调度器标记为 `RUNNING`；否则会出现 `Accepted 0/1 pending nodes` 且节点不继续执行。
- 人工审核 `AFTER_EXECUTION` 恢复不应重复执行已完成节点，应继续推进后继节点。

## 节点类型

| 节点类型 | 执行策略 | 备注 |
|---|---|---|
| START | StartNodeExecutorStrategy | 工作流起始 |
| END | EndNodeExecutorStrategy | 工作流结束 |
| LLM | LlmNodeExecutorStrategy | LLM 调用 |
| CONDITION | ConditionNodeExecutorStrategy | EXPRESSION/LLM 两种模式 |
| HTTP | HttpNodeExecutorStrategy | HTTP 请求，支持 JSON 提取 |
| TOOL | ToolNodeExecutorStrategy | MCP 工具调用 |

## TOOL 节点契约

- 前端工具选择信息位于 `userConfig.selectedTool`，其中 `selectedTool.fullName` 格式为 `mcp__{serverId}__{toolName}`。
- 后端执行器标准读取字段为 `mcpToolName`；为兼容历史图，执行器也必须支持 `selectedTool.fullName`。
- 工具名解析优先级固定为：`mcpToolName > selectedTool.fullName`。
- 前端在选择工具时必须同步保存 `mcpToolName = tool.fullName`，清空工具时必须同步清空 `selectedTool` 和 `mcpToolName`。
- TOOL 节点成功输出字段至少包含 `tool_response`；可以继续保留 `result` 作为旧引用兼容字段。
- 当看到日志 `Executing MCP tool: null` 或节点错误 `mcpToolName not configured` / `MCP 工具未配置`，先检查 graphJson 中 TOOL 节点 `userConfig` 是否有 `mcpToolName` 或 `selectedTool.fullName`。

## LLM 节点契约

- 构造模型历史消息链时过滤空内容消息；workflow 启动时初始化的 assistant 占位消息不能进入 OpenAI 兼容请求。
- 流式调用结束但聚合响应为空时，允许用同一 prompt 做非流式回退；如果回退仍为空，节点必须失败为 `LLM 返回空响应`。
- 不要把空响应伪装成成功答案；UI 显示完成但回答为空属于真实缺陷。

## 条件分支规则

- EXPRESSION 模式：SpEL 表达式求值
- LLM 模式：语义理解选分支
- 条件节点完成后调用 `Execution.pruneUnselectedBranches`，未选中分支标记为 SKIPPED
- 收敛节点（多前驱）：只有所有前驱都 SKIPPED 才跳过

## 代码入口（支撑事实）

- 接口层：`ai-agent-interfaces/src/main/java/.../workflow/WorkflowController.java`（路径 `/api/workflow/execution`）
- 应用层：`ai-agent-application/src/main/java/.../workflow/SchedulerService.java`
- 领域层：`ai-agent-domain/src/main/java/.../workflow/`
- 基础设施执行器：`ai-agent-infrastructure/src/main/java/.../workflow/executor/`
- 人工审核：`HumanReviewController.java`（路径 `/api/workflow/reviews`）

## 验证提示

- 动态 UI、真实登录态、SSE、MCP 实际调用链路优先使用 `browser-relay` 走真实浏览器验证。
- 值引用修复必须做两类 Browser Relay 验证：
  - 正向：传入图中实际引用的字段，例如 `inputs.inputMessage`，确认 `start.output.inputMessage` 能解析为 TOOL 的非空 `query`，且同一 execution 只有一条 TOOL 节点日志。
  - 反向：故意不传被引用字段，例如只传 `inputs.query`，确认 TOOL/KNOWLEDGE 在外部调用前失败，并且日志没有真正的 `Executing MCP tool`。
- Browser Relay 触发工作流时使用短 `conversationId`，例如 `brt1`；过长 ID 可能先撞上 `messages.conversation_id` 字段长度限制，污染故障判断。
- 排查 Chat UI 链路时必须让 Browser Relay 操作真实页面：选择 agent、输入消息、发送、处理人工审核弹窗，并确认 UI 最终显示 `已完成` 和答案内容。只在页面上下文里直接 fetch 后端接口不能证明真实 UI payload 正确。
- Browser Relay 主动取消 SSE reader 时服务端可能记录 `Broken pipe`，这通常是验证端断流噪声，不等价于节点执行失败。
- SSE emitter 已关闭后再次投递可能记录投递失败 WARN；这类日志要和真正的节点失败区分，确认 execution 最终状态和 UI 是否完成。
- 本地后端若使用 `mvn spring-boot:run -pl ai-agent-interfaces ...` 启动，修改 interfaces 以外模块后必须先执行：

  ```bash
  mvn -pl ai-agent-interfaces -am -DskipTests install
  ```

  否则运行进程会继续加载本地 Maven 仓库里的旧 SNAPSHOT jar。

- 定向跑 infrastructure 模块测试时，优先使用：

  ```bash
  mvn -pl ai-agent-infrastructure -am -Dtest=ToolNodeExecutorStrategyTest -Dsurefire.failIfNoSpecifiedTests=false test
  ```

- `-am` 用于避免使用本地 Maven 仓库里的旧 SNAPSHOT 依赖。
- `-Dsurefire.failIfNoSpecifiedTests=false` 用于避免 reactor 上游模块没有同名测试时误失败。

## 安全边界

- 修改 Execution 状态机转换逻辑前需要确认，避免死锁或状态不一致
- WorkflowGraph 不可变，不要尝试运行时修改
- 人工审核（PAUSED_FOR_REVIEW）需通过 `resumeExecution` API 继续，不要直接修改状态
- Checkpoint 存储在 Redis，最终结果持久化到 MySQL

## SOP 列表

| SOP | 文件 | 状态 |
|---|---|---|
| 工作流功能开发 | `references/workflow/feature-delivery.md` | 待创建 |
| 工作流执行排查 | `references/workflow/triage.md` | 待创建 |
| 新增节点类型 | `references/workflow/add-node-type.md` | 待创建 |
