# Infrastructure Layer

> Technical adapter conventions for `ai-agent-infrastructure`.

## Purpose And Boundaries

The infrastructure module is the adapter layer for this project. It turns
domain ports into concrete framework, SDK, database, cache, storage, and stream
implementations.

The dependency rule is:

1. Domain owns business interfaces and ports.
2. Infrastructure implements those ports.
3. Infrastructure may use Spring, MyBatis Plus, Redisson, Milvus, MinIO, and
   Spring AI SDK classes.
4. Domain must not depend on infrastructure or framework APIs.
5. Application code should consume domain ports when a port exists.

`CLAUDE.md` states the project-level module split as:

```text
ai-agent-domain: core business logic, entities, value objects, services, ports
ai-agent-infrastructure: database, Redis, Milvus, MinIO, external adapters
```

Do not infer an adapter name from architecture prose alone. The current code
does not contain every historical name mentioned in project notes. For example,
the current workflow execution adapter is `RedisExecutionRepository`, not a
class named `ExecutionRepositoryImpl`.

## Adapter Inventory

GitNexus indexed the following infrastructure adapter and repository classes
under `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure`:

| Concern | Domain/API side | Infrastructure class |
| --- | --- | --- |
| Agent persistence | `AgentRepository` | `AgentRepositoryImpl` |
| Chat persistence | conversation repository | `MybatisConversationRepository` |
| Dashboard reads | dashboard repository | `DashboardRepositoryImpl` |
| Email | email service | `EmailServiceImpl` |
| Knowledge text splitting | `TextSplitterPort` | `RoutedTextSplitterAdapter` |
| Knowledge document reading | `DocumentReaderPort` | `SpringAIDocumentReaderAdapter` |
| Knowledge dataset persistence | `KnowledgeDatasetRepository` | `MySQLKnowledgeDatasetRepository` |
| Knowledge document persistence | `KnowledgeDocumentRepository` | `MySQLKnowledgeDocumentRepository` |
| LLM provider config persistence | `LlmProviderConfigRepository` | `LlmProviderConfigRepositoryImpl` |
| MCP registry | tool registry abstraction | `McpToolRegistryAdapter` |
| MCP tool callback | Spring AI tool callback bridge | `McpToolCallbackAdapter` |
| MCP server persistence | MCP repository | `McpServerRepositoryImpl` |
| Vector storage | `VectorStore` | `MilvusVectorStoreAdapter` |
| Vector storage disabled mode | `VectorStore` | `NoOpVectorStoreAdapter` |
| File storage | `FileStorageService` | `MinIOFileStorageService` |
| Swarm persistence | swarm repositories | `Swarm*RepositoryImpl` classes |
| Verification code cache | verification repository | `RedisVerificationCodeRepository` |
| User persistence | `UserRepository` | `UserRepositoryImpl` |
| Human review queue | `HumanReviewQueuePort` | `RedisHumanReviewQueueAdapter` |
| Workflow execution cache | `ExecutionRepository` | `RedisExecutionRepository` |
| Workflow checkpoint cache | checkpoint repository | `RedisCheckpointRepository` |
| Workflow node log persistence | `WorkflowNodeExecutionLogRepository` | `WorkflowNodeExecutionLogRepositoryImpl` |
| Stream publisher factory | `StreamPublisherFactory` | `RedisSseStreamPublisherFactory` |
| Stream publisher | `StreamPublisher` | `RedisSseStreamPublisher` |

Use this inventory before adding new adapters. If a domain port already exists,
extend the existing infrastructure implementation or create a sibling adapter
with an explicit reason.

## Repository Implementations

Repository adapters are the most common infrastructure implementation shape.
The normal pattern is:

1. Domain defines an interface under `ai-agent-domain/.../repository` or
   `ai-agent-domain/.../port`.
2. Infrastructure injects a Mapper, SDK client, cache wrapper, or ObjectMapper.
3. Infrastructure converts Domain <-> PO near the repository boundary.
4. Infrastructure annotations such as `@Repository`, `@Service`, `@Mapper`, and
   `@Transactional` stay outside the domain module.

