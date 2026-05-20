# Cross-Layer Spec Index

This directory records executable contracts that cross frontend, interface,
application, domain, infrastructure, and persistence boundaries.

The source of truth is the current codebase, not the desired architecture.

## Files

- `workflow-execution-flow.md` documents workflow execution from a UI request
  through scheduling, node execution, Redis-backed SSE, persistence, and
  terminal chat finalization.
- `chat-streaming-flow.md` documents chat conversation creation, message
  persistence, workflow-backed streaming, and the legacy direct chat stream.
- `knowledge-retrieval-flow.md` documents long-term memory hydration,
  knowledge node retrieval, LLM RAG prompt enrichment, Milvus adapters, and
  disabled-vector-store behavior.
- `auth-flow.md` documents login, token creation, frontend token storage,
  request interception, backend authentication, and SSE authentication.
- `human-review-flow.md` documents before/after node pause gates, Redis review
  queueing, review detail retrieval, approve/reject, and resumed streams.
- `conditional-branching-flow.md` documents condition authoring, backend graph
  payloads, structured condition evaluation, LLM routing, and branch pruning.
- `event-contracts.md` documents REST and SSE event shapes shared across
  backend and frontend.

## Shared Rules

1. Prefer code-backed flow references over broad architectural statements.
2. Treat a flow as cross-layer only when at least three boundaries are involved.
3. Record both current contracts and current gaps when implementation diverges
   from expected product language.
4. Keep event names and REST paths exactly as implemented.
5. Do not invent a unified response wrapper for endpoints that return raw DTOs.
6. Do not document frontend execution controls that are not present in code.
7. Use `file:line` references so future changes can be audited quickly.
8. Use this directory before changing any API shape, SSE payload, node status,
   review contract, auth interceptor behavior, or frontend runtime state model.

## Layer Map

- Frontend modules live under `ai-agent-foward/src/modules/*`.
- Shared frontend HTTP behavior lives under `ai-agent-foward/src/app`.
- Interface controllers live under `ai-agent-interfaces/src/main/java`.
- Application orchestration lives under `ai-agent-application/src/main/java`.
- Domain aggregates and value objects live under `ai-agent-domain/src/main/java`.
- Infrastructure adapters live under `ai-agent-infrastructure/src/main/java`.
- Shared response types live under `ai-agent-shared/src/main/java`.

## High-Value Entry Points

- Workflow execution interface:
  `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:51`.
- Workflow scheduler:
  `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:240`.
- Workflow aggregate:
  `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:115`.
- Workflow Redis SSE publisher:
  `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:27`.
- Chat page runtime stream:
  `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1241`.
- Chat stream service:
  `ai-agent-foward/src/modules/chat/api/chatService.ts:327`.
- Human review controller:
  `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:41`.
- Condition runtime executor:
  `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:75`.
- Auth controller:
  `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:43`.
- Login interceptor:
  `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/common/interceptor/LoginInterceptor.java:33`.
- Milvus adapter:
  `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/memory/MilvusVectorStoreAdapter.java:52`.

## Current Cross-Layer Gotchas

1. Workflow editor screens currently save and publish graph definitions, but do
   not expose a runtime execution button. Runtime execution starts from the chat
   module through `startChatStream`.
2. Backend SSE event names are `connected`, `ping`, `start`, `update`,
   `finish`, and `error`. Domain-specific events such as `workflow_paused` are
   wrapped as `update` events with `payload.renderMode = "JSON_EVENT"`.
3. Review endpoints are not uniformly wrapped in `Response<T>`. Pending review,
   review detail, resume, and reject currently return raw DTOs or empty
   `ResponseEntity` values.
4. The current LLM node implementation constructs Spring AI `ChatClient` and
   `OpenAiChatModel` inline. There is no active `ChatModelPort` or
   `OpenAiChatModelAdapter` contract in this flow.
5. Frontend chat runtime handles `PAUSED_FOR_REVIEW`, `PENDING`, and
   `SUCCEEDED` status labels. It does not currently expose a dedicated
   `SKIPPED` visual contract.
6. Conditional branch selection stores the selected target node id in
   `NodeExecutionResult.selectedBranchId`, and the aggregate prunes successors
   whose node id does not match that value.
7. `ChatController` keeps a legacy direct stream endpoint under `/api/chat`,
   but the primary frontend runtime path uses `/api/workflow/execution/start`.
8. Auth-protected SSE requests are supported by both `Authorization: Bearer`
   and query parameter `token`; the custom fetch-based frontend stream uses the
   header path.

## Contract Change Checklist

1. If changing REST route paths, update frontend adapters and this directory.
2. If changing `SseEventPayload`, update both `event-contracts.md` and
   `chatService.ts` parser assumptions.
3. If changing node status enum values, update backend DTOs and frontend status
   renderers in chat/review/workflow modules.
4. If changing human review response shapes, update `reviewAdapter.ts`,
   `ChatPage.tsx`, and the review flow spec.
5. If changing condition branch payloads, update editor save/publish payload
   construction and runtime strategy parsing together.
6. If changing auth token names or storage, update `authAdapter.ts`,
   `app/auth.ts`, `httpClient.ts`, and `LoginInterceptor`.
7. If enabling or disabling Milvus behavior, verify both memory hydration and
   explicit knowledge retrieval behavior.
8. If changing conversation finalization, verify the scheduler path; do not rely
   on `ExecutionCompletedListener` as the main persistence path.

## Verification Notes

- GitNexus was used to identify cross-layer flows, process participation, and
  blast radius before writing this spec.
- ABCoder MCP was attempted for repository structure and AST context, but the
  tool calls were cancelled by the MCP host during this task.
- Direct file reads filled in DTO, controller, strategy, adapter, and frontend
  details after GitNexus located the relevant symbols.
- Confidence: High for documented paths and payload fields that include
  concrete file references.
- Confidence: Medium for absence statements, because they are based on targeted
  repository searches rather than an exhaustive AST index.

