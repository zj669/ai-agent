# Frontend Quality Guidelines

This spec defines frontend quality rules for `ai-agent-foward/`.

## Tooling Facts

Current package scripts in `ai-agent-foward/package.json`:

```json
{
  "dev": "vite",
  "build": "tsc -b && vite build",
  "preview": "vite preview",
  "test": "vitest run",
  "test:watch": "vitest",
  "test:e2e": "playwright test",
  "lint": "eslint .",
  "typecheck": "tsc --noEmit"
}
```

Current TypeScript config is split:

- `tsconfig.json` references `tsconfig.app.json` and `tsconfig.node.json`.
- `tsconfig.app.json` includes `src`.
- `tsconfig.node.json` includes `vite.config.ts`.

## TypeScript Strictness

`tsconfig.app.json` enables:

- `strict: true`
- `isolatedModules: true`
- `noUnusedLocals: true`
- `noUnusedParameters: true`
- `noFallthroughCasesInSwitch: true`
- `resolveJsonModule: true`
- `moduleResolution: Bundler`
- `jsx: react-jsx`

All frontend code should compile under these settings. Do not relax strictness
for a feature-local shortcut.

## Type Rules

Use explicit types at boundaries:

1. API request payloads.
2. API response DTOs.
3. Stream event payloads.
4. Form values.
5. React Flow node data.
6. Store state and actions.
7. Route params after parsing.

Observed good examples:

- `AgentSummary`, `AgentDetail`, and payload interfaces in `agentAdapter.ts`.
- `MessageDTO`, `ThoughtStepDTO`, and `ExecutionData` in `chatAdapter.ts`.
- `EditorState` in `useEditorStore.ts`.
- `WorkflowDetail` and `SaveWorkflowInput` in `workflowService.ts`.
- `StartExecutionHandlers` and `StartExecutionEvent` in `chatService.ts`.

## Avoid `any`

Do not introduce `any` for convenience.

Allowed alternatives:

- `unknown` for untrusted data before narrowing.
- `Record<string, unknown>` for dynamic payload objects.
- Specific DTO interfaces for backend responses.
- Union types for known status and event names.
- Type guards for runtime narrowing.

Existing code still has some `as Record<string, unknown>` casts around dynamic
workflow payloads. New code should narrow values close to where they enter the
frontend and avoid spreading casts through components.

## Boundary Validation

Validate data at boundaries:

1. Backend API responses.
2. JSON strings such as `graphJson`.
3. SSE payloads.
4. Route params.
5. Form submissions.
6. React Flow graph serialization/deserialization.

Observed current patterns:

- `workflowService.ts` parses `graphJson` in `parseGraphJson` and returns
  `null` on parse failure.
- `chatService.ts` parses SSE `data:` JSON and falls back to `{ raw }`.
- `WorkflowEditorPage.tsx` normalizes workflow graph nodes before saving.
- `validateWorkflowGraph.ts` checks missing/invalid edges.
- `validateConnection.ts` rejects self-loops.
- AntD `Form.Item` rules validate login and registration inputs.

## Zod Status And Policy

`zod` is not currently present in `ai-agent-foward/package.json`, and no direct
usage was found under `ai-agent-foward/src`.

Do not claim the current source already uses Zod.

If a future task adds Zod, use it at boundaries rather than inside every
component:

1. Parse backend DTOs when an endpoint returns dynamic or weakly typed payloads.
2. Parse persisted local/session storage values before using them.
3. Parse SSE JSON payloads before dispatching callbacks.
4. Parse route/search params when a page depends on structured values.
5. Parse workflow graph JSON before mapping to React Flow nodes.

Keep Zod schemas close to the boundary owner:

- Shared API response schemas under `src/shared/api` when reused.
- Module-specific schemas under `src/modules/<module>/validation` or `api`.
- Form schemas near the page/hook that owns the form unless reused.

Do not duplicate Zod schemas and TypeScript interfaces manually. Infer types
from schemas where Zod is the source of truth.

## Form Validation

The current frontend uses AntD Form, not `react-hook-form`.

Observed files:

- `src/app/pages/LoginPage.tsx`
- `src/app/pages/RegisterPage.tsx`

Current pattern:

- Use `Form.useForm`.
- Use `layout="vertical"`.
- Use `requiredMark={false}`.
- Attach validation rules to `Form.Item`.
- Use `onFinish` or explicit `form.validateFields()`.
- Show submit errors through AntD `message`.

Do not document `react-hook-form` as current implementation.

## React Hook Form Policy

