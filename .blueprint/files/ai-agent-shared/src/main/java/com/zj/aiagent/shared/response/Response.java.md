# Blueprint Mirror: ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java

## Metadata
- file: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java`
- version: `v0.1.0`
- status: 正常
- updated_at: 2026-02-15 07:49
- owner: blueprint-team


## Source File
- Path: ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java
- Type: .java

## Responsibility
- 定义统一 API 响应包装对象 `Response<T>`，封装 `code/message/data/success` 四元组。
- 提供成功/失败响应的静态工厂，避免业务层重复拼装返回结构。
- 不负责 HTTP 协议细节和异常转换，仅作为通用数据载体。

## Key Symbols / Structure
- `class Response<T>`: 通用响应模型。
- `static <T> success(T data)`: 构建成功响应。
- `static <T> success()`: 空数据成功响应。
- `static <T> error(int code, String message)`: 构建失败响应。

## Dependencies
- Lombok: `@Data/@Builder/@NoArgsConstructor/@AllArgsConstructor`。
- JDK 泛型。

## Notes
- 作为 shared 层基础模型，供 interfaces/application 返回对象复用。
