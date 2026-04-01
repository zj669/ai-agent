# Java Backend Multi-Agent Collaboration Patterns

> Research based on Spring Boot DDD architecture in `/ai-agent-domain/`, `/ai-agent-application/`, `/ai-agent-infrastructure/`, `/ai-agent-interfaces/`.
> Date: 2026-03-31

---

## Executive Summary

The Java backend implements two distinct multi-agent collaboration systems:

1. **Swarm Module** — Agent-to-agent P2P messaging via groups, virtual threads, event-driven wake/sleep lifecycle
2. **Workflow Engine** — DAG-based node orchestration with SSE streaming, human review checkpoints, and distributed locking

---

## 1. Swarm Module: Agent-to-Agent Communication

### 1.1 Core Entities

**SwarmGroup** — Represents a P2P conversation group:
```java
// ai-agent-domain/swarm/entity/SwarmGroup.java
public class SwarmGroup {
    private Long id;
    private Long workspaceId;
    private String name;
    private Integer contextTokens;
}
```

**SwarmMessage** — Inter-agent messages:
```java
// ai-agent-domain/swarm/entity/SwarmMessage.java
public class SwarmMessage {
    private Long id;
    private Long workspaceId;
    private Long groupId;
    private Long senderId;
    private String contentType;
    private String content;
    private LocalDateTime sendTime;
}
```

**SwarmAgent** — Agent entity with parent hierarchy:
```java
// ai-agent-domain/swarm/entity/SwarmAgent.java
public class SwarmAgent {
    private Long id;
    private Long parentId; // Who created this agent
    private SwarmAgentStatus status;
}
```

### 1.2 Event-Driven Wake Mechanism

**File:** `ai-agent-application/swarm/event/SwarmMessageEventListener.java`

When a message is sent, all agents in the group (except sender) are woken up:

```java
@Async
@EventListener
public void onMessageSent(SwarmMessageSentEvent event) {
    for (Long memberId : memberIds) {
        if (!memberId.equals(event.getSenderId())) {
            runtimeService.wakeAgent(memberId);
        }
    }
}
```

### 1.3 Virtual Thread Agent Runtime

**File:** `ai-agent-application/swarm/SwarmAgentRuntimeService.java`

Each Swarm agent runs as a **Java 21 Virtual Thread**:

```java
public void startAgent(SwarmAgent agent) {
    SwarmAgentRunner runner = new SwarmAgentRunner(...);
    runners.put(agent.getId(), runner);
    Thread.ofVirtual().name("swarm-agent-" + agent.getId()).start(runner);
}

public void wakeAgent(Long agentId) {
    runners.get(agentId).wake();
}

public void stopAgent(Long agentId) {
    runners.remove(agentId).stop();
    // Wake parent when child stops
    if (agent.getParentId() != null) {
        wakeAgent(agent.getParentId());
    }
}
```

**SwarmAgentRunner** — Blocking runner with CompletableFuture wake signal:
```java
// ai-agent-application/swarm/runtime/SwarmAgentRunner.java
public class SwarmAgentRunner implements Runnable {
    private volatile boolean running = true;
    private final CompletableFuture<Void> wakeSignal = new CompletableFuture<>();

    @Override
    public void run() {
        while (running) {
            currentWakeSignal.join(); // Block until woken
            if (!running) break;
            currentWakeSignal = new CompletableFuture<>();
            processTurn(); // LLM call + tool execution
        }
    }

    public void wake() { currentWakeSignal.complete(null); }
    public void stop() { running = false; currentWakeSignal.complete(null); }
}
```

### 1.4 Agent Tool-Based Spawning & Delegation

**File:** `ai-agent-application/swarm/runtime/SwarmTools.java`

Agents can spawn and delegate via tools:

```java
@Tool(description = "Create a workflow-executable Agent")
public String createAgent(String role, String description, String graphJson) {
    Long workflowAgentId = workflowAgentService.createAgent(createCmd);
    // Returns agentId, name, description, version, graphReady
}

@Tool(description = "Delegate task to another Agent")
public String send(long agentId, String message) {
    // Triggers SwarmMessageSentEvent -> wakeAgent(targetAgentId)
    SwarmMessageDTO sent = messageService.sendMessage(groupId, req);
}

@Tool(description = "Execute a workflow Agent and wait for result")
public String executeWorkflow(long agentId, String input) {
    // Synchronous blocking call, 10 min timeout
    Map<String, Object> result = schedulerService.executeAndWait(
        agentId, callerUserId, inputs, ExecutionMode.STANDARD, 10 * 60 * 1000L);
}
```

