## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/advice/GlobalExceptionHandler.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: GlobalExceptionHandler.java
- 全局异常转换器，按异常类型统一映射 HTTP 状态与 `Response.error(code,message)`，并对异步超时场景做静默处理。

## 2) 核心方法
- `handleAsyncRequestTimeoutException(AsyncRequestTimeoutException e)`
- `handleAuthenticationException(AuthenticationException e)`
- `handleValidationException(MethodArgumentNotValidException e)`
- `handleException(Exception e)`

## 3) 具体方法
### 3.1 handleAuthenticationException(AuthenticationException e)
- 函数签名: `handleAuthenticationException(AuthenticationException e) -> Response<Void>`
- 入参: 认证领域异常
- 出参: 标准错误响应
- 功能含义: 根据 `ErrorCode` 映射为 401/429/400 并记录警告日志。
- 链路作用: 认证失败异常 -> 接口层统一错误码 -> 前端可预测错误处理。

## 4) 变更记录
- 2026-02-15: 基于源码回填全局异常映射策略与异步超时处理语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
