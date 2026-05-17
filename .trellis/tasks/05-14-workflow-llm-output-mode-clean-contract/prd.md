# LLM 输出模式干净化：严格文本/JSON 二选一

## 背景

当前工作流 LLM 节点已经在前端提供 `文本输出` / `JSON 输出` 两种模式，但后端执行结果仍会在所有模式下写入 `llm_output`、`response`、`text` 等历史兼容字段。JSON 模式下还会额外写入 `json_output` 和 JSON 顶层字段。

这会导致两个问题：

1. 用户理解上不是严格二选一，后续节点和最终回复提取逻辑容易混用字段。
2. 历史兼容字段会长期堆积，使报错边界不清晰，后续维护成本上升。

本任务目标是清理 LLM 输出契约，不继续兼容旧字段。

## 目标

1. LLM 节点输出模式只允许 `text` 或 `json`。
2. `text` 模式只产出并暴露 `response`。
3. `json` 模式只产出并暴露 `json_output`，以及用户显式配置的 JSON 顶层字段。
4. JSON 顶层字段必须由用户先填写字段名和字段描述，后端按这些字段生成 JSON 格式要求发给模型。
5. 移除后端 `llm_output`、`text` 等历史别名输出。
6. 移除调度器、流式发布、最终回复提取等路径对 `llm_output/text` 的 LLM 兼容兜底。
7. JSON 模式解析失败时必须显式失败，不静默退回文本输出。

## 非目标

1. 不迁移旧工作流数据。
2. 不保留旧字段兼容。
3. 不做多套输出契约共存。
4. 不引入模型厂商原生 JSON schema 能力；本任务只清理当前工作流契约。

## 契约

### 文本模式

配置：

```json
{
  "llmOutputMode": "text"
}
```

运行输出：

```json
{
  "response": "模型文本响应"
}
```

前端可引用字段：

```text
{{llm_node.output.response}}
```

### JSON 模式

配置：

```json
{
  "llmOutputMode": "json"
}
```

运行输出：

```json
{
  "json_output": {
    "title": "示例标题",
    "content": "示例内容"
  },
  "title": "示例标题",
  "content": "示例内容"
}
```

前端可引用字段：

```text
{{llm_node.output.json_output}}
{{llm_node.output.title}}
{{llm_node.output.content}}
```

说明：`title/content` 这类顶层字段只在用户显式配置 JSON 输出字段时暴露；如果未配置，则只暴露 `json_output`。

## 实现范围

1. 前端 LLM 节点输出配置
   - 保持 `文本输出` / `JSON 输出` 二选一。
   - 确认引用候选只来自当前模式下的 outputSchema。
   - 保存归一化不得注入 `llm_output/text`。

2. 后端 LLM 执行策略
   - `text` 模式只返回 `response`。
   - `json` 模式根据节点 outputSchema 中的用户字段生成字段说明和 JSON 示例，并注入 system prompt。
   - `json` 模式解析成功后只返回 `json_output` 和显式 JSON 顶层字段。
   - `json` 模式配置了显式字段时，模型返回缺字段必须失败。
   - 删除 `llm_output/text` 输出别名。
   - JSON 解析失败直接返回节点失败结果。

3. 最终回复提取与流式发布
   - LLM 文本模式读取 `response`。
   - LLM JSON 模式读取 `json_output`，需要展示时序列化为 JSON 字符串。
   - 不再对 LLM 输出字段使用 `text/llm_output` 兜底。

4. 测试
   - 覆盖文本模式只产出 `response`。
   - 覆盖 JSON 模式只产出 `json_output` 和显式 JSON 字段。
   - 覆盖 JSON 非法时节点执行失败。
   - 覆盖前端 outputSchema 和引用候选不包含旧字段。

## 验收标准

1. 新建 LLM 节点默认 `llmOutputMode=text`，输出字段只有 `response`。
2. 切换到 JSON 输出后，输出字段只有 `json_output` 和用户显式添加的 JSON 顶层字段。
3. JSON 顶层字段编辑区必须支持字段名、类型、字段描述。
4. 后端 LLM JSON 模式 system prompt 必须包含用户定义字段名、字段类型、字段描述和 JSON 示例结构。
5. 后端 LLM 文本模式执行结果不包含 `llm_output`、`text`。
6. 后端 LLM JSON 模式执行结果不包含 `response`、`llm_output`、`text`。
7. 后端 LLM JSON 模式遇到非法 JSON 或缺少显式字段时节点失败，错误信息能定位为 JSON 输出解析失败。
8. 下游节点引用候选不会出现 `llm_output`、`text` 这类旧字段。
9. 所有相关前后端测试通过。

## 相关文件

- `ai-agent-foward/src/modules/workflow/components/NodeConfigTabs.tsx`
- `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx`
- `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`
- `ai-agent-foward/src/modules/workflow/components/__tests__/NodeConfigTabs.test.tsx`
- `ai-agent-foward/src/modules/workflow/__tests__/workflow.editor-interaction.test.tsx`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java`
- `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java`
- `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java`
