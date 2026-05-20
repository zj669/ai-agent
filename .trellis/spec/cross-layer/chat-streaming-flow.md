# Chat Streaming Flow

This flow documents conversation-backed chat streaming and the relationship
between chat UI, workflow execution, message persistence, and SSE updates.

## Scope

- Conversation creation and message listing.
- Primary frontend stream path through workflow execution.
- Legacy direct chat controller stream path.
- Assistant message initialization and finalization.
- Frontend stream parsing, local message state, and resume-after-review stream.

## Primary Runtime Path

1. The chat page is the current primary runtime entry for workflow-backed chat:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1241`.
2. `startChatStream` sends the execution request to
   `/api/workflow/execution/start`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:327`.
3. The request body uses `inputs.inputMessage` for the user text:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:347`.
4. The backend scheduler persists the user message before execution starts:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:251`.
5. The backend scheduler initializes the assistant message and records the
   assistant message id on the execution:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:285`.
6. The scheduler finalizes that assistant message on terminal execution:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1433`.
7. The frontend also creates local optimistic user and assistant messages for
   immediate UI feedback:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1216`.

## Conversation REST Contracts

1. `ChatController` is mapped under `/api/chat`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:42`.
2. `POST /api/chat/conversations` creates a conversation:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:59`.
3. Conversation creation uses the authenticated user from `UserContext` and
   accepts `agentId` as a request parameter:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:61`.
4. The frontend adapter currently sends both `userId` and `agentId` params,
   but the backend method only binds `agentId`:
   `ai-agent-foward/src/shared/api/adapters/chatAdapter.ts:77`.
5. `GET /api/chat/conversations` lists conversations for the authenticated
   user: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:68`.
6. `GET /api/chat/conversations/{conversationId}/messages` returns persisted
   messages: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:88`.
7. `ConversationResponse` exposes `id`, `title`, `createdAt`, and `updatedAt`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/dto/ConversationResponse.java:10`.
8. `MessageResponse` exposes `id`, `conversationId`, `role`, `content`,
   `thoughtProcess`, `citations`, `status`, `createdAt`, and `metadata`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/dto/MessageResponse.java:14`.
9. The shared `Response<T>` wrapper has `code`, `message`, `data`, and
   `success`: `ai-agent-shared/src/main/java/com/zj/aiagent/shared/response/Response.java:10`.

## Persistence Sequence

1. `ChatApplicationService.createConversation` builds and saves a conversation:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:41`.
2. `appendUserMessage` validates message length before persistence:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:61`.
3. `appendUserMessage` sanitizes user content before building the message:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:83`.
4. `appendUserMessage` saves a `USER` message with completed status:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:94`.
5. `initAssistantMessage` creates the streaming assistant message shell:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:109`.
6. `finalizeMessage` loads the message by id before updating it:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:119`.
7. `finalizeMessage` sanitizes assistant content and saves terminal status:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/chat/ChatApplicationService.java:129`.
8. `MybatisConversationRepository.saveMessage` persists message rows:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/repository/MybatisConversationRepository.java:85`.
9. `MybatisConversationRepository.findMessagesByConversationId` returns recent
   messages for display and memory hydration:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/chat/repository/MybatisConversationRepository.java:110`.

## Streaming UI Sequence

1. `startChatStream` receives handlers for deltas, thoughts, node starts,
   node finishes, pause events, and errors:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:37`.
2. `consumeExecutionStream` reads `response.body` as a stream:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:277`.
3. The parser splits chunks into SSE blocks by `\n\n`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:299`.
4. `parseSseEvent` extracts `event:` and `data:` fields:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:84`.
5. `extractDelta` accepts `payload.delta`, `data.content`, and string payload
   fallbacks:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:131`.
6. `handleSseEvent` treats `start` as a node-start event:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:396`.
7. `handleSseEvent` treats update deltas as assistant text or thought chunks:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:433`.
8. `handleSseEvent` treats `finish` as either node completion or final
   response:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:450`.
9. `ChatPage.appendDelta` appends streamed chunks to the local assistant
   message:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1049`.
10. `ChatPage.appendThought` creates or updates thought steps:
    `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1059`.
11. `ChatPage.updateNodeStatus` merges node status events into UI state:
    `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1087`.
12. If final SSE content is empty, `finishMsg` refreshes persisted messages from
    the server:
    `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1135`.

## Legacy Direct Chat Stream

1. `ChatController.sendMessage` exposes
   `POST /api/chat/conversations/{conversationId}/messages` as
   `text/event-stream`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:109`.
2. The direct chat stream validates conversation ownership first:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:115`.
3. The direct chat stream verifies that the agent is published:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:126`.
4. The direct chat stream registers a Redis listener for the generated
   execution id:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:135`.
5. The direct chat stream forwards Redis payload enum names as lowercase SSE
   event names:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:148`.
6. The direct chat stream sends heartbeats:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:163`.
7. The direct chat stream starts scheduler execution with `Map.of("input",
   request.getContent())`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:184`.
8. `SendMessageRequest` contains only `content`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/chat/ChatController.java:214`.

## LLM Streaming Contract

1. `LlmNodeExecutorStrategy` builds Spring AI model options from node config:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:91`.
2. It constructs `ChatClient` and `OpenAiChatModel` inline:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:136`.
3. It injects execution context from the scheduler under `__context__`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:153`.
4. It streams model content through `streamPublisher.publishDelta`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:185`.
5. It falls back to a non-streaming call if the streaming response is empty:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:212`.
6. It returns success outputs for downstream workflow execution:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java:229`.

## Resume-After-Review Stream

1. When a pause arrives, `ChatPage.handlePaused` fetches review detail and
   opens the review modal:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1331`.
2. `handleResumeExecution` posts the approval and then starts a resumed stream:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1481`.
3. `resumeChatStream` reconnects to
   `/api/workflow/execution/{executionId}/stream`:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:357`.
4. The workflow controller stream endpoint sends terminal state immediately if
   the execution is already terminal:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java:127`.

## Gotchas

1. The primary frontend chat path does not call `ChatController.sendMessage`;
   it calls the workflow execution endpoint directly.
2. `ExecutionCompletedListener` is not the authoritative chat finalization
   path; the scheduler finalizes the assistant message on completion.
3. The codebase does not currently contain an active `ChatModelPort` plus
   `OpenAiChatModelAdapter` implementation in this streaming path.
4. Node-level `finish` events and final assistant-message `finish` events share
   the same event name.
5. The frontend must tolerate an empty final SSE content and refresh persisted
   messages.
6. The frontend sends `debug-user` and authorization headers in custom fetch
   streams instead of using browser `EventSource`.
7. Persisted chat content is sanitized before storage for both user and
   assistant messages.
8. Direct stream and workflow stream are similar but not identical entry points;
   changes must be tested against both when both are kept public.

