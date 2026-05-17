# 熟悉工作流拖拽画布上游节点引用填充机制

## Background

工作流编辑器支持用户在拖拽画布中连接 START、LLM、KNOWLEDGE、HTTP、TOOL、CONDITION、END 等节点。下游节点需要读取上游节点输出，例如：

- KNOWLEDGE 节点的 `query` 引用 START 节点输入。
- TOOL 节点的工具参数引用 START、LLM、HTTP 或 KNOWLEDGE 输出。
- LLM 节点的 Prompt 模板引用祖先节点输出。
- END 节点引用上游节点输出作为最终结果。

本任务目标不是立即重构，而是建立一份可继续维护的任务上下文：明确拖拽页面如何生成可选上游变量、如何保存引用、后端如何转换并解析、执行结果如何写回给后续节点引用。

## Scope

### Frontend

- `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx`
- `ai-agent-foward/src/modules/workflow/components/VariableRefSelector.tsx`
- `ai-agent-foward/src/modules/workflow/components/NodeConfigTabs.tsx`
- `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`
- `ai-agent-foward/src/modules/workflow/validation/validateWorkflowGraph.ts`

### Backend

- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/graph/WorkflowGraphFactoryImpl.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/expression/ExpressionResolver.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/template/PromptTemplateResolver.java`
- `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Node.java`
- `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/*NodeExecutorStrategy.java`

## User Feedback On 2026-05-14

需要继续优化当前体验：

1. TOOL、HTTP 等节点需要引用 LLM 节点输出，但当前普通输入字段经常看不到 LLM 输出可选项。
2. LLM 节点输出缺少清晰的输出模式选择，应能选择普通文本输出或 JSON 输出。
3. 前端引用交互不能依赖丑陋的“点击引用变量”按钮，应支持在输入框中输入 `{{` 后弹出引用候选，选择后自动补全，例如 `{{start.output.inputMessage}}`。

## Current Understanding

### Standard Reference Syntax

拖拽页面当前只生成祖先节点输出引用：

```text
{ancestorNodeId}.output.{key}
```

后端仍保留历史图兼容解析，但新增图和前端补全不展示、不生成旧语法。START 节点历史 `start.output.query` 会在前端归一为 `start.output.inputMessage`。

### Frontend Flow

1. 用户在画布中连线，边数据使用 `source -> target` 表达依赖。
2. 展开某个节点时，`WorkflowNode` 根据入边递归找到当前节点的所有祖先节点。
3. `WorkflowNode` 读取这些祖先节点的 `outputSchema`，生成 `UpstreamVariable` 和 Prompt 模板候选。
4. 引用路径格式为 `{upstreamNodeId}.output.{fieldKey}`。
5. `VariableRefSelector` 展示可引用字段，选中后把引用字符串回传给节点配置。
6. `NodeConfigTabs` 把选中的引用写入当前节点 `inputSchema[n].sourceRef`。
7. 如果字段没有 `sourceRef`，用户可以填写 `defaultValue`。
8. 保存图时，`WorkflowEditorPage.buildGraphPayload` 把节点的 `inputSchema`、`outputSchema`、`userConfig` 一起写入 `graphJson`。

### Backend Flow

1. `WorkflowGraphFactoryImpl.fromJson` 读取前端保存的 `graphJson`。
2. `extractInputs` 从每个节点的 `inputSchema` 提取输入映射：
   - 有 `sourceRef`：`inputs[field.key] = normalizeSourceRef(sourceRef)`。
   - 没有 `sourceRef` 但有 `defaultValue`：`inputs[field.key] = defaultValue`。
3. Scheduler 执行节点前调用 `expressionResolver.resolveInputs(node.getInputs(), context)`。
4. `ExpressionResolver` 按标准引用语法从 `ExecutionContext.inputs`、`ExecutionContext.nodeOutputs` 或 `sharedState` 中取值。
5. 解析成功后传给节点执行器的就是 `resolvedInputs`。
6. 节点执行成功后，`SchedulerService` 把 `NodeExecutionResult.outputs` 写入 `ExecutionContext.nodeOutputs[nodeId]`，供后续节点继续引用。

### Important Node Outputs

- START：透传启动输入和 START 节点 resolved inputs，例如 `inputMessage`。
- KNOWLEDGE：输出 `knowledge_list`。
- TOOL：输出 `tool_response` 和 `result`。
- HTTP：输出 `response`、`body`、`statusCode`。
- LLM：输出 `llm_output`、`response`、`text`。
- END：透传 resolved inputs 并追加 `__workflow_ended__`。

## Known Boundaries And Risks

1. 普通输入字段的上游引用选择器基于当前节点的所有祖先节点 `outputSchema` 生成选项，多跳链路中的 TOOL/HTTP 可直接引用 LLM 输出。
2. LLM Prompt 模板变量列表只遍历当前节点的所有祖先节点，不再额外提供 START 输入候选。
3. 普通输入字段使用裸引用字符串，例如 `start.output.inputMessage`；LLM Prompt 模板使用 Mustache，例如 `{{start.output.inputMessage}}`。
4. `ExpressionResolver` 对普通输入引用缺失会抛错并让节点失败。
5. `PromptTemplateResolver` 对 Prompt 中找不到的占位符当前会保留原占位符并记录 warn，不会像普通输入一样直接失败。
6. `StructuredConditionEvaluator` 对条件表达式中不存在的引用当前返回 `null` 并继续比较，语义不同于普通输入解析。
7. 前端图保存校验目前主要检查连线合法性，没有全面校验 `sourceRef` 指向的节点和字段是否真实存在。

## Acceptance Criteria

- [ ] 梳理清楚拖拽画布中直接上游变量列表如何生成。
- [ ] 梳理清楚 `sourceRef` 在前端节点配置中的保存位置。
- [ ] 梳理清楚 `graphJson` 保存结构中 `inputSchema`、`outputSchema`、`userConfig` 的边界。
- [ ] 梳理清楚后端如何把 `inputSchema.sourceRef` 转成 `Node.inputs`。
- [ ] 梳理清楚执行时 `resolvedInputs` 如何从 `ExecutionContext` 中解析。
- [ ] 梳理各节点常见输出 key，方便后续配置上游引用。
- [ ] 标记当前引用链路的行为差异和潜在风险。
- [ ] 普通输入字段支持输入 `{{` 触发引用候选并自动补全。
- [ ] TOOL、HTTP 等下游节点能在普通输入字段里选择祖先 LLM 输出。
- [ ] LLM 节点输出 schema 默认有标准文本输出字段，并支持 JSON 输出模式配置。
- [ ] 前端引用补全和默认 Prompt 只暴露祖先节点输出引用，不再出现额外输入入口。

## Investigation Notes

一个典型配置片段：

```json
{
  "id": "tool-1",
  "nodeId": "tool-1",
  "type": "TOOL",
  "inputSchema": [
    {
      "key": "title",
      "label": "文章标题",
      "type": "string",
      "required": true,
      "sourceRef": "start.output.inputMessage"
    }
  ],
  "outputSchema": [
    {
      "key": "tool_response",
      "label": "工具响应",
      "type": "object",
      "system": true
    }
  ],
  "userConfig": {
    "mcpToolName": "mcp__4__send_article"
  }
}
```

后端转换后：

```text
Node.inputs["title"] = "start.output.inputMessage"
```

执行前解析后：

```text
resolvedInputs["title"] = ExecutionContext.nodeOutputs["start"]["inputMessage"]
```
