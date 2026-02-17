## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/DebugAuthStrategy.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: DebugAuthStrategy.java
- Debug 认证实现，仅在 `auth.debug.enabled=true` 时允许通过 Header 直接携带用户 ID 进行认证。

## 2) 核心方法
- `authenticate(String token, Object... extraArgs)`

## 3) 具体方法
### 3.1 authenticate(String token, Object... extraArgs)
- 函数签名: `authenticate(String token, Object... extraArgs) -> boolean`
- 入参: `token`（debug header 中用户 ID 字符串）
- 出参: 是否通过认证
- 功能含义: 校验开关开启、token 非空且可解析为 Long。
- 链路作用: `LoginInterceptor` Debug 分支 -> 策略验证 -> 设置 `UserContext`。

## 4) 变更记录
- 2026-02-15: 基于源码回填 Debug 认证策略校验规则。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
