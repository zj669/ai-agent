## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/config/HttpsConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: HttpsConfig.java
- 仅在 `prod` profile 生效的 HTTPS 强制配置，定义 HTTP(8080) 到 HTTPS(8443) 的重定向与保密约束。

## 2) 核心方法
- `servletContainer()`
- `createHttpConnector()`

## 3) 具体方法
### 3.1 servletContainer()
- 函数签名: `servletContainer() -> ServletWebServerFactory`
- 入参: 无
- 出参: Tomcat WebServerFactory
- 功能含义: 对 Context 注入 `CONFIDENTIAL` 约束，并附加 HTTP Connector 用于自动跳转 HTTPS。
- 链路作用: 生产环境启动 -> Tomcat 安全约束 -> 明文请求自动重定向。

## 4) 变更记录
- 2026-02-15: 基于源码回填生产 HTTPS 重定向与约束策略。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
