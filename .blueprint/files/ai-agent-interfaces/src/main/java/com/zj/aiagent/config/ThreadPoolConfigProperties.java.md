## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/ThreadPoolConfigProperties.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: ThreadPoolConfigProperties.java
- 绑定 `thread.pool.executor.config` 下的线程池参数，为 `ThreadPoolConfig` 提供可配置运行时参数。

## 2) 核心方法
- `getCorePoolSize()/getMaxPoolSize()/getPolicy()`（Lombok 访问器）

## 3) 具体方法
### 3.1 配置属性绑定
- 函数签名: `@ConfigurationProperties(prefix = "thread.pool.executor.config")`
- 入参: 外部配置文件参数
- 出参: `ThreadPoolConfigProperties` 实例
- 功能含义: 提供线程池默认值（20/200/10/5000/AbortPolicy）并支持外部覆盖。
- 链路作用: 配置中心 -> 属性对象 -> `ThreadPoolConfig` Bean 构建。

## 4) 变更记录
- 2026-02-15: 基于源码回填线程池参数对象职责与默认值语义。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
