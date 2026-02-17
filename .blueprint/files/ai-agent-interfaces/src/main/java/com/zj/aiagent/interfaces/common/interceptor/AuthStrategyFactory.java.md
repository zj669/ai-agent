## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/AuthStrategyFactory.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AuthStrategyFactory.java
- 认证策略工厂，集中维护策略名到实现实例的映射并提供按类型查询。

## 2) 核心方法
- `init()`
- `getStrategy(String type)`

## 3) 具体方法
### 3.1 getStrategy(String type)
- 函数签名: `getStrategy(String type) -> Optional<AuthStrategy>`
- 入参: 策略类型（`JWT`/`DEBUG`）
- 出参: 策略可选值
- 功能含义: 从工厂内部映射获取策略，避免调用方依赖具体实现类。
- 链路作用: `LoginInterceptor` -> 工厂 -> 鉴权策略实现。

## 4) 变更记录
- 2026-02-15: 基于源码回填认证策略注册与检索语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
