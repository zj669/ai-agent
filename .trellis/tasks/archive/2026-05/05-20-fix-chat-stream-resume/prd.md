# Fix chat stream resume after navigation

## Goal

Fix the chat page so an in-progress AI response can continue displaying after the user navigates away from the chat route and then returns.

The user-visible failure today: while the AI is replying, switching to another page and returning to the conversation can leave the chat content blank/white instead of continuing or recovering the assistant response.

## Requirements

- Preserve the current chat workflow execution model: the chat page starts `POST /api/workflow/execution/start` and receives SSE events from workflow execution.
- When the chat page remounts or a conversation is selected, detect in-progress assistant messages loaded from history.
- When returning to the chat page in the same browser tab, restore the last selected agent/conversation when they still exist.
- Reconnect to the existing workflow stream instead of starting a duplicate execution.
- Use the execution id already stored on assistant message metadata (`metadata.runId`) as the stream id.
- Continue rendering streaming deltas, thought steps, node status, finish, error, and pause events using the existing chat UI behavior.
- Avoid duplicate assistant messages when reconnecting to an existing message.
- Do not stop the backend workflow merely because the user navigates away from the chat page.
- Keep the change focused on the chat page/SSE recovery path unless code evidence shows a backend contract is missing.

## Acceptance Criteria

- [ ] If the user sends a message, navigates away while the AI is still responding, and returns to the same conversation, the chat page reconnects to the existing execution stream and continues displaying the assistant response.
- [ ] Returning to `/chat` in the same tab restores the last selected conversation before attempting stream recovery.
- [ ] The recovered stream appends deltas to the existing pending/streaming assistant message instead of creating a duplicate visible assistant bubble.
- [ ] If the backend execution has already reached a terminal state while the user was away, the UI does not stay blank and refreshes/finalizes the message state from history.
- [ ] Existing send, stop, error, pause-for-review, resume-after-review, and normal finish behavior remains intact.
- [ ] Targeted frontend tests cover remount/reload recovery from a pending assistant message with `metadata.runId`.

## Notes

- Confirmed code facts:
  - The current chat page uses `startChatStream()` to call `POST /api/workflow/execution/start`, not `ChatController.sendMessage`.
  - `Message.initAssistant()` stores the workflow execution id as `metadata.runId`.
  - `MessageResponse` already exposes `metadata`, but the frontend `MessageDTO` and `ChatMessage` types currently do not carry it through.
  - `resumeChatStream(executionId, ...)` already exists and is used after human-review resume, so the reconnect primitive can be reused.
