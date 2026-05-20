# Backend Application Layer

> Scope: conventions for `ai-agent-application/src/main/java/com/zj/aiagent/application/`.

## Purpose & Boundaries

The application layer owns use-case orchestration. It is the place where a
request becomes a coordinated workflow across domain aggregates, repositories,
ports, transactions, events, and framework adapters.

Application services should:

- Accept command objects, IDs, simple request values, or pagination values from
  the interface layer.
- Load aggregates through domain repositories or ports.
- Call domain behavior for business rules and state transitions.
- Persist aggregate changes through domain repository ports.
- Publish application/domain events after state changes when another use case
  must react asynchronously.
- Manage transaction boundaries for write use cases.
- Return application DTOs or domain summary projections that are safe for the
  interface layer to serialize.

Application services should not:

- Implement HTTP endpoints or parse servlet/SSE request details.
- Put core business invariants in controllers.
- Reach around domain ports to MyBatis mappers, Redis clients, MinIO clients,
  Milvus clients, WebSocket/SSE buses, or other infrastructure adapters.
- Expose mutable aggregate internals just because they are convenient for a
  controller.

Layer ownership in this project:

- `ai-agent-interfaces`: HTTP controllers, request validation groups, response
  wrapping, SSE emitter setup, user context lookup.
- `ai-agent-application`: use-case orchestration, transactions, command
  handling, cross-domain coordination, event publication/listening.
- `ai-agent-domain`: aggregates, value objects, domain services, repository
  interfaces, and technology-neutral ports.
- `ai-agent-infrastructure`: concrete adapters for persistence, Redis, SSE,
  MinIO, Milvus, MCP, LLM clients, and framework-specific integrations.

Important current-state note: `ai-agent-application/pom.xml` currently depends
on `ai-agent-infrastructure`, and GitNexus found direct infrastructure imports
inside application files. Treat these as existing architectural drift, not the
pattern for new code. New application-layer code should depend on domain ports
and let infrastructure implement those ports.

## Application Services

Use `@Service` classes for public use cases. Existing naming is mixed:

- `*ApplicationService` is used for primary feature facades:
  `AgentApplicationService`, `ChatApplicationService`,
  `KnowledgeApplicationService`, `DashboardApplicationService`,
  `MetadataApplicationService`, and `UserApplicationService`.
- Some use-case services are not suffixed with `ApplicationService`, such as
  `SchedulerService`, `SwarmMessageService`, and `McpServerService`. Prefer the
  `*ApplicationService` suffix for new feature facades unless the local module
  already has a stronger naming pattern.

Application service structure should follow this order:

- Spring/Lombok stereotypes: `@Slf4j`, `@Service`, `@RequiredArgsConstructor`.
- Final collaborators injected by constructor.
- Public command/query methods.
- Private helpers for mapping, ownership checks, normalization, and small
  orchestration details.

Example: `AgentApplicationService` orchestrates an Agent aggregate through the
domain repository and a domain service.

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/agent/service/AgentApplicationService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentApplicationService {

    private final AgentRepository agentRepository;
    private final GraphValidator graphValidator;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createAgent(AgentCommand.CreateAgentCmd cmd) {
        String graphJson = generateInitialGraphJson();

        Agent agent = Agent.builder()
                .userId(cmd.getUserId())
                .name(cmd.getName())
                .description(cmd.getDescription())
                .icon(cmd.getIcon())
                .graphJson(graphJson)
                .version(1)
                .build();

        agentRepository.save(agent);
        return agent.getId();
    }
}
```

Example: `ChatApplicationService` owns the conversation/message lifecycle and
uses `ConversationRepository`, a domain port, rather than a concrete persistence
adapter.

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatApplicationService {

    private final ConversationRepository conversationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Message appendUserMessage(String conversationId, String content, Map<String, Object> metadata) {
        if (content != null && content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Message content exceeds maximum length of %d characters", MAX_MESSAGE_LENGTH));
        }

        String filteredContent = XssFilterUtil.filter(content);
        if (filteredContent == null || filteredContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty after XSS filtering");
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(filteredContent)
                .status(MessageStatus.COMPLETED)
                .metadata(metadata)
                .build();

        return conversationRepository.saveMessage(message);
    }
}
```