`react-hook-form` is not currently present in `ai-agent-foward/package.json`,
and no direct usage was found under `ai-agent-foward/src`.

If a future task introduces `react-hook-form`, use it only when it solves a
real problem that AntD Form is not handling well, such as highly dynamic forms
with complex field arrays or shared schema resolver integration.

When using it with AntD in the future:

1. Keep one form ownership model per form.
2. Do not mix AntD Form state and react-hook-form state for the same fields.
3. Use a resolver when paired with Zod.
4. Keep submit DTO mapping explicit.
5. Add tests for validation, default values, and submit payload mapping.

Until the dependency is actually added, prefer existing AntD Form conventions.

## Runtime Parsing

Use safe parsing for dynamic values.

Examples to follow:

- `parseGraphJson` catches JSON parse errors.
- `parseSseBlock` catches SSE JSON parse errors.
- `getSavedUserInfo` catches local storage JSON parse errors.
- `useUIStream` catches malformed task notification payloads.

When catching parse errors, return a safe fallback and keep UI behavior stable.
Do not let malformed backend or storage data crash the entire route.

## Tests

Vitest and React Testing Library are configured.

Observed setup:

- `src/test/setup.ts`
- `@testing-library/jest-dom`
- `cleanup()` after each test
- `matchMedia` mock
- `ResizeObserver` mock
- `scrollIntoView` no-op
- canvas `getContext` handling

Observed test placement:

- App/router tests under `src/app/__tests__`
- Module behavior tests under `src/modules/<module>/__tests__`
- API tests under `src/modules/<module>/api/__tests__`
- Component tests under `src/modules/<module>/components/__tests__`
- Store tests under `src/modules/<module>/stores/__tests__`
- Validation tests under `src/modules/<module>/validation/__tests__`

Add tests close to the code being changed.

## What To Test

For API adapters:

- Request path.
- Request params/body.
- Envelope unwrapping.
- Raw response exceptions.
- Error propagation when relevant.

For SSE parsers:

- Event name parsing.
- Multi-line data.
- JSON fallback.
- Finish behavior.
- Error event behavior.
- Abort/cleanup behavior.

For stores:

- Initial state.
- Each action.
- Reset behavior.
- Dirty state transitions.

For components:

- User-visible behavior.
- Important callbacks.
- Form validation.
- Route navigation.
- Integration with mocked shared adapters.

For workflow canvas:

- Connection validation.
- Dirty marking.
- Node/edge callbacks.
- Save/publish validation.
- React Flow mocks matching the required callback surface.

## Linting

`eslint.config.js` uses:

- `@eslint/js`
- `typescript-eslint`
- `eslint-plugin-react-hooks`
- `eslint-plugin-react-refresh`
- browser globals

React Hooks recommended rules are enabled. Do not suppress hook dependency
warnings without a narrow reason and a local comment.

## Build And Typecheck

The build script runs `tsc -b` before `vite build`. A change that passes Vite
but fails TypeScript is not acceptable.

Use `npm run typecheck` for type-only verification and `npm run build` when the
change can affect bundling, Vite config, CSS imports, or generated output.

## Error Handling

Feature code should catch `unknown` errors and map to user-facing text.

Observed pattern:

```ts
} catch (err: unknown) {
  const msg = err instanceof Error ? err.message : '登录失败，请重试'
  message.error(msg)
}
```

Use normalized API errors from the shared client where possible. Avoid assuming
`err.response.data.message` exists in component code.

## Storage

Access token storage keys are shared:

- `localStorage.accessToken`
- `sessionStorage.accessToken`

User info is stored as:

- `localStorage.userInfo`

Remembered login email is stored as:

- `localStorage.rememberedEmail`

Parse storage values defensively. Clear both token stores on auth expiry or
logout.

## Anti-Patterns

Avoid these patterns:

1. Relaxing TypeScript strictness.
2. Casting API responses to `any`.
3. Putting raw backend DTOs directly into deep UI components when mapping is
   needed.
4. Duplicating token handling across normal Axios adapters.
5. Forgetting cleanup for timers, readers, or EventSource connections.
6. Swallowing stream errors without surfacing callback or UI state.
7. Mixing AntD Form with another form state library for the same form.
8. Adding Zod or react-hook-form docs as if they already exist in source.
9. Adding broad global CSS for a single module.
10. Moving tests away from their owning module.
11. Ignoring `noUnusedLocals` and `noUnusedParameters`.
12. Parsing route params without checking `Number.isFinite` when numbers are
    required.
13. Hard-coding backend host names instead of using Vite proxy paths.
