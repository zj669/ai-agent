# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/factory/DefaultMultiThreadStrategyFactory.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/factory/DefaultMultiThreadStrategyFactory.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/design/ruletree/factory/DefaultMultiThreadStrategyFactory.java
- Type: .java

## Responsibility
- 定义多线程规则树工厂基类，统一暴露规则树根节点。
- 不负责路由规则本身，仅约束装配入口。

## Key Symbols / Structure
- `getRootNode()`：返回 `AbstractMultiThreadStrategyRouter` 根节点。

## Dependencies
- `AbstractMultiThreadStrategyRouter`。
- Lombok `@Data`。

## Notes
- 供业务模块注入/组合完整多线程规则树。