Example: `KnowledgeApplicationService` coordinates a multi-step upload use case
across file validation, storage, aggregate creation, repository persistence,
dataset statistics, and async document processing.

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/knowledge/KnowledgeApplicationService.java
@Transactional
public KnowledgeDocument uploadDocument(
    String datasetId,
    MultipartFile file,
    ChunkingConfig chunkingConfig
) {
    FileValidator.validate(file);
    KnowledgeDataset dataset = getDataset(datasetId);

    String documentId = UUID.randomUUID().toString();
    String fileUrl = fileStorageService.upload(bucketName, objectName, file.getInputStream(), file.getSize());

    ChunkingConfig normalizedConfig =
        chunkingConfig != null ? chunkingConfig.normalized() : ChunkingConfig.fixedDefault();
    normalizedConfig.validate();

    KnowledgeDocument document = KnowledgeDocument.builder()
        .documentId(documentId)
        .datasetId(datasetId)
        .fileUrl(fileUrl)
        .chunkingConfig(normalizedConfig)
        .uploadedAt(Instant.now())
        .build();

    document = documentRepository.save(document);
    dataset.addDocument(document);
    datasetRepository.save(dataset);
    asyncDocumentProcessor.processDocumentAsync(document);
    return document;
}
```

## Commands & DTOs

Use command objects for application write use cases. A command should describe
the use case input after the interface layer has already handled transport
concerns such as HTTP annotations, validation groups, and current-user lookup.

Real command examples live in
`ai-agent-application/src/main/java/com/zj/aiagent/application/agent/cmd/AgentCommand.java`:

```java
public class AgentCommand {
    @Data
    public static class CreateAgentCmd {
        private Long userId;
        private String name;
        private String description;
        private String icon;
    }

    @Data
    public static class UpdateAgentCmd {
        private Long id;
        private Long userId;
        private String name;
        private String description;
        private String icon;
        private String graphJson;
        private Integer version;
    }
}
```

Validation currently happens mostly at the interface boundary. For example,
`AgentRequest.SaveAgentRequest` uses `@NotEmpty`, `@NotNull`, `@Null`, and
validation groups for create/update. `AgentController` then maps the request
into `AgentCommand.CreateAgentCmd` or `AgentCommand.UpdateAgentCmd`.

```java
// ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/agent/web/AgentController.java
@PostMapping("/update")
public Response<Void> updateAgent(
        @Validated(AgentRequest.Update.class) @RequestBody AgentRequest.SaveAgentRequest req) {
    Long userId = UserContext.getUserId();

    AgentCommand.UpdateAgentCmd cmd = new AgentCommand.UpdateAgentCmd();
    cmd.setId(req.getId());
    cmd.setUserId(userId);
    cmd.setName(req.getName());
    cmd.setDescription(req.getDescription());
    cmd.setIcon(req.getIcon());
    cmd.setGraphJson(req.getGraphJson());
    cmd.setVersion(req.getVersion());

    agentApplicationService.updateAgent(cmd);
    return Response.success();
}
```

Use DTO/result classes for application outputs when returning the aggregate
directly would leak unnecessary internal state. `AgentDetailResult` is the
current example:

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/agent/dto/AgentDetailResult.java
public static AgentDetailResult from(Agent agent) {
    AgentDetailResult result = new AgentDetailResult();
    result.setId(agent.getId());
    result.setName(agent.getName());
    result.setDescription(agent.getDescription());
    result.setIcon(agent.getIcon());
    result.setGraphJson(agent.getGraphJson());
    result.setVersion(agent.getVersion());
    result.setPublishedVersionId(agent.getPublishedVersionId());
    result.setStatus(agent.getStatus() != null ? agent.getStatus().getCode() : null);
    return result;
}
```

Command/DTO rules:

- Do not put `@RequestBody`, `@PathVariable`, `HttpServletRequest`, or
  `SseEmitter` in application commands.
- Include actor identity such as `userId` in commands when ownership or
  authorization checks are part of the use case.
- Keep optimistic-lock fields, such as Agent `version`, in commands when the
  aggregate requires them.
- Keep DTO conversion close to the application DTO class when the conversion is
  stable and shared by multiple controllers.

## Transaction Management

Put transaction boundaries on application write methods. This matches the role
of the application layer: it coordinates one use case and decides which
repository writes should commit or roll back together.

Current examples:

- `AgentApplicationService.createAgent`, `updateAgent`, `publishAgent`,
  `rollbackAgent`, `deleteAgentVersion`, `forceDeleteAgent`, and `deleteAgent`
  use `@Transactional(rollbackFor = Exception.class)`.
- `ChatApplicationService.createConversation`, `appendUserMessage`,
  `initAssistantMessage`, `finalizeMessage`, `deleteConversation`, and
  `deleteConversationWithAuth` use `@Transactional`.
- `KnowledgeApplicationService.createDataset`, `deleteDataset`, and
  `uploadDocument` use `@Transactional`.

Use `rollbackFor = Exception.class` when checked exceptions or broad adapter
exceptions should roll back the use case. Plain `@Transactional` is acceptable
for simple repository writes that only throw runtime exceptions.

Do not put transaction annotations on pure query methods unless there is a
specific consistency reason. Current query examples such as
`ChatApplicationService.getConversationHistory`,
`AgentApplicationService.listAgents`, and `KnowledgeApplicationService.getDataset`
do not use transaction annotations.

