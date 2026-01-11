# Tasks Document - Workflow Orchestration (v3.5)

- [x] 1. Define Core Domain Entities & Value Objects
  - File: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java`, `Node.java`, `WorkflowGraph.java`
  - File: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/vo/ExecutionContext.java`, `NodeExecutionResult.java`
  - File: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/config/NodeConfig.java` (and subtypes: `LlmNodeConfig`, `HttpNodeConfig`, `ConditionNodeConfig`)
  - File: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/vo/Branch.java`
  - Implement the `Execution` aggregate root, `Node` entity, and `ExecutionContext` VO.
  - **Constraint**: `ExecutionContext` must include SpEL resolution logic (`resolveInput(Map<String, Object> inputs)`).
  - _Leverage: com.zj.aiagent.common.enums.ExecutionStatus_
  - _Requirements: 1.1, 4.1_
  - _Prompt: Implement core domain entities for Workflow Orchestration. Execution aggregate must manage state transitions and references to ExecutionContext. NodeExecutionResult should support generic outputs and selectedBranchId. Define NodeConfig subtypes._

- [x] 2. Define Node Execution Strategy Interface & Factory
  - File: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/port/NodeExecutorStrategy.java`
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/NodeExecutorFactory.java`
  - Define the Async Strategy Interface: `CompletableFuture<NodeExecutionResult> executeAsync(Node node, Map<String, Object> inputVariables)`.
  - Implement Factory (Registry pattern) to manage strategy beans and resolve by `NodeType`.
  - _Requirements: 2.1_

- [x] 3. Implement Infrastructure Configuration (ThreadPool & Redisson)
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/config/WorkflowConfig.java`
  - Configure `ThreadPoolTaskExecutor` ("nodeExecutorThreadPool") for IO-intensive tasks.
  - Configure `RedissonClient` bean (if not already present) for distributed locks.
  - _Requirements: Non-functional (Concurrency)_

- [x] 4. Implement Infrastructure Strategies (All Types)
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/LlmNodeExecutorStrategy.java`
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/HttpNodeExecutorStrategy.java`
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java`
  - Implement `executeAsync` using `CompletableFuture.supplyAsync` with the custom thread pool.
  - LLM Strategy: Integrate with Spring AI. 
  - HTTP Strategy: Use `WebClient` or `RestTemplate`.
  - **Condition Strategy**: Implement support for 'EXPRESSION' (SpEL) and 'LLM' (Spring AI) routing modes. MUST return `selectedBranchId`.
  - _Requirements: 2.2, 3.2, 3.3_
  - _Prompt: Implement strategies. For ConditionNodeExecutorStrategy, support 'EXPRESSION' (using SpEL) and 'LLM' (using Spring AI to select a branch) modes. Return NodeExecutionResult with selectedBranchId._

- [x] 5. Implement Scheduler Service (Core Logic)
  - File: `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/service/SchedulerService.java`
  - Implement DAG traversal, In-Degree calculation, and Parallel Dispatching.
  - **Critical**: Implement `onNodeComplete` callback with **Redisson Distributed Lock** to effectively serialize `Execution.advance()`.
  - **Critical**: Implement Branch Pruning logic (mark skipped keys recursively) in `advance()` and handle "Diamond" join logic (skipped parents = satisfied).
  - _Requirements: 2.1, 2.2, 3.1_

- [x] 6. Implement Persistence Layer (Hybrid)
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/RedisCheckpointRepository.java` (Hot State)
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/repository/MySqlExecutionRepository.java` (History)
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/listener/WorkflowAuditListener.java`
  - Implement save/load of `Execution` aggregate to Redis.
  - Implement `WorkflowAuditListener` (via Spring `@EventListener`) to asynchronously write `NodeCompletedEvent` to `workflow_node_execution_log` table (MyBatis).
  - _Requirements: 4.2_
  - _Prompt: Implement persistence. Redis for active state. Create WorkflowAuditListener to handle NodeExecutionFinishedEvent and insert logs into MySQL table workflow_node_execution_log asynchronously._

- [x] 7. Implement Streaming API & Events
  - File: `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/WorkflowController.java`
  - File: `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/event/SseEventPublisher.java`
  - Endpoints: `POST /execution/start`, `POST /execution/resume`, `GET /execution/stream/{executionId}`.
  - Implement SSE logic to push node status changes to frontend.
  - _Requirements: 5.1_

- [ ] 8. Integration Testing (End-to-End)
  - File: `ai-agent-interfaces/src/test/java/com/zj/aiagent/interfaces/workflow/WorkflowExecutionTest.java`
  - Test a full flow: Start -> Condition (Branch A) -> LLM -> Join -> End.
  - Verify SSE events, MySQL logs, and Redis final state.
  - _Requirements: All_
