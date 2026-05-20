# Frontend Directory Structure

This spec describes the current frontend application under `ai-agent-foward/`.
Use it as the source of truth for file placement.

## Actual Layout

The active frontend source tree is feature-module based, with shared app
infrastructure split out of business modules.

```text
ai-agent-foward/
  package.json
  package-lock.json
  vite.config.ts
  tsconfig.json
  tsconfig.app.json
  eslint.config.js
  tailwind.config.ts
  src/
    App.tsx
    App.test.tsx
    index.css
    main.tsx
    vite-env.d.ts
    app/
    lib/
    modules/
    shared/
    test/
  e2e/
```

The important top-level `src/` directories are:

```text
src/app
src/lib
src/modules
src/shared
src/test
```

## `src/app`

`src/app` owns application bootstrapping, routing, shell layout, auth guarding,
and app-level pages.

Observed files:

- `src/app/App.tsx`
- `src/app/router.tsx`
- `src/app/AppShell.tsx`
- `src/app/AuthGuard.tsx`
- `src/app/auth.ts`
- `src/app/boot.ts`
- `src/app/pages/LoginPage.tsx`
- `src/app/pages/RegisterPage.tsx`
- `src/app/pages/ForgotPasswordPage.tsx`
- `src/app/pages/DashboardPage.tsx`
- `src/app/pages/NotFoundPage.tsx`
- `src/app/components/WorkflowAnimation.tsx`

Routing is centralized in `src/app/router.tsx`. Business pages are imported
from `src/modules/*/pages/*`, while auth and shell-level pages live in
`src/app/pages`.

`src/app/App.tsx` wraps the router with Ant Design `ConfigProvider`, sets
the locale to `zh_CN`, and configures the primary token color.

`src/app/auth.ts` owns lightweight token persistence helpers. Shared API code
also reads the same `accessToken` keys when setting request headers.

## `src/lib`

`src/lib` is currently small and general-purpose.

Observed file:

- `src/lib/utils.ts`

Use this directory only for generic utilities that are not bound to one
business module and are not specifically API, theme, or feedback infrastructure.
Search for existing helpers before adding anything here.

## `src/modules`

`src/modules` is the business feature area. Each module groups pages, API
facades, components, hooks, stores, types, validation, styles, and tests close
to the feature when those files are feature-specific.

Observed module directories:

- `src/modules/agent`
- `src/modules/auth`
- `src/modules/chat`
- `src/modules/dashboard`
- `src/modules/knowledge`
- `src/modules/llm-config`
- `src/modules/mcp`
- `src/modules/review`
- `src/modules/settings`
- `src/modules/swarm`
- `src/modules/workflow`

Observed module subdirectories include:

- `api/` for module-level service facades, such as `workflow/api/workflowService.ts`.
- `pages/` for route targets, such as `chat/pages/ChatPage.tsx`.
- `components/` for feature-specific components, such as workflow node editors.
- `hooks/` for feature-specific React hooks, such as `swarm/hooks/useUIStream.ts`.
- `stores/` for feature-specific Zustand stores, such as `workflow/stores/useEditorStore.ts`.
- `validation/` for feature-specific validation, such as workflow graph checks.
- `types/` for feature-specific types, such as swarm type declarations.
- `styles/` for feature-specific styling constants, such as swarm colors.
- `__tests__/` near the module that owns the behavior under test.

If a module has an `AGENT.md`, read it before changing that module. The module
AGENT files record cross-layer context and known API documents.

## `src/shared`

`src/shared` owns frontend infrastructure that is reused across modules.

Observed shared directories:

- `src/shared/api`
- `src/shared/api/adapters`
- `src/shared/feedback`
- `src/shared/theme`

`src/shared/api/httpClient.ts` builds the Axios client, injects auth headers,
maps errors, and handles auth expiry redirects.

`src/shared/api/adapters/*Adapter.ts` files expose typed API functions for
backend domains. Modules should import these adapters or wrap them in a
module-level service when additional UI mapping is needed.

`src/shared/api/response.ts` defines `ApiResponse<T>` and `unwrapResponse`.
Most endpoints returning the standard backend envelope should use this helper.

`src/shared/feedback/toast.ts` and `src/shared/theme/defaultTheme.ts` are
shared UI infrastructure, not business module files.

## `src/test`

`src/test/setup.ts` configures the Vitest/jsdom environment. It imports
`@testing-library/jest-dom`, runs React Testing Library cleanup after each test,
mocks `matchMedia`, provides a `ResizeObserver`, makes `scrollIntoView` safe,
and handles canvas `getContext` behavior for tests.

Module tests generally live near their owning module in `__tests__/` folders.

Observed test locations:

- `src/app/__tests__`
- `src/modules/*/__tests__`
- `src/modules/*/api/__tests__`
- `src/modules/*/components/__tests__`
- `src/modules/*/stores/__tests__`
- `src/modules/*/validation/__tests__`
- `e2e/phase5-gates.spec.ts`

## Entry Files

`src/main.tsx` is the browser entry.

`src/App.tsx` also exists at the source root. Treat `src/app/App.tsx` as the
current app-level implementation that the router stack uses; inspect imports
before changing either root-level or `app/` app files.

`src/index.css` imports Tailwind layers and defines shared markdown and swarm
CSS utilities.

## Stale `CLAUDE.md` Notice

`CLAUDE.md` currently claims the frontend uses this flat layout:

```text
src/components
src/hooks
src/pages
src/services
src/stores
src/styles
src/types
src/index.tsx
```

That is not the current `ai-agent-foward/src/` structure. The current tree is
`app/`, `lib/`, `modules/`, `shared/`, and `test/`, with feature modules under
`src/modules`.

`CLAUDE.md` also lists older frontend dependency versions. The current
`package.json` and `package-lock.json` must be checked before documenting
dependency versions.

## Placement Rules

1. Put route-level business pages under `src/modules/<module>/pages/`.
2. Put shell, auth route, and global routing files under `src/app/`.
3. Put shared API clients, adapters, response helpers, theme, and feedback
   utilities under `src/shared/`.
4. Put feature-specific API mapping under `src/modules/<module>/api/` only when
   it transforms shared adapter DTOs for the feature UI.
5. Put reusable generic helpers in `src/lib/` only after confirming they are not
   API, theme, feedback, or module-specific logic.
6. Put Zustand stores under `src/modules/<module>/stores/` unless state is
   genuinely shared across modules.
7. Keep tests close to the owner of the behavior.
8. Do not create a flat `components/hooks/pages/services/stores` structure.