Be careful with async work inside a transaction. `KnowledgeApplicationService`
persists the document and dataset before calling
`asyncDocumentProcessor.processDocumentAsync(document)`. New code should follow
the same principle: persist the durable state before handing work to async
processors, queues, event listeners, or SSE publishers.

## Orchestration Patterns

`SchedulerService` is the canonical application-layer orchestrator for workflow
execution. It is not just a CRUD service; it coordinates Agent configuration,
domain execution state, memory hydration, node execution strategies, SSE
streaming, checkpoints, review gates, execution logs, and chat completion.

Canonical flow:

1. `WorkflowController.startExecution` opens the SSE response and calls
   `SchedulerService.startExecution(...)` asynchronously.
2. `SchedulerService.startExecution` loads the Agent, selects draft/published
   graph JSON, parses it into a `WorkflowGraph`, builds an `Execution`, and
   delegates to the internal `startExecution(Execution, inputs, mode)`.
3. The internal `startExecution` saves the user message, initializes a pending
   assistant message, hydrates long-term and short-term memory, calls
   `Execution.start(inputs)`, persists the execution, and schedules ready nodes.
4. `scheduleNode` creates a `StreamPublisher`, resolves input expressions,
   obtains a `NodeExecutorStrategy`, and executes the node asynchronously.
5. `onNodeComplete` calls `Execution.advance(nodeId, result)`, creates a
   checkpoint, updates the repository, publishes a `NodeCompletedEvent`, and
   schedules the next ready nodes.
6. When the execution reaches a terminal status, `onExecutionComplete` extracts
   the final response and calls `ChatApplicationService.finalizeMessage`.

Key `SchedulerService` dependencies show what orchestration means:

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java
private final NodeExecutorFactory executorFactory;
private final ExecutionRepository executionRepository;
private final CheckpointRepository checkpointRepository;
private final AgentRepository agentRepository;
private final HumanReviewQueuePort humanReviewQueuePort;
private final StreamPublisherFactory streamPublisherFactory;
private final ApplicationEventPublisher applicationEventPublisher;
private final HumanReviewRepository humanReviewRepository;
private final VectorStore vectorStore;
private final ConversationRepository conversationRepository;
private final ChatApplicationService chatApplicationService;
private final WorkflowNodeExecutionLogRepository workflowNodeExecutionLogRepository;
private final ExpressionResolverPort expressionResolver;
```

Workflow start is an application concern because it composes multiple domains:

```java
private void startExecution(Execution execution, Map<String, Object> inputs, ExecutionMode mode) {
    if (StringUtils.hasText(execution.getConversationId())) {
        Message userMessage = chatApplicationService.appendUserMessage(
            execution.getConversationId(),
            userInput,
            Map.of("executionId", execution.getExecutionId())
        );

        String assistantMessageId = chatApplicationService.initAssistantMessage(
            execution.getConversationId(),
            execution.getExecutionId()
        );
        execution.setAssistantMessageId(assistantMessageId);
    }

    hydrateMemory(execution, inputs);
    List<Node> readyNodes = execution.start(inputs);
    executionRepository.save(execution);
    scheduleNodes(execution.getExecutionId(), readyNodes, null);
}
```

Domain state transitions remain inside the domain aggregate:

```java
// ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java
public List<Node> start(Map<String, Object> inputs) {
    if (graph.hasCycle()) {
        throw new IllegalStateException("workflow graph has cycle");
    }
    context.setInputs(inputs);
    graph.getNodes().keySet().forEach(nodeId -> nodeStatuses.put(nodeId, ExecutionStatus.PENDING));
    this.status = ExecutionStatus.RUNNING;
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
    if (isCompleted()) {
        this.status = ExecutionStatus.SUCCEEDED;
        return Collections.emptyList();
    }
    return getReadyNodes();
}
```

This split is the main rule: application services decide "which use case steps
happen in which order"; domain objects decide "whether a state transition is
valid and what the next domain state is."

## Event Handling

Use Spring events when another application concern should react after a state
change without blocking the main use case. Events should carry IDs and compact
payloads, not infrastructure objects.

Current event patterns:

- `SchedulerService.onNodeComplete` builds a `NodeCompletedEvent` and publishes
  it through `ApplicationEventPublisher`. The event contains a
  `WorkflowNodeExecutionLog` to be handled outside the scheduling path.
- `SwarmMessageService.sendMessage` persists a message, publishes a
  `SwarmMessageSentEvent`, and then emits a UI event.
- `SwarmMessageEventListener.onMessageSent` listens asynchronously and wakes
  other agents in the group.
- `ExecutionCompletedListener` exists as an async listener for
  `ExecutionCompletedEvent`, but the comments show it is not currently the
  primary completion path; `SchedulerService.onExecutionComplete` directly
  finalizes the assistant message.
- `ChatApplicationService.appendUserMessage` has a commented-out
  `MessageAppendedEvent` publication. Do not treat commented event hooks as
  active behavior.

Real publish/listen example:

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/SwarmMessageService.java
messageRepository.save(message);

eventPublisher.publishEvent(
    new SwarmMessageSentEvent(this, groupId, request.getSenderId(), group.getWorkspaceId())
);
```

