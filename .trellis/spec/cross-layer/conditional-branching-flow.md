# Conditional Branching Flow

This flow documents condition-node authoring, frontend graph payloads, backend
condition execution, selected-branch routing, branch pruning, and current UI
status limitations.

## Scope

- Frontend condition branch editing and validation.
- Graph save and publish payload conversion.
- Runtime injection of outgoing edges.
- Structured expression routing and LLM routing.
- Aggregate branch pruning and skipped-node semantics.
- Runtime frontend status display expectations.

## Frontend Authoring

1. `ConditionBranchEditor` defines branch operators and value source types:
   `ai-agent-foward/src/modules/workflow/components/ConditionBranchEditor.tsx:40`.
2. New branches are created with id, label, condition groups, and non-default
   type:
   `ai-agent-foward/src/modules/workflow/components/ConditionBranchEditor.tsx:66`.
3. Branch add, update, and remove handlers mutate condition config:
   `ai-agent-foward/src/modules/workflow/components/ConditionBranchEditor.tsx:310`.
4. LLM routing mode loads LLM configs for model selection:
   `ai-agent-foward/src/modules/workflow/components/ConditionBranchEditor.tsx:340`.
5. LLM routing mode renders branch targets and prompt fields:
   `ai-agent-foward/src/modules/workflow/components/ConditionBranchEditor.tsx:360`.
6. Expression routing mode renders branch conditions and branch management:
   `ai-agent-foward/src/modules/workflow/components/ConditionBranchEditor.tsx:419`.
7. `WorkflowNode` synchronizes condition config from node data:
   `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx:271`.
8. `WorkflowNode` creates a default branch config when needed:
   `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx:302`.
9. `WorkflowNode` renders source handles for condition branches:
   `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx:496`.
10. Branch handles use branch identifiers so edges can bind to branch outputs:
    `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx:505`.

## Frontend Save and Publish Payload

1. `WorkflowEditorPage` normalizes condition source handles and branch labels:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:286`.
2. Backend branches are converted to UI config while preserving source handles
   and target-node mapping:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:363`.
3. `getConditionConfig` reads condition config from node data:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:456`.
4. `getConditionBranchesForHandles` resolves branch handles for graph edges:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:469`.
5. `buildBackendConditionBranches` maps branch source handles to edge targets:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:485`.
6. Default branches are serialized with default branch priority:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:509`.
7. Non-default branches serialize normalized refs, operators, and condition
   groups:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:522`.
8. `buildConditionUserConfig` stores routing strategy, branches, LLM config id,
   and routing prompt:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:540`.
9. `validateConditionNodes` validates condition configuration before save or
   publish:
   `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:626`.
10. Validation requires exactly one else/default branch:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:650`.
11. Validation requires each branch to connect to exactly one target:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:664`.
12. Expression mode validation requires condition groups to be complete:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:675`.
13. LLM mode skips expression condition validation:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:672`.
14. `buildGraphPayload` serializes condition nodes with condition user config
    and edges with `sourceHandle`:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:941`.
15. `handleSave` runs graph and condition validation before saving:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:1239`.
16. `handlePublish` validates before publishing and requires a clean saved
    graph:
    `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx:1295`.

## Runtime Scheduling Inputs

1. `SchedulerService.scheduleNode` resolves node inputs from upstream outputs
   and context:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:843`.
2. The scheduler injects execution context as `__context__`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:867`.
3. The scheduler injects agent id as `__agentId__`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:870`.
4. The scheduler injects outgoing graph edges as `__outgoingEdges__`:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:873`.
5. Strategies return `NodeExecutionResult` values to the scheduler callback:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:879`.
6. Completed strategy results reach `onNodeComplete` for aggregate advancement:
   `ai-agent-application/src/main/java/com/zj/aiagent/application/workflow/SchedulerService.java:930`.

## Structured Condition Runtime

1. `ConditionNodeExecutorStrategy.executeAsync` reads node config and selected
   routing strategy:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:75`.
