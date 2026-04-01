# Frontend Directory Structure

> How the frontend code is organized in `ai-agent-foward/`.

---

## Overview

The frontend uses a **modular structure** organized by business domain (modules), with shared utilities and app-level setup separated. This is NOT a flat structure — it mirrors backend bounded contexts.

**⚠️ The active frontend is `ai-agent-foward/`** — NOT `app/frontend/` (legacy skeleton).

---

## Top-Level Layout

```
ai-agent-foward/
├── public/                    # Static assets
├── src/
│   ├── main.tsx               # Application entry point
│   ├── App.tsx                # Root component (delegates to router)
│   ├── index.css              # Global styles
│   ├── vite-env.d.ts          # Vite type declarations
│   ├── app/                   # App shell, routing, auth
│   ├── modules/               # Business feature modules
│   ├── shared/                # Shared utilities across modules
│   ├── lib/                   # Third-party library configs
│   └── test/                  # Test utilities and setup
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.js
└── eslint.config.js
```

---

## `app/` — Application Shell

App-level concerns: routing, authentication, layout.

```
app/
├── App.tsx                    # Root component
├── AppShell.tsx               # Main layout shell (sidebar + content area)
├── AuthGuard.tsx              # Route guard (RequireAuth)
├── auth.ts                    # Auth utility functions
├── boot.ts                    # App initialization
├── router.tsx                 # All routes defined here
├── components/                # App-level components (sidebar, header)
├── pages/                     # App-level pages (Login, Register, NotFound)
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── ForgotPasswordPage.tsx
│   └── NotFoundPage.tsx
└── __tests__/                 # App-level tests
```

### Routing Structure

All routes defined in `app/router.tsx`:

```tsx
<Routes>
  {/* Public routes */}
  <Route path="/login" element={<LoginPage />} />
  <Route path="/register" element={<RegisterPage />} />

  {/* Protected routes (wrapped in AppShell) */}
  <Route element={<RequireAuth />}>
    <Route path="/" element={<AppShell />}>
      <Route path="dashboard" element={<DashboardPage />} />
      <Route path="agents" element={<AgentListPage />} />
      <Route path="knowledge" element={<KnowledgePage />} />
      <Route path="chat" element={<ChatPage />} />
      <Route path="reviews" element={<ReviewPage />} />
      <Route path="llm-config" element={<LlmConfigPage />} />
      <Route path="swarm" element={<SwarmWorkspaceListPage />} />
      <Route path="settings" element={<SettingsPage />} />
    </Route>
    {/* Full-screen routes (outside AppShell) */}
    <Route path="agents/:agentId/workflow" element={<WorkflowEditorPage />} />
  </Route>
</Routes>
```

---

## `modules/` — Feature Modules

Each business domain has its own module directory mirroring backend bounded contexts:

```
modules/
├── agent/                     # Agent management
│   ├── api/                   # Module-level API service
│   │   └── agentService.ts
│   ├── pages/                 # Module pages
│   │   └── AgentListPage.tsx
│   └── __tests__/             # Module tests
├── workflow/                  # Workflow editor
│   ├── api/                   # API calls
│   ├── components/            # Module-specific components
│   │   ├── WorkflowNode.tsx
│   │   ├── CustomEdge.tsx
│   │   ├── NodeSelector.tsx
│   │   ├── NodeConfigTabs.tsx
│   │   ├── CanvasToolbar.tsx
│   │   ├── ConditionBranchEditor.tsx
│   │   └── EditorHeader.tsx
│   ├── pages/
│   │   └── WorkflowEditorPage.tsx
│   ├── stores/                # Zustand stores
│   │   └── useEditorStore.ts
│   ├── validation/            # Business validation
│   │   ├── validateConnection.ts
│   │   └── validateWorkflowGraph.ts
│   └── __tests__/
├── chat/                      # Chat conversations
├── knowledge/                 # Knowledge base
├── review/                    # Human review
│   └── pages/
│       ├── ReviewPage.tsx
│       └── ReviewDetailPage.tsx
├── llm-config/                # LLM configuration
├── swarm/                     # Multi-agent swarm
│   ├── hooks/                 # Custom hooks
│   │   ├── useSwarmMessages.ts
│   │   ├── useSwarmWorkspace.ts
│   │   ├── useAgentStream.ts
│   │   └── useUIStream.ts
│   └── pages/
├── dashboard/                 # Dashboard / overview
├── settings/                  # User settings
└── auth/                      # Auth-related UI
```