### 1.5 Dual SSE Buses (Agent-Level + UI-Level)

**Agent Event Bus:**
```java
// ai-agent-infrastructure/swarm/sse/SwarmAgentEventBus.java
public class SwarmAgentEventBus {
    private final Map<Long, List<Consumer<AgentEvent>>> subscribers = new ConcurrentHashMap<>();
    private final Map<Long, List<AgentEvent>> history = new ConcurrentHashMap<>();
    public void emit(Long agentId, AgentEvent event) { ... }
    public List<AgentEvent> getSince(Long agentId, int fromIndex) { ... }
}
```

**UI Event Bus:**
```java
// ai-agent-infrastructure/swarm/sse/SwarmUIEventBus.java
public class SwarmUIEventBus {
    private final Map<Long, List<Consumer<UIEvent>>> subscribers = new ConcurrentHashMap<>();
    // Events: ui.agent.created, ui.message.created, ui.agent.llm.start, ui.agent.stream.chunk, etc.
}
```

**SSE Endpoints:**
```
GET /api/swarm/agent/{agentId}/stream        — Agent-level stream
GET /api/swarm/workspace/{workspaceId}/ui-stream — UI-level stream
```

---

## 2. Workflow Engine: DAG-Based Node Orchestration

### 2.1 Core Aggregation Root

**File:** `ai-agent-domain/workflow/entity/Execution.java`

```java
public class Execution {
    private String executionId;
    private WorkflowGraph graph;
    private ExecutionContext context;
    private ExecutionStatus status;
    private Map<String, ExecutionStatus> nodeStatuses;
    private String pausedNodeId;
    private TriggerPhase pausedPhase;
    private Set<String> reviewedNodes;

    public List<Node> start(Map<String, Object> inputs);
    public List<Node> advance(String nodeId, NodeExecutionResult result);
    public List<Node> resume(String nodeId, Map<String, Object> additionalInputs);
    public void reject(String nodeId);
    public List<Node> getReadyNodes(); // DAG: nodes with in-degree 0
}
```

### 2.2 Workflow Graph (DAG)

**File:** `ai-agent-domain/workflow/entity/WorkflowGraph.java`

```java
public class WorkflowGraph {
    private Map<String, Node> nodes;
    private Map<String, Set<String>> edges; // source -> targets
    private Map<String, List<Edge>> edgeDetails;

    public List<String> topologicalSort();
    public boolean hasCycle();
}
```

### 2.3 Scheduler Service — Node Scheduling

**File:** `ai-agent-application/workflow/SchedulerService.java`

```java
private void scheduleNode(String executionId, Node node, String parentId) {
    // 1. Check BEFORE_EXECUTION pause
    if (checkPause(executionId, node, TriggerPhase.BEFORE_EXECUTION, ...)) return;

    // 2. Publish start event via SSE
    streamPublisher.publishStart();

    // 3. Get executor strategy by node type
    NodeExecutorStrategy strategy = executorFactory.getStrategy(node.getType());

    // 4. Execute asynchronously
    CompletableFuture<NodeExecutionResult> future = strategy.executeAsync(...);

    // 5. On completion, advance DAG
    future.whenComplete((result, error) -> {
        onNodeComplete(executionId, node.getNodeId(), ...);
    });
}
```

### 2.4 Node Executor Strategy Pattern (Registry)

**Port Interface:**
```java
// ai-agent-domain/workflow/port/NodeExecutorStrategy.java
public interface NodeExecutorStrategy {
    CompletableFuture<NodeExecutionResult> executeAsync(
        Node node, Map<String, Object> resolvedInputs, StreamPublisher streamPublisher);
    NodeType getSupportedType();
    default boolean supportsStreaming() { return false; }
}
```

**Factory (Registry Pattern):**
```java
// ai-agent-infrastructure/workflow/executor/NodeExecutorFactory.java
public class NodeExecutorFactory {
    private final Map<NodeType, NodeExecutorStrategy> strategyRegistry = new HashMap<>();
    public NodeExecutorStrategy getStrategy(NodeType nodeType) { ... }
}
```

**Available Executors:**

