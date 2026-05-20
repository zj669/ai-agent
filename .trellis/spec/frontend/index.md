# Frontend Spec Index

This directory documents the active frontend at `ai-agent-foward/`.
It does not describe the legacy `app/frontend/` skeleton.

## Current Evidence

| Source | Fact |
| --- | --- |
| `ai-agent-foward/package.json` | Active package name is `ai-agent-foward`; scripts are `dev`, `build`, `preview`, `test`, `test:e2e`, `lint`, `typecheck`. |
| `ai-agent-foward/package-lock.json` | Installed lockfile resolves React `19.2.4`, Vite `7.3.1`, TypeScript `5.9.3`, Ant Design `6.3.0`, `@xyflow/react` `12.10.1`, Zustand `5.0.11`, Tailwind CSS `3.4.19`, Axios `1.13.5`. |
| `ai-agent-foward/src/` | Actual top-level layout is `app/`, `lib/`, `modules/`, `shared/`, `test/`, plus root entry files. |
| `CLAUDE.md` | Its frontend architecture section is stale: it claims a flat `components/hooks/pages/services/stores` layout and older dependency versions. |
| GitNexus | Indexed frontend files and symbols under `ai-agent-foward/`, including `WorkflowEditorPage`, `useEditorStore`, `startChatStream`, and `subscribeUIStream`. |
| ABCoder | MCP calls were attempted, but the tool returned `user cancelled MCP tool call`; the local AST product at `~/abcoder-asts/ai-agent-foward.json` was inspected as fallback evidence. |

## Table Of Contents

| Spec | Summary |
| --- | --- |
| [Directory Structure](./directory-structure.md) | Actual `src/app`, `src/lib`, `src/modules`, `src/shared`, `src/test` layout, module ownership, and the stale `CLAUDE.md` discrepancy. |
| [State Management](./state-management.md) | Zustand usage in `modules/workflow/stores/useEditorStore.ts`, when to use stores versus local state, naming rules, and anti-patterns. |
| [API And SSE](./api-and-sse.md) | Axios client setup, API adapter pattern, token handling, client errors, chat streaming via `fetch`, and EventSource usage in swarm. |
| [Component Guidelines](./component-guidelines.md) | Component and hook naming, AntD usage, Tailwind coexistence, route/page placement, and `@xyflow/react` canvas wiring. |
| [Quality Guidelines](./quality-guidelines.md) | TypeScript strictness, validation boundaries, current absence of Zod/react-hook-form, testing conventions, and common frontend anti-patterns. |

## Loading Rule

1. Start with this index for current frontend facts.
2. Read `directory-structure.md` before choosing where a new frontend file belongs.
3. Read the topic-specific file before changing state, API/SSE, components, or tests.
4. If a module has an `AGENT.md`, load it before working in that module.
5. Treat `ai-agent-foward/src/modules/<module>/AGENT.md` as local ownership context, not as a replacement for these shared frontend conventions.

## Module AGENT Files Observed

| Module | AGENT file |
| --- | --- |
| Agent | `ai-agent-foward/src/modules/agent/AGENT.md` |
| Auth | `ai-agent-foward/src/modules/auth/AGENT.md` |
| Chat | `ai-agent-foward/src/modules/chat/AGENT.md` |
| Dashboard | `ai-agent-foward/src/modules/dashboard/AGENT.md` |
| Knowledge | `ai-agent-foward/src/modules/knowledge/AGENT.md` |
| LLM Config | `ai-agent-foward/src/modules/llm-config/AGENT.md` |
| Review | `ai-agent-foward/src/modules/review/AGENT.md` |
| Swarm | `ai-agent-foward/src/modules/swarm/AGENT.md` |
| Workflow | `ai-agent-foward/src/modules/workflow/AGENT.md` |

## Non-Goals

- Do not document `app/frontend/` as the active app.
- Do not rely on the stale frontend tree in `CLAUDE.md`.
- Do not invent dependencies that are not present in `package.json`.
- Do not move files from the current feature-module layout into a flat layout.
- Do not treat SSE streams as normal Axios requests; the current implementation uses streaming `fetch` and browser `EventSource`.
