# Backend Infrastructure Layer Spec

## Goal

Produce two spec files documenting the **infrastructure layer**:
1. `.trellis/spec/backend/infrastructure-layer.md` (new)
2. Rewrite `.trellis/spec/backend/database-guidelines.md` (currently 51-line template)

Both use real code from `ai-agent-infrastructure/`.

## Project Context

The infrastructure layer is the **technical adapter** zone. It implements domain ports, wraps third-party SDKs, and handles persistence. Adapters here MAY use any framework; the **dependency rule** is: domain defines the port (interface), infrastructure provides the adapter (`*Impl`, `*Adapter`).

**Stack to document** (verify presence):
- **Database**: MySQL 8.x, accessed via **MyBatis Plus** (NOT JPA). Schema lives in `docker/init/mysql/01_init_schema.sql` (single file, NO Flyway).
- **Cache & pub/sub**: Redis via Redisson — encapsulated as `RedisService` (use this, NEVER raw `RedisTemplate`).
- **Vector store**: Milvus via Milvus SDK (protobuf 3.25.3 pinned in parent POM)
- **Object storage**: MinIO, host port `9002 → container 9000`
- **AI**: Spring AI auto-config DISABLED. Models are user-configured at runtime via `ChatModelPort` → `OpenAiChatModelAdapter`.

**Adapter examples**:
- `ExecutionRepository` (domain port) → `ExecutionRepositoryImpl` (Redis + MySQL)
- `VectorStore` (domain port) → `MilvusVectorStoreAdapter`
- `StreamPublisher` (domain port) → `RedisSsePublisher` (Redis Pub/Sub)
- `ChatModelPort` (domain port) → `OpenAiChatModelAdapter`

**Persistence patterns**:
- PO (Persistence Object): `AgentPO`, `UserPO`, `WorkflowNodeExecution`
- Mapper: `AgentMapper` interface + `AgentMapper.xml` mapping
- Snake_case table names: `agent_info`, `user_account`, `workflow_node_execution`
- Logical delete: `deleted` column (0=active, 1=deleted)
- Timestamps: `create_time`, `update_time`

## Files You Own

Exclusively yours:
- `.trellis/spec/backend/infrastructure-layer.md` **(create new)**
- `.trellis/spec/backend/database-guidelines.md` **(rewrite — currently template)**

### infrastructure-layer.md sections
1. **Purpose & Boundaries** — adapter pattern, dependency on domain ports
2. **Repository Implementations** — `*RepositoryImpl` pattern (Redis + MySQL hybrid)
3. **External Service Adapters** — Milvus, MinIO, OpenAI integration
4. **Stream Publishers** — SSE via Redis Pub/Sub
5. **Redis Usage** — MUST use `RedisService`, NOT raw `RedisTemplate` (cite CLAUDE.md)
6. **Spring AI Configuration** — auto-config disabled, models are dynamic
7. **Anti-patterns**

### database-guidelines.md sections
1. **ORM Choice** — MyBatis Plus, why not JPA
2. **Schema Management** — single SQL file at `docker/init/mysql/01_init_schema.sql`, **NO Flyway**, first-start only
3. **Table Naming** — snake_case, examples
4. **PO Class Pattern** — `*PO.java` + Mapper interface + XML
5. **Logical Delete** — `deleted` column convention
6. **Timestamp Columns** — `create_time` / `update_time`
7. **Query Patterns** — when to use Mapper XML vs MyBatis Plus wrappers
8. **Anti-patterns** — direct JDBC, duplicate tables (e.g., creating `WorkflowExecution` when `WorkflowNodeExecution` exists)

## Tools Available

### GitNexus MCP
- `gitnexus_cypher({query: "MATCH (c:Class) WHERE c.file CONTAINS 'infrastructure' AND c.name ENDS WITH 'Adapter' RETURN c.name, c.file"})`
- `gitnexus_context({name: "MilvusVectorStoreAdapter"})` for full adapter surface
- `gitnexus_impact({target: "RedisService", direction: "upstream"})` to confirm usage spread

### ABCoder MCP
Java parse skipped. Use GitNexus + direct file reads.

### Workflow
1. Enumerate adapters and repository impls
2. Read `docker/init/mysql/01_init_schema.sql` for real DDL examples
3. Pick `ExecutionRepositoryImpl` or `MilvusVectorStoreAdapter` as showcase
4. Cross-check `RedisService` is the canonical wrapper

## Rules

- ONLY modify the two files listed above
- DO NOT modify source code or other spec files
- DO NOT run git commands

## Acceptance Criteria

- [ ] Both files exist, each 100+ lines
- [ ] At least 1 real adapter walkthrough in `infrastructure-layer.md` (file path + code)
- [ ] At least 1 real DDL example in `database-guidelines.md` (from schema file)
- [ ] `RedisService` canonical usage documented
- [ ] No-Flyway constraint clearly stated
- [ ] No placeholder text
- [ ] Only the 2 listed files modified

## Technical Notes

- Infrastructure path: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/`
- DB schema: `docker/init/mysql/01_init_schema.sql`
- Docker services: MySQL 13306, Redis 6379, Milvus 19530, MinIO 9002 (host)
- Protobuf 3.25.3 (Milvus SDK constraint, managed in parent POM)
