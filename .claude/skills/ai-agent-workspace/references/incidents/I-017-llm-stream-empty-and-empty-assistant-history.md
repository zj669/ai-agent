# I-017 llm-stream-empty-and-empty-assistant-history

## 故障签名

- 工作流执行成功进入 LLM 节点，但最终回答为空，日志 `Response received, length: 0`。
- 对同一 prompt 做非流式回退时，上游模型报 `Invalid assistant message: content or tool_calls must be set`。
- 聊天历史中包含 workflow 启动时初始化的空 assistant 占位消息。

## 根因

部分兼容 OpenAI 的 LLM 服务流式接口可能正常结束但不给有效 delta。非流式回退复用消息历史时，把空 assistant 占位消息传给模型，而 OpenAI 兼容接口要求 assistant message 至少有 `content` 或 `tool_calls`。

## 修复原则

- 流式调用结束但聚合响应为空时，用同一 prompt 做非流式回退；如果仍为空，节点应失败为 `LLM 返回空响应`。
- 构造模型历史消息链时过滤空内容消息，尤其是 workflow 启动时创建的 assistant 占位。
- 不能返回假成功或静默吞掉空响应；空响应必须被显式处理。

## 验证记录

- 修复后 Browser Relay 真实 UI executionId=`8445e5fe-d9b5-496e-a584-22316e284fb5`。
- 后端日志：`Executing with 3 messages`，说明空 assistant 占位没有进入模型请求。
- 后端日志：`Response received, length: 225`，最终 `Execution ... finished with status: SUCCEEDED`。
- 重启后复验 executionId=`799b5fcd-e053-4fca-8676-330bcfd45d5c`，后端日志 `Response received, length: 290`，最终状态 `SUCCEEDED`。
