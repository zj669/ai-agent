# Blueprint Mirror: ai-agent-application/src/main/java/com/zj/aiagent/application/dashboard/service/DashboardApplicationService.java

## Metadata
- file: `ai-agent-application/src/main/java/com/zj/aiagent/application/dashboard/service/DashboardApplicationService.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-application/src/main/java/com/zj/aiagent/application/dashboard/service/DashboardApplicationService.java
- Type: .java

## Responsibility
- 编排 Dashboard 统计查询，提供“缓存优先 + 仓储回源”能力。

## Key Symbols / Structure
- `getStats(Long userId)`
  - 从 Redisson `RBucket` 读取缓存
  - 缓存命中则 JSON 反序列化返回
  - 缓存失效或损坏时回源 `DashboardRepository`
  - 将结果缓存 5 分钟
- `clearStatsCache(Long userId)`
  - 主动删除用户统计缓存

## Dependencies
- Domain: `DashboardRepository`, `DashboardStats`
- DTO: `DashboardStatsResponse`
- Infra: `RedissonClient`, Jackson `ObjectMapper`

## Notes
- 状态: 正常
- 缓存键前缀 `dashboard:stats:`，TTL `5` 分钟。
