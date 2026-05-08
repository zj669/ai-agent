# I-011 workflow-start-input-default

## 故障签名

- TOOL/KNOWLEDGE/LLM 下游节点引用 `start.output.inputMessage` 后拿到空串。
- MCP 工具请求体中 `query` 为空，可能返回 400。
- START 节点输出类似：

```json
{"query":"周杰是谁？","inputMessage":""}
```

## 根因

`StartNodeExecutorStrategy` 合并输出时，用户启动输入先写入，随后 `inputSchema` 解析出的默认值又写入。同名字段存在 `defaultValue=""` 时，默认空串覆盖了用户输入。

## 修复规则

- START 输出合并顺序必须固定为：
  1. 先写入 `resolvedInputs` 中的非系统字段，作为 schema 默认兜底。
  2. 再写入 `ExecutionContext.inputs` 中的用户显式启动输入。
- 用户启动输入优先级必须高于 `inputSchema.defaultValue`。

## 验证

- 使用 Browser Relay 走真实浏览器链路启动工作流。
- 验证 START 输出中 `query` 和 `inputMessage` 都保留用户输入。
- 验证 TOOL 节点成功返回真实 MCP 结果，而不是空 query 400。
