# Database Guidelines

> Database patterns and conventions for the AI Agent Platform.

---

## Overview

- **Database**: MySQL 8.0 (Docker port `13306`)
- **ORM**: MyBatis Plus 3.5.5 (via `mybatis-plus-boot-starter`)
- **Schema Management**: Manual SQL file, NO Flyway/Liquibase
- **Schema File**: `ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql`
- **Vector DB**: Milvus 19530 (for knowledge base vector search)
- **Cache**: Redis 6379 (via Redisson 3.45.1)

---

## Schema Management

### Schema File Location

```
ai-agent-infrastructure/src/main/resources/docker/init/mysql/01_init_schema.sql
```

This file runs automatically on first Docker container startup. **Always check and update this file before creating or modifying tables.**

### ⚠️ NO Flyway / NO Liquibase

Schema changes are managed manually:

1. **Modify `01_init_schema.sql`** for the canonical schema
2. **Write ALTER statements** for existing environments if needed
3. **NEVER assume automatic migration** — coordinate with team

---

## Table Naming Conventions

| Convention | Rule | Example |
|------------|------|---------|
| Table names | `snake_case` | `agent_info`, `user_info`, `workflow_node_execution_log` |
| Column names | `snake_case` | `user_id`, `graph_json`, `published_version_id` |
| Primary key | `id` (bigint, auto-increment) | `id bigint(20) NOT NULL AUTO_INCREMENT` |
| Foreign key pattern | `{related_table}_id` | `agent_id`, `user_id` |
| Logical delete | `deleted` (int, 0=active, 1=deleted) | `deleted int(11) DEFAULT 0` |
| Create time | `create_time` or `created_at` | `create_time datetime DEFAULT CURRENT_TIMESTAMP` |
| Update time | `update_time` or `updated_at` | `update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` |
| Status fields | Integer codes with comments | `status int(11) DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-已下线'` |
| Index naming | `idx_{column}` or `uk_{column}` | `KEY idx_user_id (user_id)`, `UNIQUE KEY uk_email (email)` |
| Charset | `utf8mb4` | `DEFAULT CHARSET=utf8mb4` |
| Engine | `InnoDB` | `ENGINE=InnoDB` |

### ⚠️ Timestamp Inconsistency

The codebase has two timestamp conventions (legacy). For **new tables**, use:
- `create_time` / `update_time` (preferred, matches majority of tables)

---

## ORM Patterns (MyBatis Plus)

### PO (Persistence Object) Pattern

PO classes live in `infrastructure/{module}/po/` and map directly to database tables:

```java
// ai-agent-infrastructure/.../agent/po/AgentPO.java
@Data
@TableName("agent_info")
public class AgentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String description;
    // MyBatis Plus auto-fills
    @Version
    private Integer version;        // Optimistic locking
    @TableLogic
    private Integer deleted;        // Logical delete
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

### Mapper Interface Pattern

Mappers extend `BaseMapper<PO>` and live in `infrastructure/{module}/mapper/`:

```java
// ai-agent-infrastructure/.../agent/mapper/AgentMapper.java
@Mapper
public interface AgentMapper extends BaseMapper<AgentPO> {
    // Custom queries using XML or annotations
    List<AgentPO> selectSummaryByUserId(@Param("userId") Long userId);
}
```

### Repository Implementation Pattern

Repositories in infrastructure implement domain interfaces and handle PO ↔ Domain conversion:

```java
// ai-agent-infrastructure/.../agent/repository/AgentRepositoryImpl.java
@Slf4j
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {
    private final AgentMapper agentMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Agent agent) {
        AgentPO po = toPO(agent);
        if (po.getId() == null) {
            agentMapper.insert(po);
            agent.setId(po.getId());  // Write back generated ID
        } else {
            int rows = agentMapper.updateById(po);
            if (rows == 0) {
                throw new OptimisticLockingFailureException("Update failed (Optimistic Lock)");
            }
        }
    }
    
    // PO → Domain conversion
    private Agent toDomain(AgentPO po) { ... }
    // Domain → PO conversion
    private AgentPO toPO(Agent agent) { ... }
}
```

---

## Query Patterns

### Simple Queries (MyBatis Plus Lambda)

```java
// Use LambdaQueryWrapper for type-safe queries
LambdaQueryWrapper<AgentPO> wrapper = new LambdaQueryWrapper<AgentPO>()
    .eq(AgentPO::getUserId, userId)
    .eq(AgentPO::getDeleted, 0)
    .orderByDesc(AgentPO::getUpdateTime);
List<AgentPO> results = agentMapper.selectList(wrapper);
```

### Complex Queries (XML Mapper)

For complex joins or projections, use XML mapper files:
- Location: `ai-agent-infrastructure/src/main/resources/mapper/{Module}Mapper.xml`

### Batch Operations

Use MyBatis Plus `saveBatch()` or `insertBatchSomeColumn()` for bulk inserts.

---

## Transaction Management

- Use `@Transactional(rollbackFor = Exception.class)` at the **application or repository layer**
- **NEVER put `@Transactional` on domain entities** — they must be framework-free
- For cross-aggregate operations, use application-layer orchestration

```java
@Service
@RequiredArgsConstructor
public class AgentApplicationService {
    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(CreateAgentCmd cmd) {
        // Orchestrate domain operations
        Agent agent = Agent.builder()...build();
        agentRepository.save(agent);
        return agent.getId();
    }
}
```

---

## Optimistic Locking

- Use `@Version` annotation on PO's `version` field
- MyBatis Plus auto-handles version increment on update
- Catch `OptimisticLockingFailureException` for concurrent modification

---

## Data Conversion Flow

```
Controller ← DTO/Response ← ApplicationService ← Domain Entity ← Repository ← PO → Database
```

| Layer | Object Type | Example |
|-------|-------------|---------|
| API | DTO / Response | `AgentDetailResult`, `Response<T>` |
| Application | Command / DTO | `CreateAgentCmd` |
| Domain | Entity / Value Object | `Agent`, `AgentStatus` |
| Infrastructure | PO | `AgentPO` |
| Database | SQL Row | `agent_info` table |

---

## Common Mistakes

1. **❌ Creating duplicate tables** — Always check `01_init_schema.sql` first
2. **❌ Using `RedisTemplate` directly** — Use the project's `RedisService` wrapper
3. **❌ Adding MyBatis/JPA annotations to domain entities** — Domain must be pure
4. **❌ Forgetting `rollbackFor = Exception.class`** — Spring only rolls back unchecked exceptions by default
5. **❌ Not writing back generated IDs** — After `insert()`, sync `po.getId()` back to domain entity
6. **❌ Assuming Flyway is available** — Schema is manual, not auto-migrated
