# I-015 chat-ui-start-input-contract

## 故障签名

- 直接用后端接口传 `inputs.inputMessage` 可以跑通，但真实聊天 UI 仍报 `start.output.inputMessage` 为空。
- 日志中 START 节点执行成功，但 KNOWLEDGE/TOOL 的 `query` 解析为必填空值。
- Browser Relay 捕获真实 UI 请求体时可看到 `inputs.query`，而不是图中标准字段 `inputs.inputMessage`。

## 根因

工作流值引用标准迁移到了 `inputMessage`，但 Chat UI 启动 workflow 的 payload 仍沿用旧字段 `query`。这会导致 START 输出没有下游正在引用的 `inputMessage`，即使后端解析器本身是正确的也会失败。

## 修复原则

- Chat UI 启动 workflow 必须发送 `inputs.inputMessage`。
- 后端保存用户消息、记忆检索等读用户输入的逻辑优先读取 `inputMessage`。
- 不要为了让错误不报而从任意字段兜底；如果图引用 `start.output.inputMessage`，真实 UI payload 就必须提供 `inputMessage`。
- 验证必须用 Browser Relay 操作真实页面，不只用 curl 或直接 fetch 后端接口。

## 验证记录

- Browser Relay 真实 UI payload：

  ```json
  {"agentId":1,"conversationId":"6d7f5332-a9a6-4512-a6b5-46669b07ebb0","inputs":{"inputMessage":"周杰是谁？"},"mode":"STANDARD","userId":1}
  ```

- 成功 executionId：`8445e5fe-d9b5-496e-a584-22316e284fb5`。
- UI 结果：`已完成 5/5 步`，最终回答长度 225。
- 重启后复验 executionId：`799b5fcd-e053-4fca-8676-330bcfd45d5c`，真实 UI payload 仍为 `inputs.inputMessage`，UI 结果 `已完成 5/5 步`，最终回答长度 290。
