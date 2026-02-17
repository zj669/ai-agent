# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/constants/RedisKeyConstants.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/constants/RedisKeyConstants.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/constants/RedisKeyConstants.java
- Type: .java

## Responsibility
- 集中管理 Redis Key 命名常量与 TTL 常量，统一跨模块键规范。
- 仅定义常量，不负责 Redis 访问行为。

## Key Symbols / Structure
- `Email`：验证码前缀。
- `RateLimit`：IP/邮箱/登录失败/设备限流前缀。
- `User`：Token 黑名单与 RefreshToken 前缀。
- `Idempotent`：幂等锁前缀。
- `HumanIntervention`：人工介入状态前缀与 TTL。
- `WorkflowCheckpoint`：检查点前缀与 TTL。

## Dependencies
- 仅 JDK 常量类型。

## Notes
- 所有内部类均私有构造，防止实例化。
