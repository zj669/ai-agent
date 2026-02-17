# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/NodeConfig.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/NodeConfig.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/NodeConfig.java
- Type: .java
- Status: 正常

## Responsibility
- 节点通用配置对象，统一承载动态属性、人审配置、重试策略与超时设置。
- 提供类型安全的便捷读取方法，屏蔽配置属性弱类型细节。

## Key Symbols / Structure
- 字段：`properties`, `humanReviewConfig`, `retryPolicy`, `timeoutMs`。
- 方法：
  - `requiresHumanReview()`
  - 读取器：`getString/getInteger/getLong/getDouble/getBoolean/getList/getMap`
  - 写入/判断：`set(key,value)`, `has(key)`

## Dependencies
- `HumanReviewConfig`
- `RetryPolicy`
- Java 集合：`Map`, `List`

## Notes
- `properties` 支持数据库驱动的动态字段扩展。
