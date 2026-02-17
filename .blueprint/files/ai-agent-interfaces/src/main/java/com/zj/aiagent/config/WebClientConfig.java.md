## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/WebClientConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: WebClientConfig.java
- 创建 `@Primary` 的 `WebClient.Builder`，统一 Reactor Netty 超时、连接器与请求/响应日志过滤器配置。

## 2) 核心方法
- `webClientBuilder()`
- `logRequest()`
- `logResponse()`

## 3) 具体方法
### 3.1 webClientBuilder()
- 函数签名: `webClientBuilder() -> WebClient.Builder`
- 入参: 无
- 出参: `WebClient.Builder`
- 功能含义: 配置连接超时、读写超时与 wiretap，并挂载请求/响应日志过滤器。
- 链路作用: 需要响应式 HTTP 调用的组件 -> 注入 Builder -> 统一网络时延与日志可观测性。

## 4) 变更记录
- 2026-02-15: 基于源码回填 WebClient 连接参数与过滤器职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
