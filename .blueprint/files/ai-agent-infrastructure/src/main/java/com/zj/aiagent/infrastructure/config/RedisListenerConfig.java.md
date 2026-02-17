## Metadata
- file: `.blueprint/files/ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/RedisListenerConfig.java.md`
- source: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/RedisListenerConfig.java`
- version: `1.1`
- status: 正常
- updated_at: 2026-02-15
- owner: backend-blueprint-shadow

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: Redis 消息监听容器配置
- 源文件: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/RedisListenerConfig.java`
- 文件类型: `.java`
- 说明:
  - 注册 `RedisMessageListenerContainer` Bean。
  - 将 Spring Data Redis 的 `RedisConnectionFactory` 注入监听容器，作为 Redis Pub/Sub 事件消费基础设施。
  - 为 `RedisSseListener` 等监听器提供运行容器承载。

## 2) 核心方法
- `redisMessageListenerContainer(...)`：创建并配置 Redis 消息监听容器。

## 3) 具体方法
### 3.1 `redisMessageListenerContainer(...)`
- 函数签名: `public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory)`
- 入参:
  - `connectionFactory` - Redis 连接工厂
- 出参:
  - `RedisMessageListenerContainer` - Redis 订阅监听容器
- 功能含义:
  - 新建监听容器并绑定连接工厂，交由 Spring 托管生命周期。
- 链路作用:
  - 上游: Spring Redis 自动配置提供连接工厂
  - 下游: 工作流流式事件、SSE 频道转发等 Redis 监听逻辑

## 4) 变更记录
- 2026-02-14: 初始化镜像蓝图，自动创建缺失模板。
- 2026-02-15: 回填 RedisListenerConfig 真实职责与方法语义，清理“待补充”占位内容。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
