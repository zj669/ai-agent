## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisherFactory.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisherFactory.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisSseStreamPublisherFactory
- 实现 `StreamPublisherFactory`，按 `StreamContext` 创建 `RedisSseStreamPublisher` 实例。
- 将领域端口与基础设施发布实现解耦。

## 2) 核心方法
- `create(StreamContext context)`

## 3) 具体方法
### 3.1 create(...)
- 函数签名: `public StreamPublisher create(StreamContext context)`
- 入参: 流上下文
- 出参: StreamPublisher
- 功能含义: 组装 publisher 依赖并返回新实例。
- 链路作用: 调度流程进入节点执行前构造流发布能力。

## 4) 变更记录
- 2026-02-15: 回填 StreamPublisher 工厂蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
