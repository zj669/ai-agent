# Implementation Plan

## Checklist

- [x] Load pre-development guidance for frontend/cross-layer work.
- [x] Run GitNexus impact analysis before editing the affected frontend symbols.
- [x] Add `metadata` to the frontend `MessageDTO` and `ChatMessage` mapping.
- [x] Add a small helper to extract `metadata.runId` from assistant messages.
- [x] Persist and restore the last selected chat agent/conversation in the current browser tab.
- [x] Add a ChatPage effect that reconnects to the latest unfinished assistant message after history load.
- [x] Reuse `startResumedStream` and guard against duplicate stream starts.
- [x] Attach `runId` to optimistic assistant placeholders after stream connection.
- [x] Refresh persisted history when a blank assistant placeholder reaches a terminal state.
- [x] Reconcile execution status before resuming an existing stream.
- [x] Poll execution status for in-flight streams so missed pause events enter review state.
- [x] Add targeted tests for history-loaded pending assistant message recovery.
- [x] Add targeted test for blank failed assistant placeholder reconciliation.
- [x] Add targeted tests for `PAUSED_FOR_REVIEW` recovery during live stream and remount.
- [x] Run focused frontend tests and typecheck/lint as practical.
- [x] Run `gitnexus_detect_changes` before final report.

## Validation Commands

```bash
cd ai-agent-foward
npm test -- --run src/modules/chat/api/__tests__/chatService.test.ts src/modules/chat/__tests__/chat.page-streaming.test.tsx
npm run typecheck
```

## Rollback Points

- If frontend metadata is not returned in practice, stop and inspect backend serialization instead of adding local assumptions.
- If reconnect creates duplicate messages, keep metadata mapping but revise the effect guard before touching backend code.
