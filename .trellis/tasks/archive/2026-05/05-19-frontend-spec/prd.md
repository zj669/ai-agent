# Frontend Spec

## Goal

Rebuild `.trellis/spec/frontend/` from scratch (it was deleted) with real conventions from `ai-agent-foward/` — the active React 19 + Vite + AntD + Zustand frontend.

## Project Context

Active frontend: **`ai-agent-foward/`** (NOT `app/frontend/` which is a legacy skeleton — DO NOT document the legacy skeleton).

**Stack** (verify via `ai-agent-foward/package.json`):
- React 19.2.1
- Vite 6.2.0
- TypeScript 5.8.2
- Ant Design 6.1.1 (UI library)
- `@xyflow/react` 12.10.0 (workflow canvas)
- Zustand 5.0.9 (state)
- Tailwind CSS 4.1.18
- Zod 4.2.1 (schema validation)
- react-hook-form 7.69.0 (forms)
- Axios (HTTP)

**IMPORTANT — Actual structure differs from CLAUDE.md**:

CLAUDE.md (`/home/zj669/repo/ai-agent/CLAUDE.md` ~lines 145-157) claims the frontend uses a flat `components/hooks/pages/services/stores` layout. **This is incorrect.** The actual `ai-agent-foward/src/` contains:
```
app/        App.tsx, main.tsx
lib/
modules/
shared/
test/
```

You MUST document the **actual** structure, not the CLAUDE.md description. Note this discrepancy in the spec so future readers don't trust the stale docs.

**SSE consumption**: the workflow canvas (`@xyflow/react`) subscribes to backend SSE for execution events.

## Files You Own

Exclusively yours:
- `.trellis/spec/frontend/index.md` **(create new — entry / TOC)**
- `.trellis/spec/frontend/directory-structure.md` **(create new — actual layout)**
- `.trellis/spec/frontend/state-management.md` **(create new — Zustand stores)**
- `.trellis/spec/frontend/api-and-sse.md` **(create new — Axios + SSE consumption)**
- `.trellis/spec/frontend/component-guidelines.md` **(create new — naming, AntD usage, Tailwind co-existence)**
- `.trellis/spec/frontend/quality-guidelines.md` **(create new — TS strictness, validation with Zod, forms with react-hook-form)**

You may create additional files if patterns warrant; update `index.md` to reflect them.

### Required sections (per file)

**index.md**: TOC table, link to each spec file with one-line summary.

**directory-structure.md**:
1. Actual layout (`app/`, `lib/`, `modules/`, `shared/`, `test/`)
2. What lives in each top-level dir
3. Discrepancy notice about CLAUDE.md

**state-management.md**:
1. Zustand store pattern (real example from `modules/` or `shared/`)
2. When to use store vs local component state
3. Store naming convention
4. Anti-patterns

**api-and-sse.md**:
1. Axios client setup, base URL, interceptors
2. SSE consumption pattern (likely in workflow canvas)
3. Auth token handling
4. Error handling on client side

**component-guidelines.md**:
1. Component naming (PascalCase) — file matches default export
2. AntD usage — theming, when to wrap, when to use raw
3. Tailwind + AntD coexistence
4. `@xyflow/react` canvas wiring (one real example)
5. Hook naming (`useX`)

**quality-guidelines.md**:
1. TypeScript strict mode
2. Zod schema validation at boundaries
3. react-hook-form patterns
4. Anti-patterns (typecasting `any`, prop drilling, etc.)

## Tools Available

### GitNexus MCP
- `gitnexus_cypher({query: "MATCH (n) WHERE n.file CONTAINS 'ai-agent-foward' RETURN n.name, n.file LIMIT 50"})`
- `gitnexus_query({query: "frontend SSE workflow"})` to find SSE consumer

### ABCoder MCP (TS AST IS available)
| Tool | Use |
|------|-----|
| `get_repo_structure({repo_name: "ai-agent-foward"})` | Full file listing |
| `get_file_structure({repo_name: "ai-agent-foward", file_path: "src/..."})` | Symbols in a file |
| `get_ast_node({repo_name: "ai-agent-foward", node_ids: [...]})` | Code + deps |

Use ABCoder aggressively here — TS AST is available and high-fidelity.

### Workflow
1. `get_repo_structure({repo_name: "ai-agent-foward"})` to see all files
2. Read `package.json` to confirm dep versions
3. Read `tsconfig.json` for strict settings
4. Sample 2-3 modules under `src/modules/` for real patterns
5. Find Zustand store and SSE consumer with `get_file_structure`
6. Write specs with real file paths from `ai-agent-foward/src/`

## Rules

- ONLY create/modify files under `.trellis/spec/frontend/`
- DO NOT modify source code in `ai-agent-foward/`
- DO NOT modify backend specs or cross-layer specs
- DO NOT run git commands
- Document **actual** structure, not CLAUDE.md description

## Acceptance Criteria

- [ ] All 6 listed spec files exist
- [ ] Each substantive file (excluding index) 80+ lines
- [ ] `directory-structure.md` documents actual `app/lib/modules/shared/test` layout
- [ ] CLAUDE.md discrepancy explicitly noted
- [ ] At least 1 real Zustand store example with file path
- [ ] SSE consumption documented with file path
- [ ] `index.md` reflects final file set
- [ ] No placeholder text
- [ ] No source files modified

## Technical Notes

- Frontend root: `/home/zj669/repo/ai-agent/ai-agent-foward/`
- Dev server: `npm run dev` → http://localhost:5173
- TS AST product: `~/abcoder-asts/ai-agent-foward.json` (already parsed)
- Backend pairs on http://localhost:8080
