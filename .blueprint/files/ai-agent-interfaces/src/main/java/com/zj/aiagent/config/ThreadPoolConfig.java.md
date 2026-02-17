## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/ThreadPoolConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ThreadPoolConfig.java
- 根据外部配置创建全局 `ThreadPoolExecutor` Bean，并按策略名选择拒绝策略，供异步任务统一复用。

## 2) 核心方法
- `threadPoolExecutor(ThreadPoolConfigProperties properties)`

## 3) 具体方法
### 3.1 threadPoolExecutor(ThreadPoolConfigProperties properties)
- 函数签名: `threadPoolExecutor(ThreadPoolConfigProperties properties) -> ThreadPoolExecutor`
- 入参: 线程池配置属性
- 出参: `ThreadPoolExecutor`
- 功能含义: 按 core/max/queue/keepAlive 与 policy 动态创建线程池，未命中策略时回退 `AbortPolicy`。
- 链路作用: 异步执行入口 -> 注入线程池 -> 统一并发资源与拒绝行为。

## 4) 变更记录
- 2026-02-15: 基于源码回填线程池组装与策略分发语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
