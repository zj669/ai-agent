## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: LoginInterceptor.java
- 统一认证拦截器：优先 Debug 认证，其次 JWT 认证；成功后写入 `UserContext`，请求结束清理 ThreadLocal。

## 2) 核心方法
- `preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)`
- `afterCompletion(...)`

## 3) 具体方法
### 3.1 preHandle(...)
- 函数签名: `preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) -> boolean`
- 入参: 请求上下文
- 出参: 是否放行
- 功能含义: 放行 OPTIONS；按 DEBUG/JWT 两阶段认证并设置用户上下文；失败返回 401。
- 链路作用: 所有受保护接口入站统一认证关口。

## 4) 变更记录
- 2026-02-15: 基于源码回填认证拦截链与上下文清理职责。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
