# Cross-Layer Spec

## Goal

Create a new spec directory `.trellis/spec/cross-layer/` documenting **end-to-end data flows that span backend layers AND frontend** — the kind of behavior that no single-layer spec can capture cleanly.

## Project Context

This codebase has a thinking guide at `.trellis/spec/guides/cross-layer-thinking-guide.md` (259 lines, already filled). That guide is **meta** — "how to think about cross-layer issues". This new directory is the **concrete contracts**: actual flows in this project, with file paths and code.

**Canonical cross-layer flows to document**:

### 1. Workflow Execution + SSE
- User clicks "Run" in frontend (`ai-agent-foward/`, workflow canvas)
- Axios POST → `WorkflowController` (`/api/workflow/execution`)
- `SchedulerService.startExecution` → `Execution.start()`
- Memory hydration: VectorStore (LTM) + ConversationRepository (STM)
- Per node: `NodeExecutorStrategy` → `StreamPublisher` → Redis Pub/Sub channel
- Controller subscribes → `SseEmitter` → frontend receives events
- Frontend updates canvas + chat panel
- `Execution.advance` → next ready nodes → loop
- Completion: `SchedulerService.onExecutionComplete` → `ChatApplicationService.completeAssistantMessage`

### 2. Conversation Streaming
- User sends message in chat UI
- `ChatController` (`/api/chat`) → `ChatApplicationService`
- `ChatModelPort` → `OpenAiChatModelAdapter` → upstream LLM
- Token chunks → `StreamPublisher` → Redis → SSE → frontend incremental render
- On completion: persist final message via `ConversationRepository`

### 3. Knowledge Retrieval
- Workflow `KnowledgeNodeExecutorStrategy` → `VectorStore.search`
- `MilvusVectorStoreAdapter` → Milvus (port 19530)
- Hits passed back into `ExecutionContext` → fed to subsequent LLM node

### 4. Authentication
- Login: frontend → `UserController` (`/client/user`, **NOT** `/api/user`)
- Token issued, stored frontend-side (verify storage mechanism — likely Zustand + sessionStorage)
- Token attached on every Axios request via interceptor
- Backend auth filter validates token

### 5. Human Review Pause/Resume
- Workflow hits review-required node → `Execution` status `PAUSED_FOR_REVIEW`
- Checkpoint saved to Redis
- Frontend polls or receives SSE event
- Human action → `HumanReviewController` (`/api/workflow/reviews`) → `resumeExecution`
- Execution continues

### 6. Conditional Branching
- Condition node evaluates SpEL or LLM result
- `Execution.pruneUnselectedBranches` marks unselected as SKIPPED
- Convergence node skipped only if ALL predecessors SKIPPED
- Frontend canvas reflects skipped paths visually

## Files You Own

Exclusively yours (all NEW):
- `.trellis/spec/cross-layer/index.md` (TOC + summary)
- `.trellis/spec/cross-layer/workflow-execution-flow.md`
- `.trellis/spec/cross-layer/chat-streaming-flow.md`
- `.trellis/spec/cross-layer/knowledge-retrieval-flow.md`
- `.trellis/spec/cross-layer/auth-flow.md`
- `.trellis/spec/cross-layer/human-review-flow.md`
- `.trellis/spec/cross-layer/conditional-branching-flow.md`
- `.trellis/spec/cross-layer/event-contracts.md` (SSE event shapes, REST request/response contracts)

You may consolidate or split — update `index.md` to reflect final set.

### Required content per flow file

For each flow:
1. **Purpose** — what user-visible feature does this serve
2. **End-to-End Sequence** — step-by-step from UI to DB and back, with **file paths at each step**
3. **Layer Boundaries Crossed** — frontend → controller → application → domain port → infrastructure adapter → external service
4. **Failure Modes** — what happens at each layer if it fails
5. **Cross-Layer Gotchas** — any non-obvious coupling

### event-contracts.md

Enumerate the SSE event shapes (e.g., `node.started`, `node.streaming`, `node.completed`, `execution.paused`) with TypeScript-style schema + which Java class produces them. Also enumerate REST request/response shapes for the key endpoints.

## Tools Available

### GitNexus MCP (this is where it shines)
- `gitnexus_query({query: "workflow execution start"})` → end-to-end execution flow trace
- `gitnexus_query({query: "SSE streaming"})` → streaming pipeline
- `gitnexus_impact({target: "Execution", direction: "downstream"})` → what depends on it
- `gitnexus_cypher({query: "MATCH path = (a)-[*1..3]->(b) WHERE a.name = 'WorkflowController' RETURN path LIMIT 10"})` → call chains

### ABCoder MCP
TS frontend AST IS available — use for frontend side of each flow:
- `get_repo_structure({repo_name: "ai-agent-foward"})`
- `get_file_structure({repo_name: "ai-agent-foward", file_path: "..."})`

Java parse skipped — use GitNexus + direct file reads for Java side.

### Workflow
1. For each flow, start at the user-facing entry (frontend component or REST endpoint)
2. Trace through with `gitnexus_query` / `gitnexus_context`
3. Read source at each hop for accurate code/file paths
4. Write the flow doc with **path = file:line** references
5. Cross-link between flow files when they share infrastructure (e.g., StreamPublisher reused by workflow + chat)

## Rules

- ONLY create/modify files under `.trellis/spec/cross-layer/`
- DO NOT modify source code
- DO NOT modify other spec directories (`backend/`, `frontend/`, `guides/`)
- DO NOT run git commands

## Acceptance Criteria

- [ ] `index.md` + 7 flow files all exist (or your consolidated set documented in index.md)
- [ ] Each flow file 80+ lines with concrete file:line references
- [ ] At least 1 SSE event schema in `event-contracts.md`
- [ ] `/client/user` vs `/api/user` quirk called out in auth flow
- [ ] Memory hydration (LTM/STM) documented in workflow execution flow
- [ ] No placeholder text
- [ ] Only files under `cross-layer/` modified

## Technical Notes

- This is the highest-leverage spec for AI agents — they will reference it constantly when implementing cross-cutting features
- File:line references must be accurate — verify before claiming
- The thinking guide at `.trellis/spec/guides/cross-layer-thinking-guide.md` is complementary, NOT a substitute (don't duplicate its content)
