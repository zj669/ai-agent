# Database Guidelines

> Database and MyBatis Plus conventions for the backend.

## ORM Choice

The backend uses MySQL 8.x with MyBatis Plus. It does not use JPA,
Hibernate entity repositories, Flyway, or Liquibase.

Evidence:

```text
pom.xml
- mysql.version = 8.0.28
- mybatis-plus.version = 3.5.5

ai-agent-infrastructure/pom.xml
- mybatis-plus-spring-boot3-starter
- mysql-connector-java
```

Application configuration:

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

Use MyBatis Plus for persistence objects, mapper interfaces, lambda wrappers,
pagination, and built-in CRUD. Use Mapper XML only when the query needs custom
result mapping, custom type handlers, or non-trivial SQL that should not live
inside an annotation.

Do not add Spring Data JPA repositories. Do not add `@Entity` mappings. Do not
introduce Hibernate lazy-loading assumptions into this project.

## Schema Management

The current schema source of truth is:

```text
docker/init/mysql/01_init_schema.sql
```

Docker Compose mounts it into MySQL's first-start initialization directory:

```yaml
volumes:
  - ./init/mysql:/docker-entrypoint-initdb.d
```

This means:

1. The SQL file runs automatically only when the MySQL container initializes an
   empty data directory.
2. It is not a repeatable migration system.
3. Editing the file does not automatically migrate an existing local or
   production database.
4. There is currently no Flyway or Liquibase migration history table.
5. Production changes need an explicit DBA/deployment plan outside this file.

No-Flyway constraint:

```text
Do not add db/migration, Flyway, or Liquibase files unless the project first
adopts a migration tool as a separate architecture decision.
```

For development docs and AI tasks, always read
`docker/init/mysql/01_init_schema.sql` before proposing a new table. Many
historical notes use outdated example names. The SQL file is the current source
for table names.

## Real DDL Examples

The `agent_info` table is the clearest example of the current naming,
timestamp, logical delete, JSON, and optimistic-lock patterns:

```sql
CREATE TABLE IF NOT EXISTS `agent_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `icon` varchar(500) DEFAULT NULL,
  `graph_json` json DEFAULT NULL,
  `status` int(11) DEFAULT 0,
  `published_version_id` bigint(20) DEFAULT NULL,
  `version` int(11) DEFAULT 0,
  `deleted` int(11) DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

The workflow node log table shows execution-log persistence:

```sql
CREATE TABLE IF NOT EXISTS `workflow_node_execution_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `execution_id` varchar(36) NOT NULL,
  `node_id` varchar(50) NOT NULL,
  `node_name` varchar(100) DEFAULT NULL,
  `node_type` varchar(50) DEFAULT NULL,
  `render_mode` varchar(20) DEFAULT NULL,
  `status` int(11) DEFAULT 0,
  `inputs` json DEFAULT NULL,
  `outputs` json DEFAULT NULL,
  `error_message` text,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_execution_id` (`execution_id`),
  KEY `idx_execution_node` (`execution_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

The LLM configuration table shows user-scoped dynamic model settings:

```sql
CREATE TABLE IF NOT EXISTS `llm_provider_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `provider` varchar(50) NOT NULL,
  `base_url` varchar(500) NOT NULL,
  `api_key` varchar(500) DEFAULT NULL,
  `model` varchar(100) NOT NULL,
  `is_default` tinyint(1) DEFAULT 0,
  `status` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## Table Naming

Use lowercase `snake_case` table names.

Current table examples:

```text
user_info
email_log
agent_info
agent_version
conversations
messages
knowledge_dataset
knowledge_document
node_template
sys_config_field_def
node_template_config_mapping
workflow_execution
workflow_node_execution_log
workflow_human_review_record
swarm_workspace
swarm_workspace_agent
swarm_group
swarm_group_member
swarm_message
writing_session
writing_task
writing_result
writing_draft
llm_provider_config
mcp_server_config
```

Do not create new names from historical examples without checking the SQL file.
For example, current user data is in `user_info`, not `user_account`, and
current node execution logs are in `workflow_node_execution_log`, not a generic
`workflow_node_execution` table.

Index names use short, descriptive prefixes:

```text
idx_user_id
idx_status
idx_execution_id
idx_execution_node
idx_session_status
uk_agent_version
uk_task_uuid
```

## Column Naming

Column names are `snake_case`; Java fields are camelCase. MyBatis Plus maps
between them using `map-underscore-to-camel-case: true`.

Examples:

```text
user_id -> userId
published_version_id -> publishedVersionId
graph_json -> graphJson
create_time -> createTime
update_time -> updateTime
llm_config_id -> llmConfigId
```

Prefer explicit names over overloaded names:

