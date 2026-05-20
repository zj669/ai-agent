# Workflow Execution Flow

This flow describes the current executable path for starting and observing a
workflow execution.

## Scope

- Frontend execution entry from chat runtime.
- Backend workflow start endpoint and stream reconnect endpoint.
- Scheduler orchestration, memory hydration, node scheduling, and persistence.
- Redis-backed SSE publishing and browser stream consumption.
- Terminal assistant message finalization for chat-backed executions.

## Current Frontend Entry Reality

1. The workflow editor currently exposes save and publish controls, not a run
   control.
2. `EditorHeader` accepts `onSave` and `onPublish` props only:
   `ai-agent-foward/src/modules/workflow/components/EditorHeader.tsx:4`.
3. `WorkflowEditorPage` saves graph payloads through `handleSave`:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:1239`.
4. `WorkflowEditorPage` publishes versions through `handlePublish`:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:1295`.
5. The active runtime execution request is created by chat code calling
   `startChatStream`: `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1241`.
6. `startChatStream` posts to `/api/workflow/execution/start`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:327`.

## Sequence

1. The chat page creates or selects a conversation before execution:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1201`.
2. The chat page appends a local user message and a local streaming assistant
   message: `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1216`.
3. The chat page calls `startChatStream` with `agentId`, `conversationId`,
   `userId`, `versionId`, and user input:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1241`.
4. `startChatStream` builds a JSON body with `agentId`, `userId`,
   `conversationId`, `versionId`, `inputs.inputMessage`, and `mode`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:342`.
5. `startChatStream` sends `Authorization: Bearer <token>` when a token exists:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:336`.
6. `startChatStream` also sends `debug-user` from the request input:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:335`.
7. `WorkflowController.startExecution` handles
   `POST /api/workflow/execution/start` and produces `text/event-stream`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:51`.
8. The controller creates an execution id before scheduler work begins:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:62`.
9. The controller creates an emitter and sends a `connected` event immediately:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:64`.
10. The controller starts scheduler work asynchronously:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:66`.
11. The request DTO contains `agentId`, `userId`, `conversationId`,
    `versionId`, `inputs`, and `mode`:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:443`.
12. `SchedulerService.startExecution` loads the agent and graph version:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:80`.
13. The scheduler parses graph data into a `WorkflowGraph`:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:135`.
14. The scheduler builds an `Execution` aggregate with agent, user,
    conversation, version, and graph:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:137`.
15. The private scheduler start path persists the user message when both
    `conversationId` and input text exist:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:251`.
16. The scheduler initializes an assistant message and stores the assistant
    message id on the execution:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:285`.
17. The scheduler hydrates memory before starting the aggregate:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:293`.
18. `hydrateMemory` searches vector memory for the extracted user query:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:315`.
19. `hydrateMemory` stores returned memory snippets on execution context:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:328`.
20. `hydrateMemory` loads recent conversation messages when `conversationId`
    exists: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:342`.
21. `hydrateMemory` stores recent messages as chat history on context:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:360`.
22. `Execution.start` stores initial inputs, marks nodes pending, sets status
    running, and returns root nodes:
    `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:115`.
23. The scheduler saves the new execution after the aggregate starts:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:299`.
24. The scheduler schedules each root node:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:302`.
25. `scheduleNode` builds `StreamContext` with execution, node, node type,
    and optional parent id:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:803`.
26. `scheduleNode` creates a node-level stream publisher:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:814`.
27. `scheduleNode` checks before-execution human review before publishing node
    start: `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:818`.
28. `scheduleNode` publishes a node `start` event:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:831`.
29. `scheduleNode` resolves input expressions and injects `__context__`,
    `__agentId__`, and `__outgoingEdges__`:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:843`.
30. `scheduleNode` invokes the node strategy asynchronously:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:879`.
31. Node success or failure publishes node `finish` or `error`:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:885`.
32. If the execution was paused while the node was running, callback handling
    stops before advancing:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:901`.
33. Completed node results are passed to `onNodeComplete`:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:930`.
34. `onNodeComplete` reloads the execution and node before mutating state:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1146`.
35. `onNodeComplete` checks after-execution review gates before advancing:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1165`.
36. `Execution.advance` stores node output, handles routing, pause, failure,
    and completion:
    `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:176`.
37. `Execution.getReadyNodes` computes nodes with all effective predecessors
    satisfied: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:307`.
38. The scheduler saves checkpoints after node advancement:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1185`.
39. The scheduler publishes node completion events and logs:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1202`.
40. If the execution becomes terminal, the scheduler finalizes the backing
    assistant chat message:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1433`.
41. The scheduler emits an execution-level `finish` event for terminal
    executions:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1235`.
42. The frontend stream parser splits SSE chunks on blank lines:
    `ai-agent-foward/src/modules/chat/api/chatService.ts:277`.
43. The frontend handles `connected`, `start`, `update`, `finish`, and `error`
    events in `handleSseEvent`:
    `ai-agent-foward/src/modules/chat/api/chatService.ts:386`.
44. The chat page appends deltas to the local assistant message:
    `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1049`.
45. The chat page updates node status chips from node-level events:
    `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1087`.

## Backend SSE Transport

1. `WorkflowController` maps Redis payload enum names to lowercase SSE event
   names: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:167`.
2. `WorkflowController` completes the emitter when a terminal execution-level
   finish event is observed:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:178`.
3. `WorkflowController` sends heartbeat `ping` events:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:210`.
4. `RedisSseStreamPublisher.publishStart` emits `SseEventType.START` with
   status `RUNNING`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:27`.
5. `RedisSseStreamPublisher.publishDelta` emits `SseEventType.UPDATE` with
   `delta` and optional thought markers:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:33`.
6. `RedisSseStreamPublisher.publishFinish` emits `SseEventType.FINISH`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:60`.
7. `RedisSseStreamPublisher.publishError` emits `SseEventType.ERROR`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:97`.
8. `RedisSsePublisher` publishes serialized payloads to
   `workflow:channel:{executionId}`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/RedisSsePublisher.java:23`.

## Persistence and DTO Contracts

1. `ExecutionDTO` exposes `executionId`, `agentId`, `userId`,
   `conversationId`, `status`, `startTime`, `endTime`, and `nodeStatuses`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/ExecutionDTO.java:16`.
2. `ExecutionContext` stores inputs, node outputs, shared state, long-term
   memories, short-term chat history, and execution logs:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/ExecutionContext.java:34`.
3. `WorkflowGraph` stores nodes, edge map, and edge details:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/WorkflowGraph.java:36`.
4. `NodeExecutionResult` carries `status`, `outputs`, `errorMessage`,
   `selectedBranchId`, and `triggerPhase`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java:22`.

## Gotchas

1. Do not document workflow editor execution as implemented unless a runtime
   control is added to the editor.
2. The scheduler, not `ExecutionCompletedListener`, is the authoritative path
   for final assistant message persistence.
3. Memory hydration failures are logged and do not fail execution.
4. Custom workflow events are wrapped as SSE `update` events.
5. Node `finish` events and execution-level `finish` events share the same SSE
   event name; clients distinguish them by payload fields and status.
6. `PAUSED_FOR_REVIEW` can be reached before node execution or after node
   execution, depending on node configuration.
7. `Execution.getReadyNodes` treats `SUCCEEDED`, `SKIPPED`, and `FAILED`
   predecessors as effective completions for dependency readiness.
8. Any contract change to SSE event names must update both controller mapping
   and `chatService.ts` event handling together.

