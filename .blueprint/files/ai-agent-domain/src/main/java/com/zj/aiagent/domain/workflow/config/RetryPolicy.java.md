# Blueprint Mirror: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/RetryPolicy.java

## Metadata
- file: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/RetryPolicy.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/RetryPolicy.java
- Type: .java
- Status: 正常

## Responsibility
- 节点重试策略配置对象，定义重试次数、间隔与退避参数。

## Key Symbols / Structure
- 字段：`maxRetries`, `retryDelayMs`, `exponentialBackoff`, `backoffMultiplier`, `maxRetryDelayMs`。

## Dependencies
- 基础数值类型

## Notes
- 通过默认值提供通用重试策略基线。
