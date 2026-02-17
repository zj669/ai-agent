## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/HttpNodeExecutorStrategy.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/HttpNodeExecutorStrategy.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HttpNodeExecutorStrategy
- 实现 HTTP 节点执行：解析 URL/Method/Header/Body 模板并通过 `WebClient` 发起请求。
- 将响应文本封装为标准节点输出，异常时通过流式发布器上报错误。

## 2) 核心方法
- `executeAsync(Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher)`
- `getSupportedType()`
- `resolveTemplate(String template, Map<String, Object> resolvedInputs)`

## 3) 具体方法
### 3.1 executeAsync(...)
- 函数签名: `CompletableFuture<NodeExecutionResult> executeAsync(...)`
- 入参: 节点配置（url/method/headers/bodyTemplate/readTimeout）与解析输入
- 出参: `NodeExecutionResult.success|failed`
- 功能含义: 构建并发送 HTTP 请求，返回 `response/body/statusCode`。
- 链路作用: 将外部 API 能力接入工作流节点图。

### 3.2 resolveTemplate(...)
- 函数签名: `private String resolveTemplate(...)`
- 入参: 模板字符串、已解析输入
- 出参: 替换后的字符串
- 功能含义: 支持 `#{key}` 与 `{{key}}` 占位符替换，跳过内部变量 `__*`。
- 链路作用: 连接执行上下文与 HTTP 请求参数生成。

## 4) 变更记录
- 2026-02-15: 回填 HTTP 执行器蓝图语义，补齐模板解析与调用流程。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