```text
document_id
dataset_id
execution_id
node_id
workspace_id
session_id
swarm_agent_id
```

For JSON columns, use names that describe the payload:

```text
graph_json
input_data
output_data
inputs
outputs
constraints_json
input_payload_json
expected_output_schema_json
structured_payload_json
config_json
```

## PO Class Pattern

Persistence Objects live under:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/**/po
```

Mapper interfaces live under:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/**/mapper
```

Mapper XML lives under:

```text
ai-agent-infrastructure/src/main/resources/mapper/**
```

Repository implementations live under:

```text
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/**/repository
ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/**/persistence
```

Canonical PO example:

```java
@Data
@TableName(value = "agent_info", autoResultMap = true)
public class AgentPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String name;
    private String description;
    private String icon;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private JsonNode graphJson;

    @Version
    private Integer version;

    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Rules:

1. PO classes are infrastructure concerns, not domain entities.
2. PO classes should use `@TableName` when the table is not trivially inferred.
3. Use `@TableId(type = IdType.AUTO)` for auto-increment primary keys.
4. Use `@Version` for optimistic-lock version columns.
5. Use `@TableField(typeHandler = JacksonTypeHandler.class)` for JSON columns
   stored as `JsonNode`.
6. Repository implementations own Domain <-> PO conversion.
7. Do not leak PO types out through domain or application public contracts.

## Mapper Interface Pattern

Simple mapper interfaces extend `BaseMapper<PO>`:

```java
@Mapper
public interface WorkflowNodeExecutionLogMapper
    extends BaseMapper<WorkflowNodeExecutionLogPO> {
}
```

Custom methods are acceptable when MyBatis Plus wrappers cannot express the
query cleanly or when a selective projection is valuable:

```java
@Mapper
public interface AgentMapper extends BaseMapper<AgentPO> {
    @Select("""
        SELECT id, user_id, name, description, icon, status,
               published_version_id, version, deleted,
               create_time, update_time
        FROM agent_info
        WHERE user_id = #{userId} AND deleted = 0
    """)
    List<AgentPO> selectSummaryByUserId(@Param("userId") Long userId);
}
```

Keep annotation SQL short. If result mapping, JSON type handlers, joins, or
large updates are needed, use XML.

## Mapper XML Pattern

The project currently has XML mappers such as:

```text
ai-agent-infrastructure/src/main/resources/mapper/agent/AgentMapper.xml
ai-agent-infrastructure/src/main/resources/mapper/dashboard/DashboardMapper.xml
```

Use XML for custom result maps:

```xml
<resultMap id="AgentResultMap"
           type="com.zj.aiagent.infrastructure.agent.po.AgentPO">
    <id property="id" column="id"/>
    <result property="userId" column="user_id"/>
    <result property="publishedVersionId" column="published_version_id"/>
    <result property="createTime" column="create_time"/>
    <result property="updateTime" column="update_time"/>
    <result property="graphJson" column="graph_json"
            typeHandler="com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler"/>
</resultMap>
```

XML is also appropriate for custom updates that must stay close to the mapper
instead of being hand-built in service logic.

## Logical Delete

MyBatis Plus global config defines:

```yaml
logic-delete-field: deleted
logic-delete-value: 1
logic-not-delete-value: 0
```

Use `deleted` for new tables that need logical deletion.

Current examples:

```text
user_info.deleted
agent_info.deleted
mcp_server_config.deleted
```

Not every table has logical delete. Event/log/history tables often keep their
own status fields or are append-only:

```text
email_log
workflow_node_execution_log
workflow_human_review_record
messages
swarm_message
writing_result
```

Rules:

1. Do not invent `is_deleted` for new tables unless extending an existing table
   that already uses it.
2. Do not mix `deleted` and `is_deleted` in the same bounded context.
3. Query active rows explicitly when using custom SQL.
4. Let MyBatis Plus logic delete config handle standard CRUD where possible.
5. For physical deletes, verify the domain behavior requires hard deletion.

## Timestamp Columns

The project has two timestamp naming styles:

```text
create_time / update_time
created_at / updated_at
```

Core Agent and configuration-style tables commonly use:

```text
create_time
update_time
```

Many chat, knowledge, swarm, writing, and LLM tables use:

```text
created_at
updated_at
```

When modifying an existing table, preserve its style. When creating a table in
an existing bounded context, match sibling tables in that context.

Default DDL pattern:

```sql
`create_time` datetime DEFAULT CURRENT_TIMESTAMP,
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

or:

```sql
`created_at` datetime DEFAULT CURRENT_TIMESTAMP,
`updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

Java fields should use `LocalDateTime` in PO classes when the database column
is `datetime`.

## Query Patterns

Use `BaseMapper` CRUD for simple primary-key operations:

