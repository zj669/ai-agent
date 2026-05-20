# Frontend State Management

The active frontend uses Zustand for feature-level shared UI state and React
local state for component-local interaction state.

## Current Store Evidence

The real store example is:

- `ai-agent-foward/src/modules/workflow/stores/useEditorStore.ts`

It imports `create` from `zustand`, defines an `EditorState` interface, defines
an `initialState` object, and exports `useEditorStore`.

GitNexus also identifies callers from:

- `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`
- `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx`

The local ABCoder AST product at `~/abcoder-asts/ai-agent-foward.json` includes
the same symbol path:

- `ai-agent-foward?src/modules/workflow/stores/useEditorStore.ts#useEditorStore`

## Store Shape Pattern

The observed store shape is:

```ts
interface EditorState {
  agentName: string
  agentDescription: string
  agentIcon: string
  version: number | null
  isDirty: boolean
  expandedNodeId: string
  operationState: 'idle' | 'saving' | 'publishing'
  nodeTemplates: NodeTemplateDTO[]
  panelCollapsed: boolean
  setAgentInfo: (...) => void
  markDirty: () => void
  markClean: () => void
  toggleNodeExpand: (nodeId: string) => void
  setOperationState: (...) => void
  setNodeTemplates: (templates: NodeTemplateDTO[]) => void
  togglePanel: () => void
  reset: () => void
}
```

The implementation then exports:

```ts
export const useEditorStore = create<EditorState>((set) => ({
  ...initialState,
  setAgentInfo: (info) => set((s) => ({ ...s, ...info })),
  markDirty: () => set({ isDirty: true }),
  markClean: () => set({ isDirty: false }),
  setOperationState: (operationState) => set({ operationState }),
  reset: () => set(initialState),
}))
```

This is the preferred pattern for feature stores:

1. Define a narrow state interface in the same file.
2. Keep serializable state fields explicit.
3. Define actions beside the state they mutate.
4. Put an `initialState` constant before `create`.
5. Export one hook named `use<Feature>Store`.
6. Keep backend DTO imports typed with `type` imports when they are type-only.

## Naming Convention

Store hooks use the `useXStore` naming pattern.

Examples and expected names:

- `workflow/stores/useEditorStore.ts` exports `useEditorStore`.
- A chat store would be `chat/stores/useChatStore.ts`.
- A swarm workspace store would be `swarm/stores/useWorkspaceStore.ts`.

The file name should match the exported store hook. Avoid generic names such
as `store.ts`, `state.ts`, or `useStore.ts` inside modules because they make
imports ambiguous.

## When To Use A Store

Use a Zustand store when state meets at least one of these conditions:

1. Multiple components in one feature need the same state.
2. A page and nested components need to update the same flags or metadata.
3. The state represents UI workflow state, such as dirty flags, operation mode,
   selected/expanded node, node templates, panel collapse state, or draft
   entity metadata.
4. Passing the same state through several component levels would create noisy
   prop drilling.
5. The state needs shared actions so callers cannot mutate it inconsistently.

`useEditorStore` is appropriate because `WorkflowEditorPage`, `WorkflowNode`,
and editor components coordinate the same agent metadata, dirty state, operation
state, node templates, and panel state.

## When To Use Local Component State

Use React `useState`, `useRef`, `useMemo`, and `useCallback` for state that is
owned by one component instance.

Observed examples in `WorkflowEditorPage.tsx`:

- `nodes` and `edges` are owned by `useNodesState` and `useEdgesState` from
  `@xyflow/react`.
- `loadMessage`, `saveMessage`, `publishMessage`, and `connectError` are local
  page messages.
- `reactFlowWrapper` and `rfInstance` are local canvas references.

Observed examples in auth pages:

- `LoginPage.tsx` keeps `loading` and an AntD `Form` instance locally.
- `RegisterPage.tsx` keeps `step`, `email`, `countdown`, `loading`, and timer
  refs locally.

Do not promote local state into Zustand just because it is used in one large
component. Promote it only when another component needs to read or write it, or
when a shared action prevents inconsistent updates.

## Store Actions

Actions should describe domain/UI intent instead of exposing raw setters for
every primitive.

Good examples:

- `setAgentInfo`
- `markDirty`
- `markClean`
- `toggleNodeExpand`
- `setOperationState`
- `setNodeTemplates`
- `togglePanel`
- `reset`

Use a raw `setX` action when the value is simple and no invariant is involved.
Use named intent actions when the action has workflow meaning.

## Reset Behavior

Keep `initialState` separate from the `create` call and use it in `reset`.

The existing `reset: () => set(initialState)` pattern is concise and keeps the
store reset behavior aligned with default values. If future state contains
objects or arrays that can be mutated by consumers, clone them on reset instead
of sharing mutable references.

## Derived State

Prefer deriving short-lived values in components with `useMemo` when the derived
value is only used by that component.

Observed examples:

- `validationNodes` derives node ids from React Flow nodes.
- `validationEdges` derives connection endpoints from React Flow edges.
- `errorBanner` derives display state from local message strings.

Do not store derived values in Zustand unless multiple components need the same
derived value and recomputing it creates real complexity.

## Async State

Keep async calls in pages, hooks, or module service files, not inside Zustand
actions, unless the store is explicitly designed as an async resource store.

Observed workflow pattern:

1. `WorkflowEditorPage` calls `fetchWorkflowDetail` and `fetchNodeTemplates`.
2. The page writes loaded templates and agent metadata into `useEditorStore`.
3. The page keeps loading and error messages locally.

This keeps API concerns in service/page code and keeps the store focused on
state transitions.

## Selectors

The current `WorkflowEditorPage` uses `const store = useEditorStore()`, which
subscribes to the whole store. For smaller components, prefer selectors when a
component only needs one or two fields:

```ts
const isDirty = useEditorStore((s) => s.isDirty)
const markDirty = useEditorStore((s) => s.markDirty)
```

Selectors reduce unnecessary re-renders and make component dependencies clear.
Use them especially in repeated node components.

## Testing Stores

Store tests live near the store:

- `ai-agent-foward/src/modules/workflow/stores/__tests__/useEditorStore.test.ts`

Tests should cover:

1. Initial state.
2. Each action that mutates state.
3. Reset behavior.
4. Any invariant, such as dirty state or operation mode transitions.

Reset the store between tests so state does not leak across cases.

## Anti-Patterns

Avoid these patterns:

1. Creating one global store for unrelated modules.
2. Storing server DTOs without mapping when UI needs a different shape.
3. Storing values that can be derived cheaply from other state.
4. Mutating arrays or objects in place inside store actions.
5. Exposing the Zustand `set` function outside the store file.
6. Mixing API calls, routing side effects, and state mutation in one action
   without a clear reason.
7. Naming every action `setState`.
8. Duplicating the same state in React local state and Zustand.
9. Using stores to bypass the route/module ownership boundaries.
10. Adding persistent storage middleware without documenting token/privacy
    implications.
