## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: LlmNodeExecutorStrategy
- 实现 LLM 节点执行，动态创建 `ChatClient` 调用 OpenAI 兼容模型并支持流式 delta 推送。
- 融合 `ExecutionContext` 的 LTM/STM/Awareness，构建多消息链（System + 历史 + 当前输入）。

## 2) 核心方法
- `executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
- `buildSystemPrompt(NodeConfig config, ExecutionContext context, Map<String, Object> resolvedInputs)`
- `buildMessageChain(NodeConfig config, ExecutionContext context, Map<String, Object> resolvedInputs, String systemPrompt)`
- `buildUserPrompt(NodeConfig config, Map<String, Object> resolvedInputs)`
- `getSupportedType()` / `supportsStreaming()`

## 3) 具体方法
### 3.1 executeAsync(...)
- 函数签名: `CompletableFuture<NodeExecutionResult> executeAsync(...)`
- 入参: 节点配置（模型与 Prompt 相关）及解析输入
- 出参: `NodeExecutionResult.success|failed`
- 功能含义: 构建客户端、拼装 Prompt、消费流式输出并实时 publishDelta。
- 链路作用: 工作流 AI 推理主节点，直接驱动前端流式体验。

### 3.2 buildSystemPrompt(...)
- 函数签名: `private String buildSystemPrompt(...)`
- 入参: 节点配置、执行上下文、输入映射
- 出参: System Prompt 文本
- 功能含义: 注入系统人设、长期记忆、执行日志与引用节点输出。
- 链路作用: 提升推理一致性与链路感知能力。

### 3.3 buildMessageChain(...)
- 函数签名: `private List<Message> buildMessageChain(...)`
- 入参: 配置、上下文、输入、systemPrompt
- 出参: 消息链列表
- 功能含义: 组装 System/历史会话/当前用户请求。
- 链路作用: 将会话上下文映射为模型调用输入。

## 4) 变更记录
- 2026-02-15: 回填 LLM 执行器蓝图语义，补齐上下文注入与流式调用链路。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
