# Test Matrix: Human-in-the-Loop Verification

## Target Class: `SchedulerService`

| 测试场景 | 输入条件 (Given) | 执行动作 (When) | 期望结果 (Then) | 优先级 |
|---------|-----------------|-----------------|-----------------|--------|
| **Resume BEFORE_EXECUTION** | Execution paused at `BEFORE` phase | `resumeExecution(...)` | 1. `humanReviewRepository.save()` called<br>2. `streamPublisher.publishEvent("workflow_resumed")` called<br>3. `rSet.remove()` called | P0 |
| **Resume Not Found** | `executionId` does not exist | `resumeExecution(...)` | `IllegalArgumentException` thrown | P1 |
| **Resume Cancelled** | Execution is cancelled | `resumeExecution(...)` | Method returns early, no side effects | P1 |

## Key Design Decisions
1. **Use `@InjectMocks`**: Let Mockito handle dependency injection instead of manual construction.
2. **Mock Redisson**: Use `lenient().doReturn()` for generic types like `RSet<String>`.
3. **Package**: `com.zj.aiagent.application.workflow` (matches `SchedulerService`).
