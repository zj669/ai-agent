# Human Review Flow

This flow documents workflow pause, review queueing, review detail retrieval,
approval, rejection, and stream resumption across backend, Redis, and frontend
review/chat modules.

## Scope

- Before-execution and after-execution human review gates.
- Review queue persistence in Redis.
- Checkpoint persistence for paused executions.
- Review list and detail pages.
- Chat modal review and resume stream behavior.
- Approval and rejection contracts.

## Pause Creation Sequence

1. Every scheduled node checks before-execution review before emitting node
   start:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:818`.
2. Completed nodes check after-execution review before aggregate advancement:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1165`.
3. `checkPause` is the shared pause gate:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:946`.
4. `checkPause` exits early if the node does not require human review:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:953`.
5. `checkPause` skips nodes already reviewed in the current execution:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:957`.
6. `checkPause` reads the node-level review phase configuration:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:967`.
7. `checkPause` acquires a lock before mutating pause state:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:982`.
8. For review pause, the aggregate advances with `NodeExecutionResult.paused`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1002`.
9. The scheduler saves a checkpoint after pause state is set:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1008`.
10. The scheduler persists the paused execution:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1012`.
11. The scheduler emits `workflow_paused` through the stream publisher:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1014`.
12. The scheduler adds the execution id to the human review queue:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1022`.
13. The scheduler finalizes the assistant message with a pause notice when a
    chat message is attached:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:1025`.

## Domain Pause State

1. `Execution.advance` stores pause status on the node:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:190`.
2. `Execution.advance` sets execution status to `PAUSED_FOR_REVIEW`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:193`.
3. `Execution.advance` stores `pausedNodeId` and `pausedPhase`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:194`.
4. `NodeExecutionResult.paused` carries trigger phase and optional outputs:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java:77`.
5. `Node.requiresHumanReview` is checked by scheduler pause logic:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Node.java:79`.
6. `NodeConfig.requiresHumanReview` is the config-level flag:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/NodeConfig.java:46`.

## Queue and Checkpoint Persistence

1. `RedisHumanReviewQueueAdapter` stores pending execution ids in Redis set
   `human_review:pending`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java:12`.
2. The adapter adds execution ids to the set:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java:23`.
3. The adapter removes execution ids after resume or reject:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java:35`.
4. The adapter lists pending execution ids for review pages:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/adapter/RedisHumanReviewQueueAdapter.java:57`.
5. `RedisCheckpointRepository` stores checkpoints under
   `workflow:checkpoint:{executionId}:{version}`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java:21`.
6. Pause points are indexed under `workflow:pause:{executionId}`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java:34`.
7. Checkpoints and pause points use a 24-hour TTL:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java:26`.

## Review REST Contracts

1. `HumanReviewController` is mapped under `/api/workflow/reviews`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:27`.
2. `GET /api/workflow/reviews/pending` lists pending reviews:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:41`.
3. Pending review DTO includes `executionId`, `agentId`, `agentName`,
   `nodeId`, `nodeName`, `nodeType`, `phase`, `pausedAt`,
   `executionVersion`, and `userId`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/HumanReviewDTO.java:14`.
4. `GET /api/workflow/reviews/{executionId}` loads review detail:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:80`.
5. Review detail rejects executions that are not paused for review:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:91`.
6. Review detail rejects manual pause without a paused node id:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:103`.
7. Review detail resolves current inputs and upstream node outputs:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:123`.
8. Review detail DTO includes execution, node, phase, version, current inputs,
   current outputs, upstream nodes, and context:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/HumanReviewDTO.java:27`.
9. `POST /api/workflow/reviews/resume` approves and resumes execution:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:200`.
10. Resume request includes `executionId`, `nodeId`, `expectedVersion`,
    `edits`, `comment`, and `nodeEdits`:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/HumanReviewDTO.java:54`.
11. `POST /api/workflow/reviews/reject` rejects execution:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:224`.
12. Reject request includes `executionId`, `nodeId`, `expectedVersion`,
    `reason`, and `comment`:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/HumanReviewDTO.java:66`.
13. Review history returns a shared `Response<T>` wrapper:
    `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:246`.
14. Pending, detail, resume, and reject return raw DTOs or empty
    `ResponseEntity`, not shared `Response<T>`.

## Approve Sequence

1. The review page calls `resumeReview` with execution id, node id,
   expected version, and edits:
   `ai-agent-foward/src/modules/review/pages/ReviewPage.tsx:61`.
2. The review detail page can submit `nodeEdits` as structured edits:
   `ai-agent-foward/src/modules/review/pages/ReviewDetailPage.tsx:185`.
3. `reviewAdapter.resumeReview` posts to `/api/workflow/reviews/resume`:
   `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts:61`.
4. `HumanReviewController.resume` delegates to
   `SchedulerService.resumeExecution`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:205`.
5. The scheduler validates cancellation state, lock ownership, paused node, and
   expected version:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:400`.
6. The scheduler merges after-execution edits and node edits when supplied:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:450`.
7. The scheduler saves a human review record with approval:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:484`.
8. The aggregate resumes and increments execution version:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:222`.
9. The scheduler publishes `workflow_resumed`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:505`.
10. The scheduler removes the execution id from the review queue:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:519`.
11. For after-execution approval, the scheduler continues with
    `onNodeComplete`:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:523`.
12. For before-execution approval, the scheduler schedules the paused node:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:546`.

## Reject Sequence

1. The review page calls `rejectReview` with reason and expected version:
   `ai-agent-foward/src/modules/review/pages/ReviewPage.tsx:78`.
2. `reviewAdapter.rejectReview` posts to `/api/workflow/reviews/reject`:
   `ai-agent-foward/src/shared/api/adapters/reviewAdapter.ts:75`.
3. `HumanReviewController.reject` delegates to
   `SchedulerService.rejectExecution`:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/HumanReviewController.java:229`.
4. The scheduler validates paused state and expected version:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:560`.
5. The scheduler saves a human review rejection record:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:632`.
6. The aggregate marks the paused node failed and execution failed:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:277`.
7. The scheduler publishes `workflow_rejected`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:651`.
8. The scheduler publishes terminal failed finish:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:665`.
9. The scheduler removes the execution from the review queue:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:679`.
10. The scheduler finalizes the assistant message as failed:
    `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:681`.

## Chat Pause Handling

1. `chatService.ts` detects `workflow_paused` inside JSON-event update payloads:
   `ai-agent-foward/src/modules/chat/api/chatService.ts:407`.
2. `ChatPage.handlePaused` fetches review detail and opens the review modal:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1331`.
3. `handleResumeExecution` posts approval and then opens a resumed stream:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1481`.
4. `handleRejectExecution` posts rejection and marks the stream as failed:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:1518`.

## Gotchas

1. `workflow_paused`, `workflow_resumed`, and `workflow_rejected` are custom
   payload titles inside SSE `update` events, not native SSE event names.
2. `expectedVersion` is part of approve and reject contracts and protects
   against stale review actions.
3. Manual pause through `/api/workflow/execution/pause` is not the same as a
   review pause and may not have a paused node id for review detail.
4. Pending/detail/resume/reject endpoints are not wrapped in `Response<T>`.
5. After-execution review can edit result outputs before downstream nodes run.
6. Before-execution review resumes by scheduling the paused node itself.
7. Approval records and rejection records are persisted separately from Redis
   queue state.
8. Review queue Redis TTL behavior is not visible in the queue adapter; queue
   entries are removed explicitly on resume or reject.