```java
AgentPO po = agentMapper.selectById(id);
agentMapper.insert(po);
agentMapper.updateById(po);
agentMapper.deleteById(id);
```

Use `LambdaQueryWrapper` for simple dynamic filters:

```java
LambdaQueryWrapper<WorkflowNodeExecutionLogPO> wrapper =
    new LambdaQueryWrapper<>();
wrapper.eq(WorkflowNodeExecutionLogPO::getExecutionId, executionId)
       .orderByAsc(WorkflowNodeExecutionLogPO::getStartTime);
return logMapper.selectList(wrapper);
```

Use `LambdaUpdateWrapper` for narrow update statements:

```java
LambdaUpdateWrapper<LlmProviderConfigPO> wrapper =
    new LambdaUpdateWrapper<>();
wrapper.eq(LlmProviderConfigPO::getUserId, userId)
       .set(LlmProviderConfigPO::getIsDefault, false);
mapper.update(null, wrapper);
```

Use MyBatis Plus pagination for page queries:

```java
Page<KnowledgeDocumentPO> page =
    new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
IPage<KnowledgeDocumentPO> poPage = mapper.selectPage(page, wrapper);
```

Use Mapper XML when:

1. You need custom result maps.
2. You need JSON type handlers in result mapping.
3. You need hand-written joins.
4. You need a large update statement.
5. The SQL is easier to review as SQL than as wrapper code.

Do not use direct JDBC for ordinary repository work. Do not build SQL strings
with concatenation from request values. Do not put query logic in controllers.

## Transaction Boundaries

Use `@Transactional` in application services or repository methods where a use
case writes multiple rows or multiple tables.

Example pattern:

```java
@Transactional(rollbackFor = Exception.class)
public void save(Agent agent) {
    AgentPO po = toPO(agent);
    if (po.getId() == null) {
        agentMapper.insert(po);
        agent.setId(po.getId());
    } else {
        int rows = agentMapper.updateById(po);
        if (rows == 0) {
            throw new OptimisticLockingFailureException(...);
        }
    }
}
```

Keep transaction scope at the smallest boundary that preserves consistency.
Do not wrap read-only conversion or external SDK calls in a database transaction
unless there is a concrete consistency requirement.

## Domain Conversion

Repository implementations convert between domain entities and PO classes.

Example conversion responsibilities:

```text
Agent.graphJson String <-> AgentPO.graphJson JsonNode
WorkflowNodeExecutionLog.inputs Map <-> WorkflowNodeExecutionLogPO.inputs JsonNode
KnowledgeDocument.chunkingConfig <-> chunk_config_json/chunk_size/chunk_overlap
LlmProviderConfig booleans/status <-> llm_provider_config columns
```

Rules:

1. Domain entities should not know MyBatis Plus annotations.
2. PO classes should not expose domain behavior.
3. Conversion errors should fail close to the repository boundary.
4. Preserve IDs generated by insert by writing them back to the domain object
   when the caller expects the new ID.

## Anti-Patterns

Do not do any of the following:

1. Add JPA, Hibernate, or Spring Data repository patterns.
2. Add Flyway or Liquibase migrations without a project-level migration
   decision.
3. Create duplicate tables before searching `01_init_schema.sql`.
4. Create `WorkflowExecution`/`workflow_execution` alternatives when existing
   execution state and node logs already exist.
5. Create a new user table named `user_account`; current user data is
   `user_info`.
6. Create a new workflow node table named `workflow_node_execution`; current
   node log persistence is `workflow_node_execution_log`.
7. Use direct JDBC in repositories when MyBatis Plus already covers the query.
8. Put SQL in controllers.
9. Return PO classes from application or interface-layer APIs.
10. Put business decisions inside Mapper XML.
11. Bypass optimistic-lock version checks on `agent_info` updates.
12. Store secrets in docs, logs, or DTO responses.
13. Assume editing `01_init_schema.sql` migrates an existing database.
14. Mix timestamp naming styles inside the same table family.
15. Add a table without adding the matching PO, Mapper, and repository adapter
    plan.

## Checklist Before Database Changes

Before proposing or implementing a database change:

1. Read `docker/init/mysql/01_init_schema.sql`.
2. Search existing PO classes under `ai-agent-infrastructure/**/po`.
3. Search existing Mapper interfaces under `ai-agent-infrastructure/**/mapper`.
4. Search repository implementations in the target bounded context.
5. Check whether the data should actually live in Redis, MinIO, Milvus, or an
   existing table.
6. Match table, column, logical-delete, and timestamp style with sibling
   tables.
7. Decide whether the query belongs in a MyBatis Plus wrapper, annotation SQL,
   or Mapper XML.
8. Decide how existing databases will be migrated, because the init SQL is
   first-start only.
