# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/PageResult.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/PageResult.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/PageResult.java
- Type: .java

## Responsibility
- 定义分页结果模型，统一承载总数、总页数及分页数据列表。
- 仅作为数据结构，不实现分页计算与查询逻辑。

## Key Symbols / Structure
- `total`: 总记录数。
- `pages`: 总页数。
- `list`: 当前页数据列表。

## Dependencies
- Lombok `@Data/@NoArgsConstructor/@AllArgsConstructor`。
- JDK `List<T>`。

## Notes
- 常作为 `Response<PageResult<T>>` 的 data 部分。
