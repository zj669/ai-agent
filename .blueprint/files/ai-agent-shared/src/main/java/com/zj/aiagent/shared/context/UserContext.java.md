# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/context/UserContext.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/context/UserContext.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/context/UserContext.java
- Type: .java

## Responsibility
- 提供线程内用户 ID 上下文存取能力，支持请求链路用户信息透传。
- 不负责鉴权逻辑，仅存取当前线程上下文。

## Key Symbols / Structure
- `setUserId(Long userId)`：写入用户 ID。
- `getUserId()`：读取用户 ID。
- `clear()`：清理 ThreadLocal。

## Dependencies
- JDK `ThreadLocal<Long>`。

## Notes
- 在线程池复用场景必须调用 `clear` 防止脏数据泄漏。
