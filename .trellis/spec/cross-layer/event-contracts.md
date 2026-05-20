# Event Contracts

This file documents REST and SSE event shapes shared across workflow, chat,
review, auth, and frontend runtime modules.

## Shared Response Wrapper

1. Shared response wrapper type is `Response<T>`:
   `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java:10`.
2. The wrapper fields are `code`, `message`, `data`, and `success`:
   `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java:10`.
3. `Response.success(data)` returns code `200`, message `success`, data, and
   success flag `true`:
   `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java:16`.
4. Not every endpoint in this spec uses the shared wrapper; review pending,
   detail, resume, and reject currently return raw DTOs or empty responses.

## Workflow Start Request

1. Route: `POST /api/workflow/execution/start`.
2. Produces: `text/event-stream`.
3. Controller: `WorkflowController.startExecution`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:51`.
4. Request DTO fields are `agentId`, `userId`, `conversationId`, `versionId`,
   `inputs`, and `mode`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:443`.
5. Frontend body includes `inputs.inputMessage`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:347`.
6. The controller returns the emitter before asynchronous scheduler work
   finishes:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:99`.
7. Start errors are emitted as SSE `error` events:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:85`.

## Workflow Stream Request

1. Route: `GET /api/workflow/execution/{executionId}/stream`.
2. Produces: `text/event-stream`.
3. Controller: `WorkflowController.streamExecution`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:108`.
4. If execution is terminal, the controller sends `connected` and a terminal
   `finish` event immediately:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:127`.
5. If execution is not terminal, the controller creates a Redis-backed emitter:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:153`.
6. Frontend resume stream uses this route:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:357`.

## SSE Event Envelope

1. `SseEventPayload` fields are `executionId`, `nodeId`, `nodeType`,
   `parentId`, `eventType`, `status`, `timestamp`, and `payload`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/chat/valobj/SseEventPayload.java:18`.
2. Payload content fields are `title`, `content`, `delta`, `isThought`, and
   `renderMode`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/chat/valobj/SseEventPayload.java:77`.
3. Event enum values are `START`, `UPDATE`, `FINISH`, and `ERROR`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/SseEventType.java:7`.
4. `WorkflowController` maps enum names to lowercase event names:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:167`.
5. The emitted backend event names are therefore `start`, `update`, `finish`,
   and `error`.
6. `connected` is a controller-created event, not an enum-backed Redis payload:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:235`.
7. `ping` is a controller-created heartbeat event:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:210`.
8. The frontend parser extracts `event:` and `data:` fields:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:84`.

## SSE Event Names and Meanings

1. `connected`: sent when an emitter is created.
2. `ping`: heartbeat with current timestamp.
3. `start`: node execution started.
4. `update`: node data, content delta, thought delta, or JSON custom event.
5. `finish`: node finished or execution finished.
6. `error`: node or execution error.
7. There are no native SSE event names such as `node.started` or
   `execution.completed` in the current controller mapping.

## Node Start Event

1. Producer: `RedisSseStreamPublisher.publishStart`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:27`.
2. Event type: `START`.
3. SSE event name after controller mapping: `start`.
4. Status: `RUNNING`.
5. Payload title: node id.
6. Payload content: node type.
7. Frontend handler: `onNodeStart`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:396`.
8. Chat page state update:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1087`.

## Delta and Thought Update Event

1. Producer: `RedisSseStreamPublisher.publishDelta`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:33`.
2. Event type: `UPDATE`.
3. SSE event name: `update`.
4. Normal content chunks set `payload.delta`.
5. Thought chunks set `payload.isThought = true`.
6. Frontend delta extraction:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:131`.
7. Frontend thought branch:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:433`.
8. Chat page content append:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1049`.
9. Chat page thought append:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1059`.

## Node and Execution Finish Event

1. Producer for node finish: `RedisSseStreamPublisher.publishFinish`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:60`.
2. Producer for execution-level finish:
   `SchedulerService.onNodeComplete`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1235`.
3. Event type: `FINISH`.
4. SSE event name: `finish`.
5. Node finish payload content is extracted from `response`, `json_output`,
   `output`, or `finalResult`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:65`.
6. The controller completes the emitter on terminal execution finish:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:178`.
7. Frontend finish handler processes node finish and final response:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:450`.
8. Chat page may refresh persisted messages if final content is empty:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1135`.

## Error Event

