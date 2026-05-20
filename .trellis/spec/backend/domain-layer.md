# Backend Domain Layer

This guide documents the current domain-layer conventions for the AI Agent
Platform backend. It is based on `ai-agent-domain/`, the domain-facing types in
`ai-agent-shared/`, GitNexus code graph results, and direct source reads.

## Purpose & Dependency Rules

The domain layer owns business concepts and invariants. It should model the
platform in terms of aggregates, value objects, domain services, repository
interfaces, and ports. Application services orchestrate use cases around these
domain types. Infrastructure code implements persistence, Redis, SSE, Milvus,
MinIO, MyBatis Plus, Spring AI, and other technical adapters.

Layer direction:

```text
interfaces -> application -> domain <- infrastructure
                           ^
                         shared
```

For new domain code, keep the dependency surface intentionally narrow:

- Allowed: Java standard library, domain packages, and `ai-agent-shared` where
  the shared type is a framework-neutral utility or constant.
- Common current tooling: Lombok annotations such as `@Data`, `@Builder`,
  `@Getter`, and `@RequiredArgsConstructor` are used throughout the existing
  domain model.
- Forbidden in new domain code: Spring stereotypes and infrastructure APIs,
  MyBatis Plus, Redisson, Milvus SDK, MinIO SDK, HTTP controller types, database
  persistence objects, and framework-specific DTOs.
- Domain repository and port interfaces must expose domain types or neutral
  Java types. Framework-specific exceptions, pageable types, and adapter
  classes should stay outside domain contracts.

Current source has known framework leakage that should not be copied:

- `ai-agent-domain/pom.xml` currently declares `spring-data-commons`,
  `spring-context`, `jackson-databind`, and `spring-security-crypto` in addition
  to `ai-agent-shared` and Lombok.
- Domain files currently importing Spring include
  `domain/knowledge/repository/KnowledgeDocumentRepository.java`,
  `domain/chat/port/ConversationRepository.java`,
  `domain/agent/service/GraphValidator.java`,
  `domain/auth/service/ratelimit/RateLimiterFactory.java`,
  `domain/swarm/service/SwarmDomainService.java`,
  `domain/user/service/UserAuthenticationDomainService.java`, and
  `domain/user/valobj/Email.java`.
- Treat those imports as existing technical debt or compatibility exceptions,
  not as a pattern for new domain work.

## Aggregate Roots

Aggregate roots live under each bounded context's `entity/` package. They hold
identity, mutable state, and behavior that changes that state. Verified examples:

- `domain/workflow/entity/Execution.java`
- `domain/agent/entity/Agent.java`
- `domain/chat/entity/Conversation.java`
- `domain/knowledge/entity/KnowledgeDataset.java`
- `domain/workflow/entity/WorkflowGraph.java`

