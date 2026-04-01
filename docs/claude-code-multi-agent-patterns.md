# Claude-Code Multi-Agent Collaboration Patterns

> Research based on source code in `Claude-Code/` directory.
> Date: 2026-03-31

---

## Executive Summary

Claude-Code implements a sophisticated multi-agent orchestration system with three execution backends (in-process, tmux, iTerm2), file-based message passing via mailbox system, shared task management, and a hierarchical coordinator-worker pattern designed for software engineering tasks.

---

## 1. Execution Architectures (Three Backends)

### 1.1 In-Process Backend

**File:** `src/utils/swarm/InProcessBackend.ts`

- Uses `AsyncLocalStorage` for context isolation between agents
- Shared React context with the leader agent
- `AbortController` for cancellation
- Direct prompt injection (no mailbox for initial message)
- Mailbox polling still used for subsequent messages

### 1.2 Tmux Backend

**File:** `src/utils/swarm/backends/TmuxBackend.ts`

- Creates split panes within existing tmux session, or separate windows in `claude-swarm` session
- CLI args passed via command line
- Environment variables for identity: `CLAUDE_CODE_AGENT_ID`, `CLAUDE_CODE_AGENT_NAME`, `CLAUDE_CODE_TEAM_NAME`

### 1.3 iTerm2 Backend

**File:** `src/utils/swarm/backends/ITermBackend.ts`

- Native iTerm2 split panes via `it2` CLI
- Requires `it2` installation
- Setup prompt for users without it2

### 1.4 Backend Abstraction

**File:** `src/utils/swarm/backends/types.ts`

```typescript
export type PaneBackend = {
  readonly type: BackendType
  readonly displayName: string
  readonly supportsHideShow: boolean

  isAvailable(): Promise<boolean>
  isRunningInside(): Promise<boolean>
  createTeammatePaneInSwarmView(name: string, color: AgentColorName): Promise<CreatePaneResult>
  sendCommandToPane(paneId: PaneId, command: string, useExternalSession?: boolean): Promise<void>
  killPane(paneId: PaneId, useExternalSession?: boolean): Promise<boolean>
  // ... other pane management methods
}
```

Auto-detected via `detectAndGetBackend()` with fallback to in-process.

---

## 2. Message Passing: Mailbox System (File-Based IPC)

### 2.1 Mailbox Structure

**File:** `src/utils/teammateMailbox.ts`

```
~/.claude/teams/{team_name}/inboxes/{agent_name}.json
```

**File Locking:** Uses `proper-lockfile` with retry backoff (10 retries, 5-100ms timeout).

### 2.2 Message Types

| Message Type | Direction | Purpose |
|--------------|-----------|---------|
| `idle_notification` | Worker -> Lead | Task completion/failure |
| `shutdown_request` | Lead -> Worker | Graceful shutdown request |
| `shutdown_approved` | Worker -> Lead | Shutdown acknowledgment |
| `shutdown_rejected` | Worker -> Lead | Shutdown refusal |
| `plan_approval_request` | Worker -> Lead | Request implementation approval |
| `plan_approval_response` | Lead -> Worker | Approval/rejection |
| `permission_request` | Worker -> Lead | Tool usage approval |
| `permission_response` | Lead -> Worker | Permission decision |
| `task_assignment` | Lead -> Worker | Task delegation |
| `team_permission_update` | Lead -> Workers | Bulk permission grants |
| `mode_set_request` | Lead -> Worker | Change teammate mode |
| `sandbox_permission_request` | Worker -> Lead | Network access approval |

### 2.3 Inbox Polling

**File:** `src/hooks/useInboxPoller.ts`

- Polling interval: 1000ms
- Routes messages to different queues based on type
- Delivery strategy:
  - **Idle state:** Submit immediately as new turn
  - **Busy state:** Queue in AppState for later delivery

### 2.4 SendMessage Tool

**File:** `src/tools/SendMessageTool/SendMessageTool.ts`

```typescript
const inputSchema = z.object({
  to: z.string(), // recipient name, "*" for broadcast, "uds:<path>" or "bridge:<id>"
  summary: z.string().optional(), // 5-10 word preview
  message: z.union([
    z.string(), // plain text
    StructuredMessage() // shutdown_request, shutdown_response, plan_approval_response
  ])
})
```

Handlers:
- `handleMessage()` — Direct message to single recipient
- `handleBroadcast()` — Send to all teammates except sender
- `handleShutdownRequest()` — Request teammate termination
- `handleShutdownApproval()` — Acknowledge shutdown (signals abort)
- `handlePlanApproval()` — Approve worker implementation plan

