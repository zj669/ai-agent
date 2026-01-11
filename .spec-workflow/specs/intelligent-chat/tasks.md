# Tasks Document

- [x] 1. Define Data Models and Persistence (Async Audit)
  - File: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/listener/WorkflowAuditListener.java
  - Define `WorkflowNodeExecutionLog` entity (fields: inputs, outputs, renderMode, executionId, nodeId)
  - Create repository interface `WorkflowNodeExecutionLogRepository`
  - Implement class `WorkflowAuditListener` annotated with `@Component`
  - Use `@Async` and `@EventListener` to handle `NodeCompletedEvent` and save logs without blocking
  - Purpose: Persist detailed execution logs for debug and history asynchronously
  - _Requirements: 2.3, 4.1_
  - _Prompt: Role: Java Backend Developer | Task: Implement persistence for workflow logs. 1) Create `WorkflowNodeExecutionLog` entity (fields: inputs, outputs, renderMode, executionId, nodeId). 2) Create Repository. 3) Implement a class `WorkflowAuditListener` annotated with `@Component`. It must use `@Async` and `@EventListener` to handle `NodeCompletedEvent` and save the log to the repository without blocking the main thread._

- [x] 2. Implement Redis SSE Publisher and Subscriber
  - File: ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/dto/SseEventPayload.java
  - Define `SseEventPayload` class with fields: `boolean isThought`, `String content`, `RenderConfig renderConfig`, `long timestamp`
  - Implement `RedisSsePublisher` to serialize and publish payload to `workflow:channel:{executionId}`
  - Implement `RedisSseListener` to deserialize and handle messages
  - Purpose: Enable distributed event routing with structured payload
  - _Requirements: 2.1, 3.0_
  - _Prompt: Role: Java Spring Developer | Task: Implement Redis Pub/Sub for SSE. 1) Define `SseEventPayload` class with fields: `boolean isThought`, `String content`, `RenderConfig renderConfig`, `long timestamp`. 2) Implement `RedisSsePublisher` to serialize and publish this payload to channel `workflow:channel:{executionId}`. 3) Implement `RedisSseListener` to deserialize and handle messages._

- [x] 3. Refactor WorkflowController for Direct POST Streaming
  - File: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java
  - Create and return `SseEmitter` (timeout=30m) immediately
  - Asynchronously trigger `schedulerService.startExecution` (e.g., using `CompletableFuture.runAsync`)
  - Subscribe the Emitter to the `RedisSseListener`
  - Implement a heartbeat mechanism (e.g., a Scheduled task) that sends a "ping" event every 15s
  - Purpose: Fix race conditions, support real-time streaming and keep-alive
  - _Requirements: 2.1, 4.1, 4.2_
  - _Prompt: Role: Java Spring WebFlow Developer | Task: Refactor `startExecution` endpoint. 1) Create and return `SseEmitter` (timeout=30m) immediately. 2) Asynchronously trigger `schedulerService.startExecution` (e.g., using `CompletableFuture.runAsync`). 3) Subscribe the Emitter to the `RedisSseListener`. 4) Implement a heartbeat mechanism (e.g., a Scheduled task) that sends a "ping" event to the emitter every 15 seconds to prevent timeout._

- [x] 4. Implement Scheduler Integration
  - File: ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java
  - Publish `SseEventPayload` via `RedisSsePublisher` when node starts/completes/streams
  - Ensure `isThought` is set correctly based on the node type or render config
  - Check cancellation flag (Redis/DB) before executing a node
  - Purpose: Connect core scheduler execution with the Event Stream and Control
  - _Requirements: 2.1, 4.2_
  - _Prompt: Role: Backend Logic Developer | Task: Update `SchedulerService`. 1) When a node starts/completes/streams, publish `SseEventPayload` via `RedisSsePublisher`. Ensure `isThought` is set correctly based on the node type or render config. 2) Before executing a node, check if the execution status is `CANCELLED` (check Redis/DB) and stop if true._

- [x] 5. Implement History and Debug APIs
  - File: ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java
  - Implement `GET /history/{conversationId}`
  - Implement `GET /execution/{executionId}/node/{nodeId}`
  - Purpose: Provide data for Chat UI history and Debug sidepanel
  - _Requirements: 2.2, 2.3_
  - _Prompt: Role: API Developer | Task: Implement endpoints for fetching conversation history and node execution details from repository. | Success: APIs return correct JSON structure defined in design_
