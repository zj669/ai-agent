## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WorkflowConfig.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WorkflowConfig.java`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: 工作流执行线程池配置
- 源文件: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WorkflowConfig.java`
- 文件类型: `.java`
- 说明:
  - 提供节点执行专用线程池 Bean（`nodeExecutorThreadPool`）。
  - 从配置项读取核心参数（core/max/queue），隔离工作流节点执行与公共线程池。
  - 使用 `CallerRunsPolicy` 与优雅停机参数，保障高压场景与服务关闭过程稳定。

## 2) 核心方法
- `nodeExecutorThreadPool()`：构建 `ThreadPoolTaskExecutor` 并以 `wf-exec-` 前缀暴露给节点执行链路。

## 3) 具体方法
### 3.1 `nodeExecutorThreadPool()`
- 函数签名: `public Executor nodeExecutorThreadPool()`
- 入参:
  - 无（依赖字段注入：`corePoolSize/maxPoolSize/queueCapacity`）
- 出参:
  - `Executor` - 实际返回 `ThreadPoolTaskExecutor`
- 功能含义:
  - 初始化线程池容量、拒绝策略、关闭等待时长并注册为 Spring Bean。
- 链路作用:
  - 上游: 工作流基础设施装配阶段
  - 下游: 各类 `NodeExecutorStrategy` 异步执行任务提交

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 WorkflowConfig 真实职责与方法语义，清理“待补充”占位内容。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