---

## 3. Work Coordination: Task System

### 3.1 Task Schema

**File:** `src/utils/tasks.ts`

```typescript
export type Task = {
  id: string
  subject: string
  description: string
  activeForm?: string  // e.g., "Running tests"
  owner?: string       // agent ID
  status: 'pending' | 'in_progress' | 'completed'
  blocks: string[]    // task IDs this blocks
  blockedBy: string[]  // task IDs blocking this
  metadata?: Record<string, unknown>
}
```

**Storage:** `~/.claude/tasks/{taskListId}/{taskId}.json`

### 3.2 Task Tools

- **TaskCreate:** `src/tools/TaskCreateTool/TaskCreateTool.ts` — Creates task with subject, description, activeForm, metadata
- **TaskUpdate:** `src/tools/TaskUpdateTool/TaskUpdateTool.ts` — Updates status, owner, blocks, etc.
  - Auto-assigns owner when teammate marks task in_progress
  - Sends `task_assignment` message via mailbox when ownership changes

### 3.3 Concurrency Control

**File:** `src/utils/tasks.ts` (lines 541-692)

```typescript
export async function claimTask(
  taskListId: string,
  taskId: string,
  claimantAgentId: string,
  options: ClaimTaskOptions = {}  // checkAgentBusy?: boolean
): Promise<ClaimTaskResult>
```

**Claim Reasons:**
- `task_not_found` / `already_claimed` / `already_resolved`
- `blocked` (with `blockedByTasks`)
- `agent_busy` (with `busyWithTasks`)

Uses task-list-level lock for TOCTOU prevention.

### 3.4 Agent Status Tracking

```typescript
// Agent is "idle" if no open tasks owned
// Agent is "busy" if owns at least one open task
export async function getAgentStatuses(teamName: string): Promise<AgentStatus[] | null>
```

---

## 4. Coordinator Pattern (Star Topology)

**File:** `src/coordinator/coordinatorMode.ts`

Coordinator activated by `CLAUDE_CODE_COORDINATOR_MODE` env var.

### 4.1 Phase-Based Workflow

| Phase | Who | Purpose |
|-------|-----|---------|
| Research | Workers (parallel) | Investigate codebase, find relevant files |
| Synthesis | Coordinator | Read findings, craft specs |
| Implementation | Workers | Make targeted changes per spec |
| Verification | Workers (can overlap) | Test changes work |

### 4.2 Continue vs. Spawn Decision Matrix

| Situation | Mechanism | Reason |
|-----------|-----------|--------|
| Research explored exact files needing edit | **Continue** | Worker has relevant context |
| Research was broad, implementation is narrow | **Spawn fresh** | Avoid exploration noise |
| Correcting failure or extending recent work | **Continue** | Error context is valuable |
| Verifying code another worker wrote | **Spawn fresh** | Fresh eyes avoid bias |
| Wrong approach entirely | **Spawn fresh** | Clean slate |
| Unrelated task | **Spawn fresh** | No context overlap |

### 4.3 Task Notification Protocol

Worker results arrive as `<task-notification>` XML:

```xml
<task-notification>
  <task-id>{agentId}</task-id>
  <status>completed|failed|killed</status>
  <summary>{human-readable status summary}</summary>
  <result>{agent's final text response}</result>
  <usage>
    <total_tokens>N</total_tokens>
    <tool_uses>N</tool_uses>
    <duration_ms>N</duration_ms>
  </usage>
</task-notification>
```

### 4.4 Team Creation

**File:** `src/tools/TeamCreateTool/TeamCreateTool.ts`

```typescript
export type TeamFile = {
  name: string
  description?: string
  createdAt: number
  leadAgentId: string
  leadSessionId?: string
  members: Array<{
    agentId: string
    name: string
    agentType?: string
    model?: string
    prompt?: string
    color?: string
    planModeRequired?: boolean
    joinedAt: number
    tmuxPaneId: string
    cwd: string
    subscriptions: string[]
    backendType?: BackendType
    isActive?: boolean
    mode?: PermissionMode
  }>
}
```

Key constraints:
- One team per leader (enforced)
- Deterministic agent ID: `formatAgentId(TEAM_LEAD_NAME, teamName)`
- Stores team config at `~/.claude/teams/{team_name}/config.json`

---

## 5. Spawn Flow

