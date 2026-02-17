## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSseListener.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSseListener.java`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: RedisSseListener
- Redis 消息监听器，负责将频道消息反序列化为 `SseEventPayload` 并回调消费。
- 处理 Redisson JsonCodec 双层序列化兼容（带外层引号字符串场景）。

## 2) 核心方法
- `onMessage(Message message, byte[] pattern)`

## 3) 具体方法
### 3.1 onMessage(...)
- 函数签名: `public void onMessage(Message message, byte[] pattern)`
- 入参: Redis 消息、匹配 pattern
- 出参: 无
- 功能含义: UTF-8 解码、可选去壳反序列化并调用 `eventHandler.accept(payload)`。
- 链路作用: Redis Pub/Sub 到 SSE emitter 桥接链路的接收端。

## 4) 变更记录
- 2026-02-15: 回填 Redis SSE 监听器蓝图语义。
- 2026-02-14: 初始化镜像蓝图（自动补缺）。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
