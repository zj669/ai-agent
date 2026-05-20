<!-- TRELLIS:START -->
# Trellis Instructions

These instructions are for AI assistants working in this project.

This project is managed by Trellis. The working knowledge you need lives under `.trellis/`:

- `.trellis/workflow.md` — development phases, when to create tasks, skill routing
- `.trellis/spec/` — package- and layer-scoped coding guidelines (read before writing code in a given layer)
- `.trellis/workspace/` — per-developer journals and session traces
- `.trellis/tasks/` — active and archived tasks (PRDs, research, jsonl context)

If a Trellis command is available on your platform (e.g. `/trellis:finish-work`, `/trellis:continue`), prefer it over manual steps. Not every platform exposes every command.

If you're using Codex or another agent-capable tool, additional project-scoped helpers may live in:
- `.agents/skills/` — reusable Trellis skills
- `.codex/agents/` — optional custom subagents

Managed by Trellis. Edits outside this block are preserved; edits inside may be overwritten by a future `trellis update`.

<!-- TRELLIS:END -->

# Codex 项目初始化入口（按业务模块分发）

## 启动原则
1. 先读全局：`docs/PROJECT_QUICK_CONTEXT.md`
2. 再按任务选择业务模块，只加载对应模块的 `AGENT.md`
3. 需要跨模块时，再增量加载下一个模块，避免全仓扫描

## 全局入口
1. `README.md`
2. `docs/PROJECT_QUICK_CONTEXT.md`

## 业务模块索引
1. Agent
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/agent/AGENT.md`
- 前端：`ai-agent-foward/src/modules/agent/AGENT.md`

2. Workflow
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/AGENT.md`
- 前端：`ai-agent-foward/src/modules/workflow/AGENT.md`

3. Chat
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/chat/AGENT.md`
- 前端：`ai-agent-foward/src/modules/chat/AGENT.md`

4. Knowledge
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/knowledge/AGENT.md`
- 前端：`ai-agent-foward/src/modules/knowledge/AGENT.md`

5. User/Auth
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/user/AGENT.md`
- 后端（鉴权）：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/auth/AGENT.md`
- 前端：`ai-agent-foward/src/modules/auth/AGENT.md`

6. Review（审核）
- 后端归属：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/workflow/AGENT.md`
- 前端：`ai-agent-foward/src/modules/review/AGENT.md`

7. Dashboard
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/dashboard/AGENT.md`
- 前端：`ai-agent-foward/src/modules/dashboard/AGENT.md`

8. LLM Config
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/llm/AGENT.md`
- 前端：`ai-agent-foward/src/modules/llm-config/AGENT.md`

9. Swarm
- 后端：`ai-agent-domain/src/main/java/com/zj/aiagent/domain/swarm/AGENT.md`
- 前端：`ai-agent-foward/src/modules/swarm/AGENT.md`

## 执行规则
1. 任何任务先输出“选中的模块 + 已加载的模块 AGENT 列表”。
2. 未命中模块时，先回到全局入口，不直接遍历源码。
3. 涉及流程暂停/恢复/审批，默认联动 Workflow + Review 两个模块。

<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **ai-agent** (10398 symbols, 22664 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> If any GitNexus tool warns the index is stale, run `npx gitnexus analyze` in terminal first.

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `gitnexus_impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `gitnexus_detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `gitnexus_query({query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `gitnexus_context({name: "symbolName"})`.

## Never Do

- NEVER edit a function, class, or method without first running `gitnexus_impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `gitnexus_rename` which understands the call graph.
- NEVER commit changes without running `gitnexus_detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/ai-agent/context` | Codebase overview, check index freshness |
| `gitnexus://repo/ai-agent/clusters` | All functional areas |
| `gitnexus://repo/ai-agent/processes` | All execution flows |
| `gitnexus://repo/ai-agent/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