**File:** `src/tools/shared/spawnMultiAgent.ts`

```
1. Validate team context exists
2. Generate unique teammate name (append numeric suffix if conflict)
3. Sanitize name (replace `@` with `-` for agent IDs)
4. Assign color via assignTeammateColor()
5. Create pane via backend
6. Build CLI command with inherited flags
7. Send initial prompt via mailbox
8. Register in team.json + AppState
```

Three spawn strategies:

**A. In-Process (lines 840-1032)**
- Uses AsyncLocalStorage for context isolation
- Direct prompt injection without mailbox
- AbortController for cancellation

**B. Split-Pane (lines 305-539)**
- Creates tmux/iTerm2 panes alongside leader
- Backend auto-detection

**C. Separate Window (lines 545-753)**
- Creates dedicated tmux windows in `claude-swarm` session
- CLI args propagation via `--agent-id`, `--agent-name`, `--team-name`, etc.

---

## 6. Permission Bridging

**File:** `src/hooks/toolPermission/handlers/swarmWorkerHandler.ts`

Worker permission flow:
1. Try classifier auto-approval
2. Forward request to leader via mailbox
3. Register callback for leader response
4. Show pending indicator while waiting
5. Handle abort signal during wait

---

## 7. Session Cleanup and Lifecycle

### 7.1 Shutdown Protocol

```
Leader -> Worker: shutdown_request
Worker -> Leader: shutdown_approved | shutdown_rejected
On approval: abortController.abort() (in-process) or pane.kill() (tmux)
```

### 7.2 Orphan Cleanup

**File:** `src/utils/swarm/teamHelpers.ts` (lines 576-683)

On ungraceful leader exit (SIGINT/SIGTERM):
1. Kill all pane-backed teammate panes
2. Clean up team directories
3. Clean up tasks directories

### 7.3 Team Deletion

**File:** `src/tools/TeamDeleteTool/TeamDeleteTool.ts`

Gracefully shuts down all teammates, destroys git worktrees, removes team and task list directories.

---

## 8. Key Design Patterns

| Pattern | Location | Description |
|---------|----------|-------------|
| **Deterministic Agent IDs** | `formatAgentId(name, teamName)` | Consistent across processes |
| **File-Based IPC** | `teammateMailbox.ts` | JSON mailboxes with file locking |
| **AsyncLocalStorage Isolation** | `InProcessBackend.ts` | In-process teammate context isolation |
| **Pervasive Task Lists** | `tasks.ts` | Shared task list ID per team |
| **Structured Message Protocols** | `teammateMailbox.ts` | Typed JSON messages for coordination |
| **Backend Abstraction** | `backends/types.ts` | Pluggable execution backends |
| **Coordinator Synthesis** | `coordinatorMode.ts` | Leader must understand worker results before delegating |
| **Permission Bridging** | `swarmWorkerHandler.ts` | Workers forward permission requests to leader |

---

## 9. Key Files Summary

| Category | File Path |
|----------|-----------|
| **Coordinator** | `src/coordinator/coordinatorMode.ts` |
| **Worker Type** | `src/coordinator/workerAgent.ts` |
| **Team Creation** | `src/tools/TeamCreateTool/TeamCreateTool.ts` |
| **Team Deletion** | `src/tools/TeamDeleteTool/TeamDeleteTool.ts` |
| **Team Helpers** | `src/utils/swarm/teamHelpers.ts` |
| **Spawn Logic** | `src/tools/shared/spawnMultiAgent.ts` |
| **Mailbox System** | `src/utils/teammateMailbox.ts` |
| **Inbox Poller** | `src/hooks/useInboxPoller.ts` |
| **SendMessage** | `src/tools/SendMessageTool/SendMessageTool.ts` |
| **Task System** | `src/utils/tasks.ts` |
| **TaskCreate** | `src/tools/TaskCreateTool/TaskCreateTool.ts` |
| **TaskUpdate** | `src/tools/TaskUpdateTool/TaskUpdateTool.ts` |
| **Backend Types** | `src/utils/swarm/backends/types.ts` |
| **In-Process Runner** | `src/utils/swarm/InProcessBackend.ts` |
| **Tmux Backend** | `src/utils/swarm/backends/TmuxBackend.ts` |
| **iTerm2 Backend** | `src/utils/swarm/backends/ITermBackend.ts` |
| **Permission Handler** | `src/hooks/toolPermission/handlers/swarmWorkerHandler.ts` |
| **Constants** | `src/utils/swarm/constants.ts` |