| Executor | File | Purpose |
|----------|------|---------|
| `StartNodeExecutorStrategy` | infrastructure | Start node |
| `EndNodeExecutorStrategy` | infrastructure | End node |
| `LlmNodeExecutorStrategy` | infrastructure | LLM calls with RAG, LTM, STM |
| `ConditionNodeExecutorStrategy` | infrastructure | Conditional routing (EXPRESSION or LLM mode) |
| `HttpNodeExecutorStrategy` | infrastructure | HTTP calls |
| `ToolNodeExecutorStrategy` | infrastructure | MCP tool calls |
| `KnowledgeNodeExecutorStrategy` | infrastructure | Knowledge retrieval |

---

## 3. Streaming Architecture: Redis Pub/Sub + SSE

### 3.1 Stream Publisher Port

```java
// ai-agent-domain/workflow/port/StreamPublisher.java
public interface StreamPublisher {
    void publishStart();
    void publishDelta(String delta);
    void publishDelta(String delta, boolean isThought);
    void publishThought(String thought);
    void publishFinish(NodeExecutionResult result);
    void publishError(String errorMessage);
    void publishData(Object data, String renderMode);
    void publishEvent(String eventType, Map<String, Object> payload);
}
```

### 3.2 Redis SSE Publisher

```java
// ai-agent-infrastructure/workflow/event/RedisSsePublisher.java
public class RedisSsePublisher {
    private static final String CHANNEL_PREFIX = "workflow:channel:";

    public void publish(SseEventPayload payload) {
        String channel = CHANNEL_PREFIX + payload.getExecutionId();
        redisService.publish(channel, message);
    }
}
```

### 3.3 SSE Event Payload

```java
// ai-agent-domain/chat/valobj/SseEventPayload.java
public class SseEventPayload {
    private String executionId;
    private String nodeId;
    private String nodeType;
    private String parentId;
    private SseEventType eventType; // START, UPDATE, FINISH, ERROR
    private ExecutionStatus status;
    private long timestamp;
    private ContentPayload payload;
}
```

### 3.4 SSE Controller

```java
// ai-agent-interfaces/workflow/WorkflowController.java
@GetMapping(value = "/{executionId}/stream", produces = "text/event-stream;charset=UTF-8")
public SseEmitter streamExecution(@PathVariable String executionId) {
    SseEmitter emitter = new SseEmitter(SSE_TIMEOUT); // 30 min timeout

    RedisSseListener listener = new RedisSseListener(payload -> {
        emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
    });

    redisMessageListenerContainer.addMessageListener(listener, new ChannelTopic(channel));
    return emitter;
}
```

---

## 4. Human Review / Pause Points

### 4.1 Trigger Phase

```java
// ai-agent-domain/workflow/valobj/TriggerPhase.java
public enum TriggerPhase {
    BEFORE_EXECUTION, // Pause before execution, allows input modification
    AFTER_EXECUTION   // Pause after execution, allows output modification
}
```

### 4.2 Pause Check Flow

```java
// SchedulerService.java (lines 893-998)
private boolean checkPause(String executionId, Node node, TriggerPhase phase, ...) {
    if (!node.requiresHumanReview()) return false;
    if (execution.isNodeReviewed(node.getNodeId())) return false;

    execution.advance(node.getNodeId(), NodeExecutionResult.paused(phase, outputs));
    checkpointRepository.save(execution.createCheckpoint(node.getNodeId()));
    humanReviewQueuePort.addToPendingQueue(executionId);
    return true;
}
```

### 4.3 Resume Execution

```java
// SchedulerService.java
public void resumeExecution(String executionId, String nodeId, Integer expectedVersion,
                           Map<String, Object> edits, Long reviewerId, String comment, ...) {
    // Merge edits into context based on trigger phase
    HumanReviewRecord record = HumanReviewRecord.builder()
        .executionId(executionId).nodeId(nodeId).reviewerId(reviewerId)
        .decision("APPROVE").triggerPhase(pausedPhase)
        .modifiedData(serializeToJson(edits)).comment(comment).build();
    humanReviewRepository.save(record);

    List<Node> readyNodes = execution.resume(nodeId, edits);
    humanReviewQueuePort.removeFromPendingQueue(executionId);
}
```

---

## 5. Concurrency Patterns

### 5.1 Node Executor Thread Pool

```java
// ai-agent-infrastructure/config/WorkflowConfig.java
@Bean("nodeExecutorThreadPool")
public Executor nodeExecutorThreadPool() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(20);
    executor.setMaxPoolSize(100);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("wf-exec-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
}
```