Representative MySQL repositories:

```text
AgentRepository -> AgentRepositoryImpl
KnowledgeDocumentRepository -> MySQLKnowledgeDocumentRepository
LlmProviderConfigRepository -> LlmProviderConfigRepositoryImpl
WorkflowNodeExecutionLogRepository -> WorkflowNodeExecutionLogRepositoryImpl
```

Representative Redis repositories:

```text
ExecutionRepository -> RedisExecutionRepository
HumanReviewQueuePort -> RedisHumanReviewQueueAdapter
StreamPublisherFactory -> RedisSseStreamPublisherFactory
```

Workflow execution uses a split persistence model:

1. `RedisExecutionRepository` stores the hot `Execution` aggregate in Redis for
   active execution, pause, resume, and conversation execution history.
2. `WorkflowNodeExecutionLogRepositoryImpl` stores node logs in MySQL table
   `workflow_node_execution_log`.
3. `RedisCheckpointRepository` stores checkpoint data in Redis.
4. `RedisHumanReviewQueueAdapter` stores pending review execution ids in a
   Redis set.

Do not collapse these responsibilities into one new `WorkflowExecution`
adapter. Search for existing workflow execution and node log components first.

### Walkthrough: `ExecutionRepository` To `RedisExecutionRepository`

Domain port:

```text
ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/ExecutionRepository.java
```

Infrastructure adapter:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisExecutionRepository.java
```

Key implementation excerpt:

```java
@Repository
@RequiredArgsConstructor
public class RedisExecutionRepository implements ExecutionRepository {
    private static final String KEY_PREFIX = "workflow:execution:";
    private static final String VERSION_KEY_SUFFIX = ":_v";
    private static final long TTL_HOURS = 48;

    private final IRedisService redisService;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
}
```

This adapter writes the serialized `Execution` aggregate to
`workflow:execution:{executionId}` with a 48 hour TTL. It also maintains:

```text
workflow:execution:{executionId}:_v
workflow:conversation:{conversationId}:executions
```

`IRedisService` handles normal string and set operations. `RedissonClient` is
used only where the implementation needs Redisson primitives directly, such as
the version counter.

Important behavior:

1. `save` serializes the aggregate and initializes the version counter.
2. `findById` deserializes from Redis and returns `Optional.empty()` if absent.
3. `findByConversationId` reads the Redis set index, multi-gets execution keys,
   filters nulls, and sorts by `createdAt` descending.
4. `update` updates the version counter and rolls it back if serialization or
   Redis write fails.

Do not add a MySQL table or mapper for this hot execution aggregate unless the
domain contract changes. Node-level execution history already has the MySQL
repository `WorkflowNodeExecutionLogRepositoryImpl`.

### Walkthrough: MySQL Node Logs

Domain port:

```text
ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/WorkflowNodeExecutionLogRepository.java
```

Infrastructure adapter:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/persistence/WorkflowNodeExecutionLogRepositoryImpl.java
```

Mapper and PO:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/mapper/WorkflowNodeExecutionLogMapper.java
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/po/WorkflowNodeExecutionLogPO.java
```

This is the canonical pattern for MyBatis Plus persistence where simple
CRUD/query wrapper behavior is enough:

```java
LambdaQueryWrapper<WorkflowNodeExecutionLogPO> wrapper =
    new LambdaQueryWrapper<>();
wrapper.eq(WorkflowNodeExecutionLogPO::getExecutionId, executionId)
       .orderByAsc(WorkflowNodeExecutionLogPO::getEndTime);
return logMapper.selectList(wrapper).stream()
    .map(this::toDomain)
    .collect(Collectors.toList());
```

The PO uses `@TableName(value = "workflow_node_execution_log", autoResultMap =
true)` and `JacksonTypeHandler` for JSON input/output fields.

## External Service Adapters

### Milvus

The vector store port lives in:

```text
ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java
```

The enabled adapter is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java
```