2. The default routing strategy is expression mode:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:82`.
3. Structured expression mode parses branches from `config.properties.branches`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:108`.
4. If structured branches are absent, the strategy converts legacy outgoing
   edges into branch definitions:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:117`.
5. The strategy evaluates branch conditions with `StructuredConditionEvaluator`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:136`.
6. The strategy returns `NodeExecutionResult.routing(selectedTargetNodeId,
   outputs)`:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:140`.
7. `parseBranches` converts raw branch objects with Jackson:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:150`.
8. Legacy conversion maps edge conditions into structured branch objects:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:175`.
9. `NodeExecutionResult.routing` stores selected target id in
   `selectedBranchId`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/valobj/NodeExecutionResult.java:69`.

## Structured Evaluator

1. `StructuredConditionEvaluator.evaluate` validates branch definitions before
   matching:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:38`.
2. It sorts branches by priority:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:47`.
3. It returns the first non-default branch that matches:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:52`.
4. It falls back to the default branch when no non-default branch matches:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:61`.
5. It requires exactly one default branch:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:76`.
6. Branch evaluation requires condition groups:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:98`.
7. Group evaluation starts from the configured group operator:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/condition/StructuredConditionEvaluator.java:122`.

## LLM Routing Runtime

1. LLM routing parses structured branches or legacy edge branches:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:251`.
2. It builds valid target ids from branches:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:259`.
3. It builds a model client for routing:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:266`.
4. It prompts the model to select a branch:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:273`.
5. It retries and falls back to a default branch when model routing is invalid:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:290`.
6. It returns a routing result using the selected target node id:
   `ai-agent-infrastructure/src/main/java/com/zj/aiagent/infrastructure/workflow/executor/ConditionNodeExecutorStrategy.java:305`.

## Aggregate Branch Pruning

1. `Execution.advance` checks whether the node result is routing:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:185`.
2. `pruneUnselectedBranches` reads the selected target id from
   `NodeExecutionResult.selectedBranchId`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:363`.
3. It compares each successor node id to the selected target id:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:373`.
4. Unselected successors are skipped recursively:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:376`.
5. Recursive skip marks node status `SKIPPED`:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:387`.
6. Convergent nodes with multiple predecessors are skipped only if all
   predecessors are skipped:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:408`.
7. `getReadyNodes` treats skipped predecessors as effective completions:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:326`.
8. Execution completion allows `SUCCEEDED`, `SKIPPED`, and `FAILED` node
   statuses:
   `ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/entity/Execution.java:427`.

## Frontend Runtime Status Gap

1. `ExecutionDTO.nodeStatuses` exposes backend node status names:
   `ai-agent-interfaces/src/main/java/com/zj/aiagent/interfaces/workflow/dto/ExecutionDTO.java:24`.
2. `chatAdapter.ExecutionData` models `nodeStatuses` as a record of strings:
   `ai-agent-foward/src/shared/api/adapters/chatAdapter.ts:66`.
3. `ChatPage.STATUS_TAG` handles `SUCCEEDED`, `PENDING`, and
   `PAUSED_FOR_REVIEW`:
   `ai-agent-foward/src/modules/chat/pages/ChatPage.tsx:416`.
4. There is no dedicated `SKIPPED` label in the current chat status tag map.
5. Runtime branch pruning is backend-owned; frontend status display must treat
   missing or unrecognized status strings conservatively until explicit skipped
   visuals are added.

## Gotchas

1. In current structured routing, the selected branch value is the selected
   target node id, not the UI branch id.
2. Frontend `sourceHandle` is the bridge between branch rows and graph edges.
3. Expression mode uses structured branch data in current payloads; legacy edge
   condition conversion exists for backward compatibility.
4. LLM mode still requires branches with target node ids.
5. Validation requires exactly one default branch before save or publish.
6. Convergent nodes are protected from premature skip when at least one
   predecessor remains unskipped.
7. Backend can return `SKIPPED` node statuses even though the current frontend
   status map does not render a specific skipped label.
8. Any change to branch id, source handle, or selected target semantics must be
   updated in editor payload construction, runtime strategy, and aggregate
   pruning together.

