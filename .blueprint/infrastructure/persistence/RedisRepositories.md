# Redis Repositories Blueprint

## 职责契约
- **做什么**: 实现需要高性能临时存储的 Repository 接口——执行检查点、SSE 流发布、速率限制计数器、缓存
- **不做什么**: 不负责持久化数据（最终数据归 MySQL）；不包含业务逻辑

## 实现清单

| 端口接口 (Domain) | 实现类 (Infrastructure) | 用途 |
|-------------------|------------------------|------|
| CheckpointRepository | RedisCheckpointRepository | 工作流执行检查点（暂停/恢复） |
| StreamPublisher | RedisSseStreamPublisher | SSE 流式输出中转 |
| WorkflowCancellationPort | (Redis实现) | 工作流取消信号 |
| RateLimiter | (Redis实现) | 速率限制滑动窗口 |

## 依赖拓扑
- **上游**: SchedulerService, AuthService
- **下游**: Redisson 客户端, Redis 服务器

## 设计约束
- 使用项目封装的 RedisService，禁止直接使用 RedisTemplate
- 所有 Redis key 必须设置过期时间
- 检查点数据序列化为 JSON
- SSE 流通过 Redis Pub/Sub 实现跨实例通信

## 变更日志
- [初始] 从现有代码逆向生成蓝图