Usage in executors:
```java
public LlmNodeExecutorStrategy(@Qualifier("nodeExecutorThreadPool") Executor executor) {
    this.executor = executor;
}

public CompletableFuture<NodeExecutionResult> executeAsync(...) {
    return CompletableFuture.supplyAsync(() -> { ... }, executor);
}
```

### 5.2 Distributed Locking (Redisson)

```java
// SchedulerService.java
String lockKey = "lock:exec:" + executionId;
RLock lock = redisService.getLock(lockKey);
try {
    lock.lock(30, TimeUnit.SECONDS);
    // Critical section
} finally {
    lock.unlock();
}
```

---

## 6. Persistence & State

### 6.1 Execution Repository (Redis)

```java
// ai-agent-infrastructure/workflow/repository/RedisExecutionRepository.java
public class RedisExecutionRepository implements ExecutionRepository {
    private static final String KEY_PREFIX = "workflow:execution:";
    private static final long TTL_HOURS = 48;

    public void save(Execution execution) {
        redisService.setString(key, value, TTL_HOURS, TimeUnit.HOURS);
        // Secondary index by conversation
        String indexKey = "workflow:conversation:" + conversationId + ":executions";
        redisService.addToSet(indexKey, execution.getExecutionId());
    }

    public void update(Execution execution) {
        // Optimistic locking via version check
        if (!existing.getVersion().equals(execution.getVersion() - 1)) {
            throw new OptimisticLockingFailureException("Version mismatch");
        }
    }
}
```

### 6.2 Human Review Queue (Redis Set)

```java
// ai-agent-domain/workflow/port/HumanReviewQueuePort.java
public interface HumanReviewQueuePort {
    void addToPendingQueue(String executionId);
    void removeFromPendingQueue(String executionId);
    boolean isInPendingQueue(String executionId);
    Set<String> getPendingExecutionIds();
}

// ai-agent-infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java
public class RedisHumanReviewQueueAdapter implements HumanReviewQueuePort {
    private static final String PENDING_QUEUE_KEY = "human_review:pending";
}
```

---

## 7. Comparison: Claude-Code vs Java Backend

| Dimension | Claude-Code | Java Backend |
|-----------|-------------|--------------|
| **Agent Model** | CLI subprocess (tmux/iTerm2/in-process) | Virtual Thread + LLM loop |
| **Message Passing** | File-based mailbox (JSON, file locks) | In-memory + Redis Pub/Sub |
| **Coordination** | Coordinator (star) + task list | SchedulerService (DAG executor) |
| **Persistence** | Filesystem task lists | Redis + MySQL |
| **Streaming** | N/A (CLI output) | Redis Pub/Sub + SSE |
| **Concurrency** | OS-level (tmux panes) | ThreadPoolTaskExecutor + Virtual Threads |
| **Human Review** | N/A | BEFORE_EXECUTION/AFTER_EXECUTION pause |
| **Task Model** | Shared task list (blocked/blocks) | DAG node execution + human review queue |
| **Agent Spawn** | `spawnMultiAgent` (tmux pane) | `SwarmTools.createAgent` (DB + runtime) |
| **Delegation** | `SendMessage` tool | `send` tool (SwarmMessage → wakeAgent) |

---

## 8. Key Design Patterns Summary

| Pattern | Implementation |
|---------|---------------|
| **Inter-Agent Communication** | P2P messaging via SwarmGroups, event-driven wake notifications |
| **Workflow Engine** | DAG-based SchedulerService, NodeExecutorStrategy (Strategy pattern) |
| **Streaming/SSE** | Redis Pub/Sub + Spring SSE Emitter, dual bus (Agent-level + UI-level) |
| **Message Queue** | Redis Set for human review queue, Spring Application Events for async |
| **Human Review** | BEFORE_EXECUTION/AFTER_EXECUTION pause points, audit logging |
| **Concurrency** | ThreadPoolTaskExecutor for nodes, Virtual Threads for swarm agents |
| **Agent Spawning** | SwarmTools.createAgent(), delegation via send(), blocking executeWorkflow() |
| **Distributed Lock** | Redisson RLock for execution state mutations |
| **Persistence** | Redis with 48h TTL, optimistic locking via version field |
| **Event-Driven** | Spring `@Async` + `@EventListener` for non-blocking notifications |