The disabled fallback is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/NoOpVectorStoreAdapter.java
```

`MilvusVectorStoreAdapter` implements `VectorStore` and is active only when:

```java
@ConditionalOnProperty(
    prefix = "milvus",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
```

`NoOpVectorStoreAdapter` implements the same port when `milvus.enabled=false`
or the property is missing. It returns empty search results and logs warnings,
allowing the application to start without vector storage.

Milvus configuration is owned by:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/config/MilvusVectorStoreConfig.java
```

It creates:

```text
MilvusServiceClient
knowledgeVectorStore -> agent_knowledge_base
memoryVectorStore -> agent_chat_memory
```

The parent POM pins `protobuf.version` to `3.25.3` to avoid Milvus SDK protobuf
conflicts. Do not remove that version pin without testing Milvus startup and
vector read/write paths.

### MinIO

The file storage port lives in:

```text
ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/port/FileStorageService.java
```

The adapter is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/knowledge/MinIOFileStorageService.java
```

It implements upload, download, delete, and bucket creation using `MinioClient`.
The client bean is configured in:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/MinIOConfig.java
```

Local Docker maps MinIO API port `9002` on the host to `9000` in the container:

```yaml
ports:
  - "9002:9000"
  - "9001:9001"
```

Application local config should use `MINIO_ENDPOINT: http://localhost:9002`
for the Docker profile. The generic application default remains
`http://localhost:9000`.

### OpenAI And Runtime LLM Configuration

Spring AI OpenAI auto-configuration is disabled in:

```text
ai-agent-interfaces/src/main/resources/application.yml
```

Disabled auto-config classes include:

```text
OpenAiChatAutoConfiguration
OpenAiEmbeddingAutoConfiguration
OpenAiAudioSpeechAutoConfiguration
OpenAiAudioTranscriptionAutoConfiguration
OpenAiImageAutoConfiguration
OpenAiModerationAutoConfiguration
ChatClientAutoConfiguration
```

Models are user-configured at runtime. Current code does not contain a
`ChatModelPort` or `OpenAiChatModelAdapter` class. Instead, runtime OpenAI
models are built from `LlmProviderConfig` in these places:

```text
ai-agent-application/src/main/java/com/zj/aiagent/application/llm/LlmConfigService.java
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/swarm/llm/SwarmLlmCaller.java
```

`LlmNodeExecutorStrategy` prefers `llmConfigId`, loads
`LlmProviderConfigRepository.findById`, then builds `OpenAiApi`,
`OpenAiChatModel`, and `ChatClient`. It falls back to legacy node-local
`model/baseUrl/apiKey` style fields only for old workflow data.

Do not re-enable Spring AI auto-configuration to get a default global chat
model. That would bypass per-user/per-workflow runtime model selection.

## Stream Publishers

The domain stream contract is:

```text
ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java
ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisherFactory.java
```

The infrastructure factory is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisherFactory.java
```

The per-execution publisher is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java
```

The Redis Pub/Sub bridge is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java
```

Flow:

```text
NodeExecutorStrategy
-> StreamPublisher
-> RedisSseStreamPublisher
-> RedisSsePublisher
-> IRedisService.publish("workflow:channel:{executionId}", json)
-> SSE subscriber / controller layer
```

`RedisSseStreamPublisher` is responsible for translating node-level events into
`SseEventPayload`:

```text
START -> SseEventType.START
delta -> SseEventType.UPDATE
finish -> SseEventType.FINISH
error -> SseEventType.ERROR
custom data -> SseEventType.UPDATE with render mode
```

`RedisSsePublisher` is responsible only for channel name construction,
serialization, and Redis publish. Keep these responsibilities separate.

## Redis Usage

The canonical general-purpose Redis wrapper is:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/redis/IRedisService.java
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/redis/service/RedissonService.java
```

`RedissonService` is the `@Service("redissonService")` implementation backed by
`RedissonClient`.

`RedisConfig` owns the `RedissonClient` bean:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/redis/config/RedisConfig.java
```

Use `IRedisService` in new adapters for normal Redis operations:

```text
set/get string values
sets and membership
hash maps
lists
pub/sub
locks and Redisson primitives exposed by the wrapper
TTL/expire operations
```

`CLAUDE.md` explicitly says to search for existing service wrappers and never
use `RedisTemplate` or `RestTemplate` directly. In this codebase, GitNexus
impact for `IRedisService` reported:

```text
risk: MEDIUM
direct affected files: 8
notable direct users:
- SchedulerService.java
- RedisExecutionRepository.java
- RedisCheckpointRepository.java
- RedisSsePublisher.java
- RedisHumanReviewQueueAdapter.java
- RedisVerificationCodeRepository.java
- RedissonService.java
```

There are existing security/auth classes that use `StringRedisTemplate`. Treat
those as existing specialized or legacy exceptions. Do not copy that pattern
into new infrastructure adapters. If the auth implementation is touched, first
decide whether the operation should move behind `IRedisService`.

## Spring AI Configuration

The project keeps Spring AI libraries but disables automatic model beans. This
is intentional because user-level model configuration is persisted in
`llm_provider_config` and selected at runtime.

Rules:

1. Do not inject a global `ChatClient` for workflow or swarm execution.
2. Do not assume one application-wide OpenAI key.
3. Use `LlmProviderConfigRepository` when execution needs a saved model config.
4. Preserve `llmConfigId` as the primary workflow reference.
5. Keep legacy `model/baseUrl/apiKey` fallback only for compatibility with old
   node JSON.
6. Do not log API keys.
7. Do not include API keys in DTO responses.

## Anti-Patterns

Do not do any of the following:

1. Add framework annotations or MyBatis classes to `ai-agent-domain`.
2. Create a new adapter before searching for an existing domain port and
   infrastructure implementation.
3. Add raw `RedisTemplate` or `StringRedisTemplate` usage to new adapters.
4. Re-enable Spring AI OpenAI auto-configuration for workflow or swarm calls.
5. Create a duplicate execution persistence table when node logs already use
   `workflow_node_execution_log` and hot execution state already uses Redis.
6. Create a duplicate file storage service instead of using
   `FileStorageService` and `MinIOFileStorageService`.
7. Bypass `VectorStore` and call Milvus directly from application/domain code.
8. Put SDK client construction inside domain entities or value objects.
9. Add JPA repositories; persistence uses MyBatis Plus.
10. Add Flyway or Liquibase migration files; schema management currently uses
    the single Docker initialization SQL file.
11. Hard-code local Docker ports in Java code; keep them in config.
12. Log secrets such as LLM API keys, MinIO credentials, Redis passwords, or
    JWT secrets.

## When Adding A New Infrastructure Adapter

Before implementation:

1. Search for a domain port or repository interface first.
2. Search for existing sibling adapters in the same bounded context.
3. Check whether the integration is runtime-configured by user data.
4. Check whether the operation belongs in Redis, MySQL, MinIO, Milvus, or an
   external HTTP/SDK client.
5. Check `docker/init/mysql/01_init_schema.sql` before adding any table.
6. Check `application.yml` and local/prod profile files before adding new
   config keys.

Implementation checklist:

1. Keep the adapter in `ai-agent-infrastructure`.
2. Convert Domain <-> PO/SDK DTO at the adapter boundary.
3. Prefer constructor injection with `@RequiredArgsConstructor`.
4. Use MyBatis Plus `BaseMapper` and wrappers for simple persistence.
5. Use Mapper XML only when custom result maps, type handlers, or non-trivial
   SQL are needed.
6. Use `IRedisService` for Redis operations.
7. Use existing SDK client beans (`MinioClient`, `MilvusServiceClient`,
   `RedissonClient`) instead of constructing clients ad hoc.
8. Document any intentional exception near the code.
