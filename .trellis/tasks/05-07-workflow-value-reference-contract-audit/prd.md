# 复查工作流前后端节点值引用契约与占位参数解析

## Background

工作流中下游节点需要引用上游节点输出值，例如 TOOL 节点入参引用 START/KNOWLEDGE/HTTP/TOOL 的输出字段。最近连续出现两类相关问题：

- TOOL 节点已能解析 MCP 工具名，但传给工具的占位参数仍可能为空。
- START 节点曾因 `inputSchema.defaultValue=""` 覆盖用户启动输入，导致下游引用 `start.output.inputMessage` 得到空串。

这说明问题范围不只在单个 TOOL 执行器，而是在“前端引用配置 → 图保存 → 图转换 → 输入解析 → 执行器入参 → 日志可观测”的完整链路。

## Core Principle

本任务禁止通过兜底逻辑掩盖错误。

- 如果引用路径错误，应在保存校验或执行解析阶段明确报错。
- 如果前端保存格式与后端解析契约不一致，应修正契约或保存结构。
- 如果后端解析器拿不到引用值，应暴露具体缺失位置，而不是静默改用空串、默认值、全局输入或其它猜测来源。
- 如果某节点必须等待上游输出，应修正调度/ready 判断，而不是在执行器里临时补值。

## Scope

### Frontend

- `ai-agent-foward/src/modules/workflow/components/VariableRefSelector.tsx`
- `ai-agent-foward/src/modules/workflow/components/NodeConfigTabs.tsx`
- `ai-agent-foward/src/modules/workflow/components/FieldRenderer.tsx`
- `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`
- `ai-agent-foward/src/modules/workflow/validation/validateWorkflowGraph.ts`

检查点：

- 前端生成的引用语法是否与后端解析器一致。
- 引用上游节点输出字段时，是否使用稳定的 nodeId/outputKey，而不是展示名或临时 label。
- TOOL 节点参数字段是否保存为后端输入解析器能识别的结构。
- 输出 schema 变更后，下游引用是否能同步看到正确字段，例如 `tool_response`。
- 保存图前是否能发现引用不存在、引用字段不存在、引用方向错误、引用循环等硬错误。

### Backend

- `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/expression/ExpressionResolver.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/template/PromptTemplateResolver.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/WorkflowGraphFactoryImpl.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/converter/NodeConfigConverter.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/*NodeExecutorStrategy.java`
- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/NodeConfig.java`
- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`
- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowGraph.java`

检查点：

- `resolvedInputs` 的生成位置、引用解析规则、错误传播规则。
- `{{nodeId.output.key}}`、`nodes.{nodeId}.{key}`、`inputs.{key}` 等多套语法是否存在割裂。
- 下游节点执行时，上游节点输出是否已经写入 `ExecutionContext`。
- 引用缺失时当前是否返回 `null`、空串或默认值；这类行为需要复查，不能作为 TOOL 入参场景的兜底。
- 执行日志是否记录解析前配置、解析后入参、缺失引用路径，便于定位问题。

## Non-Goals

- 不重构 MCP 连接池、MCP 传输协议或第三方工具服务。
- 不把 LLM 人工审核暂停视为失败。
- 不用单测替代真实链路验证；用户要求真实测试优先使用 Browser Relay。
- 不通过临时兜底逻辑让 TOOL 不报错。

## Requirements

### 1. 建立唯一引用契约

明确工作流节点引用值的标准语法和数据结构，至少覆盖：

- 全局启动输入引用。
- 上游节点输出引用。
- 嵌套对象/数组字段引用。
- START/KNOWLEDGE/TOOL/HTTP/LLM/END 节点输出字段。

若历史上存在多套语法，需要给出兼容边界和迁移策略，但新增图必须只使用标准契约。

### 2. 前端保存与后端解析一致

前端写入的 TOOL/LLM/HTTP/KNOWLEDGE 输入映射必须能被后端解析器直接识别。禁止前端保存 A 格式、后端只解析 B 格式。

验收时需要列出至少一个真实图 JSON 片段，说明：

- 前端保存的引用字段是什么。
- 后端解析器读取哪个字段。
- 解析后传给执行器的 `resolvedInputs` 是什么。

### 3. 引用缺失必须失败并暴露位置

如果下游节点引用的上游节点或字段不存在，应失败并给出明确错误，例如：

```text
引用解析失败：node=tool-xxx input=query ref={{start.output.inputMessage}} reason=上游输出字段不存在
```

禁止行为：

- 静默返回空串。
- 静默返回 `null` 后继续调用 TOOL。
- 静默改用 `inputs.query` 或其它全局字段。
- 在执行器里猜测“可能用户想用哪个字段”。

### 4. TOOL 入参不得为空调用外部工具

如果 TOOL schema 要求 `query`，且引用解析后为空，应在工作流输入解析阶段失败，而不是继续调用 MCP 并等第三方返回 400。

失败原因必须指向原始引用路径和目标输入字段。

### 5. 使用 Browser Relay 做真实链路验证

验收验证必须使用 Browser Relay 控制真实 Chrome 页面或页面上下文：

- 触发工作流执行。
- 观察 SSE 节点事件。
- 检查后端日志中 TOOL 的解析后入参。
- 确认错误场景不会静默兜底。

## Acceptance Criteria

- [ ] 已梳理前端引用选择器、保存图 JSON、后端图转换、输入解析、执行器入参的完整数据流。
- [ ] 已确认并记录当前标准引用语法；如存在多套语法，明确哪套废弃、哪套保留。
- [ ] TOOL 节点 `query` 这类必填入参如果引用为空，工作流在调用 MCP 前失败，并指出缺失引用路径。
- [ ] 正确引用上游 START 输出时，TOOL 收到真实用户输入，不为空。
- [ ] 错误引用不存在字段时，不兜底、不猜测、不继续外部调用。
- [ ] 前端保存的新图与后端解析器字段契约一致。
- [ ] Browser Relay 验证记录包含 executionId、SSE 关键事件、后端日志关键行。
- [ ] 项目 skill 中记录本次值引用契约和坑点。

## Suggested Investigation Plan

1. 定位当前所有引用语法与解析器：
   - 前端 `VariableRefSelector` / `NodeConfigTabs`
   - 后端 `ExpressionResolver` / `PromptTemplateResolver` / `SchedulerService`

2. 还原真实故障图：
   - 查询当前 agent 图 JSON。
   - 找出 TOOL 节点输入映射字段。
   - 找出引用的上游 nodeId/outputKey。

3. 追踪执行链路：
   - 图转换后 `NodeConfig` 中保留了什么。
   - `SchedulerService` 构造 `resolvedInputs` 时如何解析引用。
   - TOOL 执行器收到的 map 是否已经为空。

4. 修复契约或解析实现：
   - 优先修正真实错误位置。
   - 不在执行器里添加猜测式兜底。
   - 必填值缺失时，明确失败。

5. Browser Relay 验证：
   - 正确引用场景：TOOL 成功且入参非空。
   - 错误引用场景：调用 MCP 前失败，错误信息指向引用路径。

## Verification

必须以 Browser Relay 真实链路为主：

```text
Browser Relay → Chrome 页面上下文 → /api/workflow/execution/start → SSE → 后端日志 → 数据库节点日志
```

如需要补充自动化检查，只能作为辅助，不能替代 Browser Relay 验证。