1. Producer: `RedisSseStreamPublisher.publishError`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:97`.
2. Event type: `ERROR`.
3. SSE event name: `error`.
4. Status: `FAILED`.
5. Payload content contains the error message.
6. Frontend error handler:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:496`.
7. Start-time asynchronous errors are sent directly by the controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:85`.

## JSON Custom Workflow Events

1. Producer: `RedisSseStreamPublisher.publishEvent`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/stream/RedisSseStreamPublisher.java:126`.
2. Wrapper event type: `UPDATE`.
3. SSE event name: `update`.
4. Custom event name is stored in `payload.title`.
5. Custom event JSON is stored in `payload.content`.
6. `payload.renderMode` is `JSON_EVENT`.
7. `workflow_paused` producer:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1014`.
8. `workflow_resumed` producer:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:505`.
9. `workflow_rejected` producer:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:651`.
10. Frontend JSON custom event parser:
    `ai-agent-foward/src/modules/chat/api/chatService.ts:407`.
11. A direct frontend branch for `event.event === "workflow_paused"` exists,
    but the current backend sends pause as wrapped `update`:
    `ai-agent-foward/src/modules/chat/api/chatService.ts:501`.

## Chat Conversation Contracts

1. Route: `POST /api/chat/conversations`.
2. Controller: `ChatController.createConversation`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:59`.
3. Backend parameter: `agentId`.
4. Authenticated user id is read from `UserContext`.
5. Frontend adapter:
   `ai-agent-foward/src/shared/api/adapters/chatAdapter.ts:77`.
6. Response wrapper data is the conversation id.
7. Route: `GET /api/chat/conversations`.
8. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:68`.
9. Response data is a list of `ConversationResponse`.
10. Route: `GET /api/chat/conversations/{conversationId}/messages`.
11. Controller:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:88`.
12. Response data is a page of `MessageResponse`.
13. `ConversationResponse` fields:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/dto/ConversationResponse.java:10`.
14. `MessageResponse` fields:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/dto/MessageResponse.java:14`.

## Legacy Direct Chat Stream Contract

1. Route:
   `POST /api/chat/conversations/{conversationId}/messages`.
2. Produces: `text/event-stream`.
3. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:109`.
4. Request body field: `content`.
5. Request DTO:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:214`.
6. The controller validates ownership before streaming:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:115`.
7. The controller validates published agent state:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:126`.
8. The controller starts workflow execution with `inputs.input`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:184`.

## Review Contracts

1. Route: `GET /api/workflow/reviews/pending`.
2. Response: raw list of `PendingReviewDTO`.
3. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:41`.
4. Frontend adapter:
   `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts:33`.
5. Route: `GET /api/workflow/reviews/{executionId}`.
6. Response: raw `ReviewDetailDTO`.
7. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:80`.
8. Frontend adapter:
   `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts:42`.
9. Route: `POST /api/workflow/reviews/resume`.
10. Response: empty response entity.
11. Controller:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:200`.
12. Frontend adapter:
    `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts:61`.
13. Route: `POST /api/workflow/reviews/reject`.
14. Response: empty response entity.
15. Controller:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:224`.
16. Frontend adapter:
    `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts:75`.

## Auth Contracts

1. Route: `POST /client/user/login`.
2. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:43`.
3. Frontend adapter:
   `ai-agent-foward/src/shared/api/adapters/authAdapter.ts:35`.
4. Request fields: `email`, `password`, optional backend `deviceId`.
5. Response data fields: `token`, `refreshToken`, `expireIn`, `deviceId`,
   `user`.
6. Backend DTO:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/UserLoginResponse.java:12`.
7. Route: `POST /client/user/refresh`.
8. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/user/UserController.java:53`.
9. Refresh request fields: `refreshToken` and validated `deviceId`.
10. Refresh response fields: `accessToken`, `refreshToken`, `expiresIn`,
    `tokenType`.
11. Backend refresh DTO:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/user/dto/TokenRefreshResponse.java:15`.

## Execution DTO Contract

1. Route: `GET /api/workflow/execution/{executionId}`.
2. Controller:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:292`.
3. Response data is `ExecutionDTO`.
4. `ExecutionDTO` fields are `executionId`, `agentId`, `userId`,
   `conversationId`, `status`, `startTime`, `endTime`, and `nodeStatuses`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/ExecutionDTO.java:16`.
5. `nodeStatuses` maps node id to enum name:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/ExecutionDTO.java:38`.
6. Frontend chat API type models this as `Record<string, string>`:
   `ai-agent-foward/src/shared/api/adapters/chatAdapter.ts:66`.

## Transport Gotchas

1. Do not treat custom workflow event names as native SSE event names.
2. Do not assume all controller responses use the shared `Response<T>` shape.
3. Do not assume `finish` always means execution completed; it may be a node
   finish.
4. Do not remove `connected`; frontend code uses it to learn execution id.
5. Do not remove `ping` without replacing heartbeat behavior.
6. Do not rename `payload.delta`, `payload.isThought`, or `payload.renderMode`
   without updating `chatService.ts`.
7. Do not change review endpoint wrappers without updating `reviewAdapter.ts`.
8. Do not change token names without updating `authAdapter.ts`, `app/auth.ts`,
   and `httpClient.ts`.
9. If introducing browser `EventSource`, account for the backend query-token
   fallback or add an equivalent auth channel.
10. If adding new node statuses, update DTO consumers and runtime status maps.