### Module Internal Structure

Each module follows this convention:

```
{module}/
├── api/                       # API service layer (calls shared/api adapters)
│   └── {module}Service.ts
├── components/                # Module-specific React components
│   └── {ComponentName}.tsx
├── pages/                     # Route-level page components
│   └── {PageName}Page.tsx
├── stores/                    # Zustand state stores (if needed)
│   └── use{Name}Store.ts
├── hooks/                     # Custom React hooks (if needed)
│   └── use{Name}.ts
├── validation/                # Business validation logic (if needed)
│   └── validate{Name}.ts
└── __tests__/                 # Tests for this module
    └── {name}.test.tsx
```

---

## `shared/` — Shared Utilities

Cross-module shared code:

```
shared/
├── api/                       # HTTP client & API adapters
│   ├── client.ts              # Re-exports httpClient as apiClient
│   ├── httpClient.ts          # Axios instance with interceptors
│   ├── response.ts            # ApiResponse<T> type + unwrapResponse()
│   ├── errorMapper.ts         # Maps API errors to NormalizedApiError
│   └── adapters/              # Per-domain API adapters
│       ├── agentAdapter.ts
│       ├── chatAdapter.ts
│       ├── dashboardAdapter.ts
│       ├── metadataAdapter.ts
│       └── __tests__/
├── feedback/                  # User feedback utilities
│   ├── toast.ts               # Toast notification system
│   └── __tests__/
└── theme/                     # Theme utilities
```

### API Layer Architecture

```
Module Page/Component
       ↓
Module API Service (modules/{mod}/api/{mod}Service.ts)
       ↓
Shared API Adapter (shared/api/adapters/{mod}Adapter.ts)
       ↓
HTTP Client (shared/api/httpClient.ts) — Axios w/ interceptors
       ↓
Backend REST API
```

---

## Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Page component | `PascalCase` + `Page` suffix | `AgentListPage.tsx`, `LoginPage.tsx` |
| UI component | `PascalCase` | `WorkflowNode.tsx`, `NodeSelector.tsx` |
| Custom hook | `camelCase` with `use` prefix | `useEditorStore.ts`, `useSwarmMessages.ts` |
| API service | `camelCase` + `Service` suffix | `agentService.ts` |
| API adapter | `camelCase` + `Adapter` suffix | `agentAdapter.ts`, `chatAdapter.ts` |
| Store | `use` + `PascalCase` + `Store` | `useEditorStore.ts` |
| Test file | `{name}.test.tsx` / `{name}.test.ts` | `agent.create-to-workflow.test.tsx` |
| Validation | `validate` + `PascalCase` | `validateConnection.ts` |
| Constants | `UPPER_SNAKE_CASE` | `AVATAR_COLORS`, `TOAST_DURATION` |

---

## Adding a New Feature Module

1. Create `modules/{feature-name}/` directory
2. Add `api/`, `pages/`, and optionally `components/`, `stores/`, `hooks/`
3. Create API adapter in `shared/api/adapters/{feature}Adapter.ts`
4. Add route in `app/router.tsx`
5. Add navigation in `app/AppShell.tsx` sidebar

---

## Examples

| Well-structured module | Notable patterns |
|------------------------|-----------------|
| `modules/agent/` | Simple module: api → page |
| `modules/workflow/` | Complex module: components, stores, validation |
| `modules/swarm/` | Multiple custom hooks for SSE streaming |
| `shared/api/` | Layered API architecture with adapters |
