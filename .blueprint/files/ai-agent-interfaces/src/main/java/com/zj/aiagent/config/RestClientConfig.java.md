## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/RestClientConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RestClientConfig.java
- 封装 Apache HttpClient 5 与 Spring `RestClient.Builder`，提供默认、简化、SSL 三类构建器及可选监控 Bean。

## 2) 核心方法
- `restClientBuilder()`
- `simpleRestClientBuilder()`
- `sslRestClientBuilder()`
- `restClient(RestClient.Builder)`

## 3) 具体方法
### 3.1 restClientBuilder()
- 函数签名: `restClientBuilder() -> RestClient.Builder`
- 入参: 无
- 出参: `RestClient.Builder`
- 功能含义: 配置连接池、超时、重试、请求/响应日志拦截器和默认请求头，作为主 `@Primary` Builder。
- 链路作用: 上游 HTTP 节点/适配器 -> 注入 Builder -> 统一网络参数与观测能力。

## 4) 变更记录
- 2026-02-15: 基于源码回填 RestClient 多构建器与监控开关职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
