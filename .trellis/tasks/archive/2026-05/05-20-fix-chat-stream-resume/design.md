# Design: Chat stream resume after navigation

## Boundary

The fix is frontend-owned unless implementation reveals that persisted message metadata is not returned correctly. The backend already persists `metadata.runId` on assistant messages and exposes `MessageResponse.metadata`.

## Data Flow

```text
SchedulerService.initAssistantMessage
  -> Message.metadata.runId = executionId
  -> GET /api/chat/conversations/{conversationId}/messages
  -> MessageDTO.metadata
  -> ChatMessage.metadata.runId
  -> ChatPage detects pending/streaming assistant message
  -> resumeChatStream(runId)
  -> existing SSE handlers append deltas/thoughts to the same message
```

## Runtime Behavior

1. Preserve the current optimistic send path.
2. Carry message metadata through the frontend adapter and `ChatMessage`.
3. Persist the last selected agent and conversation in `sessionStorage`, scoped to the browser tab.
4. On chat page remount, restore the last selected agent after the agent list loads, then restore the last selected conversation if it still belongs to that agent.
5. After history is loaded for a conversation, find the latest assistant message with status `PENDING` or `STREAMING` and a string `metadata.runId`.
6. If no stream is already active, call the existing `startResumedStream(executionId, assistantMessageId)` helper.
7. Reuse the existing resume handlers so deltas, thought steps, node status, pause, error, and finish behavior stay consistent with review resume.
8. Guard against duplicate reconnects by tracking the active/resumed execution id in a ref.
9. When a resumed stream is already terminal and emits an immediate finish with no deltas, rely on the existing `finishMsg` refresh path to reload persisted messages.

## Compatibility

- No REST route changes.
- No backend SSE event changes.
- No database changes.
- Existing persisted messages without metadata are ignored by reconnect detection.
- A conversation with multiple unfinished assistant messages reconnects only to the latest one, matching the UI's single in-flight stream model.

## Risks

- If the component unmounts during an active `fetch` stream, aborting the request must not call stop/cancel. Current cleanup aborts only the browser request, so the backend workflow continues.
- The reconnect should not fire while the user is actively sending a new message or while a review-resume stream is active.
- React effects must avoid repeated reconnect loops after every message state update.