`Execution` is the canonical aggregate for workflow execution. It owns the
execution id, graph, context, global status, per-node status map, pause state,
reviewed nodes, timestamps, and optimistic version. The aggregate exposes
business methods for start, dispatch marking, advance, resume, reject, ready
node calculation, branch pruning, and checkpoint creation.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

    private String executionId;
    private Long agentId;
    private Long userId;
    private String conversationId;
    private String assistantMessageId;
    private WorkflowGraph graph;

    @Builder.Default
    private ExecutionContext context = new ExecutionContext();

    @Builder.Default
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Builder.Default
    private Map<String, ExecutionStatus> nodeStatuses = new HashMap<>();

    private String pausedNodeId;
    private TriggerPhase pausedPhase;

    @Builder.Default
    private Set<String> reviewedNodes = new HashSet<>();

    @Builder.Default
    private Integer version = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public List<Node> start(Map<String, Object> inputs) {
        if (graph.hasCycle()) {
            throw new IllegalStateException("工作流图存在循环依赖");
        }

        context.setInputs(inputs);
        graph.getNodes().keySet().forEach(nodeId ->
            nodeStatuses.put(nodeId, ExecutionStatus.PENDING)
        );

        this.status = ExecutionStatus.RUNNING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        return getReadyNodes();
    }

    public List<Node> advance(String nodeId, NodeExecutionResult result) {
        nodeStatuses.put(nodeId, result.getStatus());

        if (result.getOutputs() != null) {
            context.setNodeOutput(nodeId, result.getOutputs());
        }

        if (result.isRouting()) {
            pruneUnselectedBranches(nodeId, result.getSelectedBranchId());
        }

        if (result.isPaused()) {
            this.status = ExecutionStatus.PAUSED_FOR_REVIEW;
            this.pausedNodeId = nodeId;
            this.pausedPhase = result.getTriggerPhase();
            this.updatedAt = LocalDateTime.now();
            this.version++;
            return Collections.emptyList();
        }

        this.updatedAt = LocalDateTime.now();
        this.version++;

        if (isCompleted()) {
            this.status = ExecutionStatus.SUCCEEDED;
            return Collections.emptyList();
        }

        if (hasFailed()) {
            this.status = ExecutionStatus.FAILED;
            return Collections.emptyList();
        }

        return getReadyNodes();
    }
}
```

The example above is shortened to focus on aggregate fields and public state
transitions. The real class also includes `markRunning`, `resume`, `reject`,
recursive branch skipping, `isCompleted`, `hasFailed`, and
`createCheckpoint`.

Other aggregate patterns:

- `Agent` owns draft graph JSON, lifecycle status, published version pointer,
  optimistic version, ownership checks, publish, rollback, clone, and disable.
- `Conversation` is intentionally small: it owns title updates and the updated
  timestamp for a chat conversation.
- `KnowledgeDataset` owns dataset identity, owner, optional agent binding,
  document/chunk counters, and the metadata filter used to isolate vectors.
- `WorkflowGraph` owns DAG nodes and edges and offers topology operations such
  as `getSuccessors`, `getPredecessors`, and `hasCycle`.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/entity/KnowledgeDataset.java`:

```java
public class KnowledgeDataset {
    private String datasetId;
    private String name;
    private String description;
    private Long userId;
    private Long agentId;

    @Builder.Default
    private Integer documentCount = 0;

    @Builder.Default
    private Integer totalChunks = 0;

    public void addDocument(KnowledgeDocument document) {
        this.documentCount++;
        this.updatedAt = Instant.now();
    }

    public Map<String, Object> buildMetadataFilter() {
        Map<String, Object> filter = new HashMap<>();
        filter.put("dataset_id", this.datasetId);
        if (this.agentId != null) {
            filter.put("agent_id", this.agentId);
        }
        return filter;
    }
}
```

Guidelines:

- Put invariant-changing behavior on the aggregate when the behavior primarily
  affects that aggregate's own state.
- Keep identity and lifecycle fields in the aggregate, not in DTOs.
- Let infrastructure translate persistence objects to domain objects. Do not
  import `*PO`, MyBatis mappers, Redis clients, or Spring AI classes into an
  aggregate.
- Use repository methods to load and save aggregates instead of making
  aggregates perform persistence.

## Value Objects

Value objects live under `valobj/`. They should be immutable when possible,
validate their own construction, and implement value equality. Verified examples
include `Email`, `Credential`, `ExecutionContext`, `NodeExecutionResult`,
`StreamContext`, `AgentSummary`, `ChunkingConfig`, `DashboardStats`, and memory
`Document`.

`Credential` is the cleanest value-object example. It stores only encrypted
password data, has a final field, exposes factory reconstruction, and delegates
password matching to the shared BCrypt utility.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/valobj/Credential.java`:

```java
@Getter
@ToString
public class Credential {

    private final String encryptedPassword;

    Credential(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public static Credential fromEncrypted(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            throw new IllegalArgumentException("Encrypted password cannot be empty");
        }
        return new Credential(encryptedPassword);
    }

    public boolean verify(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return false;
        }
        return BCryptUtil.matches(rawPassword, this.encryptedPassword);
    }
}
```

`Email` validates format and stores a final value, but it currently imports
`org.springframework.util.Assert`. Do not copy that dependency in new value
objects; use plain Java validation.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/valobj/Email.java`:

