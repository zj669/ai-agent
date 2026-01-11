# 任务文档

- [x] 1. 实现领域层实体 (Domain Layer Entities)
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/entity/Agent.java
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/entity/AgentVersion.java
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/valobj/AgentStatus.java
  - 定义 `Agent` 聚合根，包含字段: `id`, `userId`, `name`, `description`, `graphJson`, `status`, `publishedVersionId`, `version`, `createTime`, `updateTime`
  - 实现核心行为: `publish(validator)`, `updateConfig(...)`, `rollbackTo(...)`, `clone(...)`
  - **实现安全校验方法**: `boolean isOwnedBy(Long userId)`
  - 定义 `AgentVersion` 实体和 `AgentStatus` 枚举
  - 目标: 建立核心业务逻辑、状态一致性保障及安全基石
  - _Requirements: 2.1, 2.2 (#5, #6, #8)_
  - _Prompt: Role: Java Backend Developer specializing in DDD | Task: Implement Agent aggregate root and related entities enforcing documented business rules including optimistic locking and version control | Restrictions: Use JPA/MyBatis annotations as needed. **Implement a logic method `boolean isOwnedBy(Long userId)` within the Agent entity for security checks.** Ensure `graphJson` is defined as a flexible structure (e.g., String or Object/Map). | Success: Entities contain all required fields, logic for publish/rollback/clone is implemented correctly, **and optimistic locking uses a `version` field**._

- [x] 2. 实现领域服务和仓储接口 (Domain Services & Repository Interface)
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/service/GraphValidator.java
  - 文件: ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/repository/AgentRepository.java
  - 实现 `GraphValidator` 用于纯内存结构校验
  - 定义 `AgentRepository` 接口，严格分离命令 (save/findById) 和查询 (findSummary) 操作
  - 目标: 定义数据访问契约和高内聚的校验规则
  - _Requirements: 2.1, 2.2 (#6)_
  - _Prompt: Role: Java Architect | Task: Create domain service for DAG validation and repository interfaces following CQRS principles | Restrictions: `GraphValidator` must be pure logic without DB dependencies. **It must implement methods to detect: 1. Cycles (using topological sort or DFS), 2. Connectivity (all nodes reachable from Start), 3. Single Start Node.** `AgentRepository` must separate `findSummary` (excludes graphJson) from `findById` (includes graphJson). | Success: Validator correctly identifies invalid graphs, Repository interface covers all CRUD and versioning needs._

- [x] 3. 实现基础设施持久层 (Infrastructure Persistence Layer)
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent/po/AgentPO.java
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent/po/AgentVersionPO.java
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent/mapper/AgentMapper.java
  - 文件: ai-agent-infrastructure/src/main/resources/mapper/agent/AgentMapper.xml
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent/repository/AgentRepositoryImpl.java
  - 实现 MyBatis POs, Mappers 和 XML 配置
  - **配置 JSON 类型处理器**: 使用 JacksonTypeHandler 自动映射 `graph_json`
  - 实现 SQL 级别的乐观锁 (`WHERE version = ?`)
  - 目标: 提供高性能且数据安全的持久化实现
  - _Requirements: 2.1 (#1, #2, #3)_
  - _Prompt: Role: Backend Developer with MyBatis expertise | Task: Implement infrastructure layer including POs, Mappers, and RepositoryImpl | Restrictions: **Use `JacksonTypeHandler` (or custom TypeHandler) in MyBatis XML/Mapper to automatically map `graph_json` (DB TEXT) to Java Object/Map.** Implement optimistic locking in the `update` SQL statement (`WHERE id = ? AND version = ?`). **Return affected rows to catch concurrency errors.** | Success: Data is correctly saved/retrieved, JSON parsing is automated, optimistic locking updates verify version match._

- [x] 4. 实现元数据服务 (Metadata Service Support)
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/meta/po/NodeTemplatePO.java
  - 文件: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/meta/mapper/NodeTemplateMapper.java (及 XML)
  - 文件: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/MetadataApplicationService.java
  - 实现对 `node_template` 和 `sys_config_field_def` 表的只读访问
  - 目标: 为前端编辑器提供节点定义、参数配置和校验规则元数据 (Server-Driven UI)
  - _Requirements: 2.1 (Editor Support)_
  - _Prompt: Role: Backend Developer | Task: Implement read-only access for `node_template` and `sys_config_field_def` tables and a service to expose them. | Context: Frontend uses this data to render the DAG editor dynamically (Server-Driven UI). | Success: APIs return full node definitions including their config field mappings and validation rules._

- [x] 5. 实现应用层服务 (Application Layer Services)
  - 文件: ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java
  - 实现生命周期管理: `create`, `update`, `publish`, `copy`, `rollback`
  - 实现调试逻辑: `debugAgent` (读取草稿而非发布版本)
  - **安全与事务**: 首行执行 `isOwnedBy` 校验，处理 `OptimisticLockingFailureException`
  - 目标: 协调业务流，确保数据一致性和安全性
  - _Requirements: 2.1, 2.2, 2.3 (#1-#9)_
  - _Prompt: Role: Senior Java Developer | Task: Implement AgentApplicationService orchestrating all use cases | Restrictions: **Security First: Call `agent.isOwnedBy(currentUser)` immediately after loading.** Transactional consistency is required. **For `debugAgent`, explicitly load configuration from `Agent` (Draft), whereas normal chat flows would load from `AgentVersion` (Published).** Handle `OptimisticLockingFailureException` gracefully. | Success: Service methods implement full logic, security checks are robust, debug uses draft data._

- [x] 6. 实现接口层 (Interface Layer: DTOs & Controller)
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/agent/dto/AgentDTOs.java
  - 文件: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/web/agent/AgentController.java
  - 定义 DTOs 并使用 **JSR-303 Groups** 区分 Create(无ID)/Update(有ID) 校验
  - 实现 `debugAgent` 的 SSE 端点，返回包含 `inputs/outputs` 的 `debugInfo`
  - 目标: 提供标准 REST API 和流式调试接口
  - _Requirements: 2.1, 2.2, 2.3_
  - _Prompt: Role: API Developer | Task: Implement AgentController and DTOs following REST best practices and SSE for debug | Restrictions: **Use `@Validated` with Groups for DTOs (distinguish Create vs Update constraints).** The `debugAgent` endpoint must return `SseEmitter` and **stream events complying with the `RenderConfig` format defined in design docs (including inputs/outputs in debugInfo)**. | Success: API endpoints match design specs, DTOs handle validation, Debug endpoint streams detailed execution data._

- [x] 7. 验证计划 (Verification Plan)ual Testing)
  - 手动测试验证所有端点
  - 测试乐观锁 (场景: 并发更新触发异常)
  - 测试回滚 (场景: 发布V1 -> 修改V2 -> 回滚V1)
  - 测试调试模式 (场景: 验证草稿数据的即时生效和DebugInfo输出)
  - 测试元数据接口 (场景: 确认前端能获取节点定义)
  - 目标: 在交付前确保功能符合预期
  - _Requirements: All_
  - _Prompt: Role: QA Engineer | Task: Perform manual verification of implemented features | Restrictions: Focus on edge cases like concurrency and invalid graph structures | Success: All acceptance criteria from requirements.md are met_
