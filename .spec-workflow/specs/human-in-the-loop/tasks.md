# Tasks: Human-in-the-Loop Implementation

- [x] 1. Domain Layer: Core Enums and Entities
  - File: `ai-agent-domain/.../ExecutionStatus.java`, `TriggerPhase.java`, `HumanReviewRecord.java`
  - Ensure `RedisExecutionRepository` correctly serializes/deserializes new `Execution` fields.
  - _Leverage: Existing Repository patterns_
  - _Requirements: 4.2, Design "Data Models"_

- [x] 2. Domain Layer: Aggregate Root Updates
  - File: `ai-agent-domain/.../Execution.java`, `Node.java`
  - [x] Update `Execution` to include `String pausedNodeId` and `TriggerPhase pausedPhase`.
  - [x] Add logic to persist/restore these fields (ensure Builders/Constructors updated).
  - [x] Update `Node` or `NodeConfig` to include `HumanReviewConfig` (with `enabled` and `triggerPhase`).
  - _Leverage: Existing `Execution` logic_
  - _Requirements: 4.1, 4.3, Design Doc_

- [x] 3. Infrastructure Layer: Repository Implementation
  - File: `ai-agent-infrastructure/.../HumanReviewRepositoryImpl.java`
  - [x] Implement `HumanReviewRepository` using JPA/MyBatis.
  - [x] Ensure `RedisExecutionRepository` correctly serializes/deserializes new `Execution` fields.
  - _Leverage: Existing Repository patterns_
  - _Requirements: 4.2, Design "Data Models"_

- [x] 4. Service Layer: Pause Logic
  - File: `ai-agent-application/.../SchedulerService.java`
  - [x] Implement `checkPause(Execution, Node)` method.
  - [x] Hook into `scheduleNode` (for `BEFORE_EXECUTION`) and `onNodeComplete` (for `AFTER_EXECUTION`).
  - [x] Implement SSE `workflow_paused` event publication with payload: `{ type, executionId, nodeId, triggerPhase }`.
  - _Leverage: `RedissonClient`, `RedisSseListener`_
  - _Requirements: 5.2, 5.3_

- [x] 5. Service Layer: Resume Logic
  - File: `ai-agent-application/.../SchedulerService.java`
  - [x] Implement `resumeExecution` with Redisson Distributed Lock + Version Check (Optimistic Lock).
  - [x] Implement Context Merging Strategy:
    - [x] If `pausedPhase == BEFORE`: Update Inputs.
    - [x] If `pausedPhase == AFTER`: Update Outputs.
  - [x] Persist `HumanReviewRecord`.
  - [x] Publish `workflow_resumed` event.
  - _Requirements: 3.1, 5.2, Design "Context Merging Strategy"_

- [x] 6. Interface Layer: DTOs and Controller
  - File: `ai-agent-interfaces/.../HumanReviewController.java`
  - [x] Create `PendingReviewDTO`, `ReviewDetailDTO`, `ResumeExecutionRequest`.
  - [x] Implement `GET /pending-review` (Query Redis for active PAUSED executions).
  - [x] Implement `GET /pending-review-detail/{id}`.
  - [x] Implement `POST /resume` (Handle OptimisticLockException -> 409).
  - [x] Implement `GET /history` (Query MySQL).
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 7. Verification and Integration
  - Write Unit Test: `SchedulerServiceTest` (Pause/Resume flows).
  - Write Integration Test: Full flow from Start -> Pause -> Resume -> Complete.
  - _Requirements: 6.0_
