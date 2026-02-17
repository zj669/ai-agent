## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisSsePublisher
- 将 `SseEventPayload` 序列化并发布到 Redis 频道 `workflow:channel:{executionId}`。
- 作为执行引擎与 SSE 网关之间的消息总线发送端。

## 2) 核心方法
- `publish(SseEventPayload payload)`

## 3) 具体方法
### 3.1 publish(...)
- 函数签名: `public void publish(SseEventPayload payload)`
- 入参: SSE 事件载荷
- 出参: 无
- 功能含义: 组装频道名、JSON 序列化并调用 `redisService.publish`。
- 链路作用: 节点执行事件进入 Redis Pub/Sub 的统一出口。

## 4) 变更记录
- 2026-02-15: 回填 Redis SSE 发布器蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