```java
// ai-agent-application/src/main/java/com/zj/aiagent/application/swarm/event/SwarmMessageEventListener.java
@Async
@EventListener
public void onMessageSent(SwarmMessageSentEvent event) {
    List<Long> memberIds = groupRepository.findMemberIds(event.getGroupId());
    for (Long memberId : memberIds) {
        if (!memberId.equals(event.getSenderId())) {
            runtimeService.wakeAgent(memberId);
        }
    }
}
```

Event rules:

- Publish after the local state change is durable enough for listeners to read.
- Use `@Async` listeners for slow or follow-up work that should not block the
  original use case.
- Do not hide mandatory same-transaction invariants behind async events.
- If an event is part of the user-visible contract, document who publishes it,
  who listens to it, and what IDs tie the listener back to source state.

## Dependency Direction

Preferred dependency direction:

```text
interfaces -> application -> domain
                         -> shared
infrastructure -> domain ports
```

Application services may use Spring stereotypes and transaction annotations.
They may also use application services from another application submodule for
cross-domain orchestration, as `SchedulerService` does with
`ChatApplicationService`.

Application services should depend on these domain abstractions:

- Repositories such as `AgentRepository`, `ConversationRepository`,
  `KnowledgeDatasetRepository`, `KnowledgeDocumentRepository`,
  `ExecutionRepository`, and `WorkflowNodeExecutionLogRepository`.
- Ports such as `VectorStore`, `FileStorageService`, `StreamPublisherFactory`,
  `HumanReviewQueuePort`, `ExpressionResolverPort`, and
  `NodeExecutorStrategy`.
- Domain services such as `GraphValidator`.

The current codebase has known application-to-infrastructure imports, including
`SchedulerService` importing `WorkflowGraphFactoryImpl`, `IRedisService`, and
`NodeExecutorFactory`, `AutoTitleListener` importing `WebSocketMessageService`,
and several Swarm services importing infrastructure LLM/SSE/MCP adapters. New
work should avoid adding more of these; introduce or reuse a domain port
instead.

When an application service needs technology behavior:

- Add a domain port if the domain concept already owns the abstraction.
- Add an application-facing port only if the behavior is orchestration-specific
  and does not belong in a domain package.
- Implement the port in infrastructure and inject the interface into
  application code.
- Keep adapter types, persistence objects, mapper types, Redis/SSE clients, and
  framework-specific event buses out of application method signatures.

## Anti-patterns

Avoid these patterns in application-layer work:

- Controller owns orchestration: a controller loads aggregates, calls multiple
  repositories, publishes events, and updates cross-domain state directly.
- Application bypasses domain behavior: setting aggregate fields manually when a
  domain method such as `Agent.publish(...)`, `Agent.rollbackTo(...)`,
  `KnowledgeDataset.addDocument(...)`, or `Execution.advance(...)` already owns
  the transition.
- Application imports concrete infrastructure adapters for new behavior instead
  of depending on a domain port.
- Application methods accept HTTP transport types unless the feature is already
  a documented exception, such as file upload using `MultipartFile` in
  `KnowledgeApplicationService`.
- Transactions are placed only around repository calls in infrastructure while
  the actual use case spans multiple repository writes.
- Async listeners perform required state changes with no retry, idempotency, or
  source ID tying them back to the original use case.
- DTOs expose secrets or raw adapter objects. For example, LLM config DTOs
  should expose provider/model/base URL metadata, not API keys.
- Command objects omit `userId` or version fields needed for ownership checks
  and optimistic locking.
- Event hooks are documented as active when the code only contains a commented
  `publishEvent(...)`.

## Pre-change Checklist

Before changing application-layer code:

- Search for an existing application service or domain service that already
  owns the use case.
- Use GitNexus impact analysis before editing a function, method, or class.
- Confirm whether the dependency you need is already represented as a domain
  repository or port.
- Keep writes inside one application-level transaction unless the use case
  explicitly requires async/eventual consistency.
- Keep cross-domain orchestration in application services, not aggregates.
- Check whether the controller already performs request validation and command
  mapping before adding duplicate validation in the application service.
- If adding an event, document publisher, listener, payload IDs, transaction
  timing, and retry/idempotency expectations.
