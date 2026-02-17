## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/AuthStrategy.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AuthStrategy.java
- 认证策略抽象接口，定义统一认证契约，供 JWT 与 Debug 两类策略实现。

## 2) 核心方法
- `authenticate(String token, Object... extraArgs)`

## 3) 具体方法
### 3.1 authenticate(String token, Object... extraArgs)
- 函数签名: `authenticate(String token, Object... extraArgs) -> boolean`
- 入参: 认证载荷（token 或 debug-user）及扩展参数
- 出参: 认证是否通过
- 功能含义: 统一认证策略接口，隔离具体鉴权机制差异。
- 链路作用: `LoginInterceptor` 通过工厂选择策略后调用该契约。

## 4) 变更记录
- 2026-02-15: 基于源码回填认证策略接口契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