```java
@Getter
@ToString
@EqualsAndHashCode
public class Email {
    private static final String EMAIL_REGEX =
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

    private final String value;

    public Email(String value) {
        Assert.hasText(value, "Email cannot be empty");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + value);
        }
        this.value = value;
    }

    public static Email of(String value) {
        return new Email(value);
    }
}
```

Guidelines:

- Prefer `final` fields and constructor/factory validation.
- Value objects may call framework-neutral helpers from `ai-agent-shared`.
- Avoid framework assertions, mutable public collections, and hidden persistence
  behavior.
- A value object should describe a domain concept, not a request or response
  payload.

## Domain Services

Use a domain service when business logic spans multiple aggregates, repositories,
ports, or policies and does not naturally belong on a single aggregate. Verified
domain services include:

- `domain/user/service/UserAuthenticationDomainService.java`
- `domain/swarm/service/SwarmDomainService.java`
- `domain/agent/service/GraphValidator.java`
- `domain/knowledge/service/KnowledgeRetrievalService.java`

`UserAuthenticationDomainService` coordinates email validation, verification
code storage, rate limiting, password policy, credential creation, user state,
and repository persistence. That behavior crosses `User`, `Email`,
`Credential`, rate-limit policy, verification-code storage, and email delivery,
so it belongs in a service rather than on `User`.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/service/UserAuthenticationDomainService.java`:

```java
public User register(String emailStr, String code, String password, String username) {
    Email email = Email.of(emailStr);

    String storedCode = verificationCodeRepository.get(email);
    if (storedCode == null || !storedCode.equals(code)) {
        throw new AuthenticationException(ErrorCode.INVALID_VERIFICATION_CODE);
    }

    if (userRepository.existsByEmail(email)) {
        throw new AuthenticationException(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    validatePasswordStrength(password);

    Credential credential = Credential.fromEncrypted(BCryptUtil.encode(password));
    String finalUsername = (username == null || username.isBlank())
        ? extractUsernameFromEmail(emailStr)
        : username;

    User newUser = new User(finalUsername, email, credential);
    User savedUser = userRepository.save(newUser);

    verificationCodeRepository.remove(email);
    return savedUser;
}
```

`GraphValidator` validates graph JSON structure before `Agent.publish`. It is a
domain service because the validation is a graph policy, not persistence, HTTP,
or UI behavior.

Guidelines:

- Put single-aggregate state transitions on the aggregate.
- Put cross-aggregate policy, repository coordination, and domain calculations
  in a domain service.
- Domain service method names should describe business actions, not transport
  actions. Use names such as `register`, `login`, `markRead`, `validate`, or
  `retrieve`.
- Current services use Spring `@Service` in domain. New code should avoid adding
  more domain stereotypes unless the project deliberately accepts that
  dependency leak.

## Repository Interfaces

Repository interfaces define persistence contracts in domain terms. The
implementation belongs in `ai-agent-infrastructure/`.

Canonical repository naming uses the business name without an `I` prefix:

- `domain/agent/repository/AgentRepository.java`
- `domain/knowledge/repository/KnowledgeDatasetRepository.java`
- `domain/swarm/repository/SwarmAgentRepository.java`
- `domain/workflow/port/ExecutionRepository.java`

`AgentRepository` is the cleanest naming example. It separates full command
loads from lightweight query summaries and keeps methods in domain language.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/repository/AgentRepository.java`:

```java
public interface AgentRepository {

    void save(Agent agent);

    void deleteById(Long id);

    Optional<Agent> findById(Long id);

    List<AgentSummary> findSummaryByUserId(Long userId);

    void saveVersion(AgentVersion version);

    Optional<AgentVersion> findVersion(Long agentId, Integer version);

    Optional<Integer> findMaxVersion(Long agentId);

    List<AgentVersion> findVersionHistory(Long agentId);

    void deleteVersion(Long agentId, Integer version);

    void deleteAllVersions(Long agentId);
}
```

The infrastructure implementation is
`ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/agent/repository/AgentRepositoryImpl.java`:

```java
@Slf4j
@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentMapper agentMapper;
    private final AgentVersionMapper agentVersionMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Agent agent) {
        AgentPO po = toPO(agent);
        if (po.getId() == null) {
            agentMapper.insert(po);
            agent.setId(po.getId());
        } else {
            int rows = agentMapper.updateById(po);
            if (rows == 0) {
                throw new OptimisticLockingFailureException(
                    "Update failed: Agent modified by another user (Optimistic Lock)");
            }
        }
        agent.setVersion(po.getVersion());
        agent.setUpdateTime(po.getUpdateTime());
    }
}
```

Guidelines:

- Domain repository interfaces should return aggregates, value objects, or
  `Optional<T>`/`List<T>`.
- Infrastructure repository implementations may use Spring, MyBatis Plus,
  Redis, Jackson, transactions, and persistence objects.
- Prefer no `I` prefix for new repository interfaces.
- Existing exceptions include `IUserRepository`, `IVerificationCodeRepository`,
  `IMcpServerRepository`, `IMcpConnectionManager`, and `IMcpToolRegistry`.
  Treat those as legacy naming, not the preferred pattern.

## Ports & Adapters

Ports are domain-facing interfaces that describe what the domain or application
needs without committing to a technology. Adapters implement those ports in
infrastructure.

Verified ports include:

- `domain/workflow/port/ExecutionRepository.java`
- `domain/workflow/port/CheckpointRepository.java`
- `domain/workflow/port/HumanReviewRepository.java`
- `domain/workflow/port/HumanReviewQueuePort.java`
- `domain/workflow/port/ConditionEvaluatorPort.java`
- `domain/workflow/port/ExpressionResolverPort.java`
- `domain/workflow/port/NodeExecutorStrategy.java`
- `domain/workflow/port/StreamPublisher.java`
- `domain/workflow/port/StreamPublisherFactory.java`
- `domain/workflow/port/WorkflowNodeExecutionLogRepository.java`
- `domain/memory/port/VectorStore.java`
- `domain/knowledge/port/DocumentReaderPort.java`
- `domain/knowledge/port/FileStorageService.java`
- `domain/knowledge/port/TextSplitterPort.java`
- `domain/chat/port/ConversationRepository.java`
- `domain/mcp/port/IMcpConnectionManager.java`
- `domain/mcp/port/IMcpServerRepository.java`
- `domain/mcp/port/IMcpToolRegistry.java`

`ChatModelPort` is referenced in older project notes but is not present in the
current source tree. Do not document or use it as a current domain port unless
it is added to code first.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/StreamPublisher.java`:

```java
public interface StreamPublisher {

    void publishStart();

    void publishDelta(String delta);

    void publishDelta(String delta, boolean isThought);

    void publishThought(String thought);

    void publishFinish(NodeExecutionResult result);

    void publishError(String errorMessage);

    void publishData(Object data, String renderMode);

    void publishEvent(String eventType, java.util.Map<String, Object> payload);
}
```

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/memory/port/VectorStore.java`:

```java
public interface VectorStore {

    List<String> search(String query, Long agentId, int topK);

    default List<String> search(String query, Long agentId) {
        return search(query, agentId, 5);
    }

    void store(Long agentId, String content, Map<String, Object> metadata);

    void storeBatch(Long agentId, List<String> contents);

    List<Document> similaritySearch(SearchRequest request);

    void addDocuments(List<Document> documents);

    void deleteByMetadata(Map<String, Object> filter);
}
```

Verified adapter examples:

- `RedisExecutionRepository implements ExecutionRepository`
- `RedisSseStreamPublisher implements StreamPublisher`
- `RedisSseStreamPublisherFactory implements StreamPublisherFactory`
- `MilvusVectorStoreAdapter implements VectorStore`
- `NoOpVectorStoreAdapter implements VectorStore`
- `StructuredConditionEvaluator implements ConditionEvaluatorPort`
- `ExpressionResolver implements ExpressionResolverPort`
- `RoutedTextSplitterAdapter implements TextSplitterPort`
- `SpringAIDocumentReaderAdapter implements DocumentReaderPort`

Guidelines:

- Port methods should use domain value objects, aggregates, and neutral Java
  collections.
- Adapters are responsible for translating framework types to domain types.
- Keep conditional Spring configuration, Redis keys, MyBatis wrappers, Milvus
  search requests, SSE serialization, and MinIO object operations in
  infrastructure.
- Avoid default port methods that throw `UnsupportedOperationException` unless
  the default is genuinely optional and documented.

## State Machines

Workflow execution is the canonical state machine. The same enum is used for
the execution and node lifecycle.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionStatus.java`:

```java
@Getter
@AllArgsConstructor
public enum ExecutionStatus {
    PENDING(0),
    RUNNING(1),
    SUCCEEDED(2),
    FAILED(3),
    SKIPPED(4),
    PAUSED(5),
    CANCELLED(6),
    PAUSED_FOR_REVIEW(10);

    private final int code;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
```

Canonical transition paths:

- Start: `PENDING -> RUNNING` when `Execution.start` initializes all node
  statuses and returns ready nodes.
- Dispatch: node-level `PENDING -> RUNNING` in `Execution.markRunning`.
- Node success: node-level `RUNNING -> SUCCEEDED` through
  `NodeExecutionResult.success` and `Execution.advance`.
- Condition pruning: unselected branches become `SKIPPED` through
  `pruneUnselectedBranches` and `skipNodeRecursively`.
- Human review: `RUNNING -> PAUSED_FOR_REVIEW` when a node result is paused.
- Review approval: `PAUSED_FOR_REVIEW -> RUNNING` in `Execution.resume`.
- Review rejection: paused node and whole execution become `FAILED`.
- Completion: if every node is `SUCCEEDED`, `SKIPPED`, or `FAILED`,
  `Execution.advance` marks the aggregate `SUCCEEDED` first, then checks failure.
  Because the implementation checks `isCompleted()` before `hasFailed()`, a graph
  containing a failed node can be treated as completed before the failure branch.
  Preserve this behavior only if it is intentional; otherwise fix code and tests
  before changing the spec.

Example from `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java`:

```java
public static NodeExecutionResult paused(TriggerPhase phase, Map<String, Object> outputs) {
    return NodeExecutionResult.builder()
        .status(ExecutionStatus.PAUSED_FOR_REVIEW)
        .triggerPhase(phase)
        .outputs(outputs)
        .build();
}

public boolean isPaused() {
    return status == ExecutionStatus.PAUSED || status == ExecutionStatus.PAUSED_FOR_REVIEW;
}

public boolean isRouting() {
    return selectedBranchId != null;
}
```

Guidelines:

- State transitions belong in the aggregate or value-object factory methods.
- Application services should call domain methods rather than mutate status maps
  directly.
- Keep enum codes stable because persistence and event payloads may rely on
  them.
- When adding a new status, update terminal-state checks, scheduler behavior,
  DTO mapping, SSE payloads, and tests together.

## Anti-Patterns

Do not add framework annotations to new domain classes.

Reason: Spring stereotypes in domain couple business rules to the IoC container.
Current examples such as `GraphValidator`, `SwarmDomainService`, and
`UserAuthenticationDomainService` import `org.springframework.stereotype.*`;
these should be treated as legacy leakage, not new convention.

Do not expose infrastructure types from domain contracts.

Reason: Domain contracts become hard to implement or test without the selected
technology. Existing examples to avoid copying include Spring `Pageable` in
`ConversationRepository` and `KnowledgeDocumentRepository`, and the Spring
exception reference in `ExecutionRepository.update`.

Do not invent repository implementations in domain.

Reason: `AgentRepository` proves that the domain should define the contract,
while `AgentRepositoryImpl` in infrastructure owns MyBatis, Jackson conversion,
transactions, and persistence objects.

Do not duplicate state-machine decisions outside `Execution`.

Reason: `Execution` owns pause, resume, reject, ready-node calculation, pruning,
and checkpoint behavior. Application code should coordinate the workflow, not
rebuild the state machine in parallel.

Do not document unverified ports as current architecture.

Reason: `ChatModelPort` appears in older notes but was not found in the current
source tree. Specs must describe code facts first, then call out missing or
planned abstractions separately.
