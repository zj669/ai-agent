# Frontend Component Guidelines

The frontend is a React 19, Vite, Ant Design, Tailwind, and `@xyflow/react`
application organized around feature modules.

## Component Placement

Put components where their ownership is clearest.

Use `src/app` for app shell and route infrastructure:

- `src/app/App.tsx`
- `src/app/AppShell.tsx`
- `src/app/AuthGuard.tsx`
- `src/app/router.tsx`
- `src/app/pages/LoginPage.tsx`
- `src/app/pages/RegisterPage.tsx`
- `src/app/components/WorkflowAnimation.tsx`

Use `src/modules/<module>` for business feature UI:

- `src/modules/workflow/pages/WorkflowEditorPage.tsx`
- `src/modules/workflow/components/WorkflowNode.tsx`
- `src/modules/chat/pages/ChatPage.tsx`
- `src/modules/swarm/pages/SwarmMainPage.tsx`
- `src/modules/mcp/components/*`

Use `src/shared` for shared infrastructure, not for feature-specific widgets.

## Naming

Components use PascalCase.

Examples:

- `WorkflowEditorPage`
- `WorkflowNode`
- `NodeSelector`
- `NodeConfigTabs`
- `CanvasToolbar`
- `LoginPage`
- `RegisterPage`
- `SwarmMainPage`

The file name should match the default exported component when a file's primary
purpose is one component. Example:

```ts
function WorkflowEditorPage() {
  ...
}

export default WorkflowEditorPage
```

Avoid files named `index.tsx` for major route pages because they make search
results and stack traces less useful.

## Page Components

Route targets should end in `Page`.

Examples:

- `LoginPage.tsx`
- `RegisterPage.tsx`
- `DashboardPage.tsx`
- `WorkflowEditorPage.tsx`
- `ReviewDetailPage.tsx`

Route wiring belongs in `src/app/router.tsx`. Feature pages should not create
their own top-level routers.

## Hooks

Hook names must start with `use`.

Observed examples:

- `useEditorStore` in `workflow/stores/useEditorStore.ts`
- `useUIStream` in `swarm/hooks/useUIStream.ts`
- `useAgentStream` in `swarm/hooks/useUIStream.ts`

Place feature hooks under `src/modules/<module>/hooks/` unless they are
framework-level shared hooks. Keep hooks focused on behavior and side effects;
rendering should remain in components.

When a hook subscribes to an external resource, it must clean up in `useEffect`.
`useUIStream` and `useAgentStream` close EventSource instances in cleanup.

## Ant Design Usage

Ant Design is the primary UI component library.

Observed usage:

- `src/app/App.tsx` wraps the app in `ConfigProvider`.
- `LoginPage.tsx` uses `Form`, `Input`, `Button`, `Checkbox`, `Typography`,
  `message`, and Ant Design icons.
- `RegisterPage.tsx` uses `Form`, `Input`, `Button`, `Typography`, `message`,
  `Steps`, and icons.

Use AntD for standard controls:

- Forms
- Inputs
- Buttons
- Checkboxes
- Steps
- Typography
- Feedback messages
- Icons from `@ant-design/icons`

Use the existing `ConfigProvider` theme token instead of local color systems for
basic AntD primary behavior. Current primary token is `#1677ff`.

## AntD Form Pattern

The current code uses AntD Form, not `react-hook-form`.

Observed pattern in `LoginPage.tsx`:

```ts
const [form] = Form.useForm<LoginFormValues>()

<Form
  form={form}
  onFinish={handleFinish}
  initialValues={{ ... }}
  size="large"
  layout="vertical"
  requiredMark={false}
>
```

Observed validation uses `rules` on `Form.Item`.

Use typed form values where practical. For quick pages that have not yet been
typed, prefer adding a local form value interface before wiring more fields.

## When To Wrap AntD Components

Wrap AntD components only when one of these is true:

1. The wrapper enforces a project-wide convention used in several modules.
2. The wrapper hides a backend or domain-specific mapping.
3. The wrapper adds tested behavior that would otherwise be duplicated.
4. The wrapper composes several AntD primitives into one reusable feature unit.

Do not wrap AntD just to rename props or add one-off styling.

Feature-specific wrappers should live in the owning module. Shared wrappers
should live under `src/shared` only after at least two modules need them.

## Tailwind And AntD Coexistence

Tailwind is enabled through `src/index.css` and `tailwind.config.ts`.

Observed Tailwind usage:

- `WorkflowEditorPage.tsx` uses layout utility classes such as
  `flex h-screen flex-col bg-slate-50`.
- React Flow controls and minimap use AntD-like override classes with `!`.
- `src/index.css` imports Tailwind base, components, and utilities.

Use Tailwind for:

- Page layout
- Spacing
- Flex/grid utilities
- Lightweight text and border utilities
- Feature-specific canvas/layout framing

Use AntD tokens and component props for:

- Standard control sizing
- Form layout and validation
- Button intent
- Message feedback
- Modal/table/form conventions when introduced

Avoid fighting AntD internal styles with large blocks of `!important` utilities.
Small targeted overrides are acceptable when integrating with third-party
surfaces such as React Flow.

## Global CSS

`src/index.css` currently owns:

- Tailwind imports.
- Shared markdown body styles for Swarm and Chat.
- Highlight.js code block overrides.
- Swarm message animations.
- Streaming cursor styles.
- CSS variables for background, foreground, and muted foreground.
- Base body font and background.

Do not put module-specific page layout rules into global CSS unless the style is
intentionally shared across modules.

## `@xyflow/react` Canvas Wiring

The workflow canvas is implemented in:

- `src/modules/workflow/pages/WorkflowEditorPage.tsx`

Observed imports include:

- `ReactFlow`
- `Controls`
- `MiniMap`
- `Background`
- `BackgroundVariant`
- `addEdge`
- `useNodesState`
- `useEdgesState`
- `type ReactFlowInstance`

The page owns canvas state:

```ts
const [nodes, setNodes, onNodesChange] = useNodesState(INITIAL_NODES)
const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
const [rfInstance, setRfInstance] = useState<ReactFlowInstance | null>(null)
```

The rendered canvas passes custom node and edge types:

```tsx
<ReactFlow
  nodes={nodes}
  edges={edges}
  onNodesChange={handleNodesChange}
  onEdgesChange={handleEdgesChange}
  onConnect={onConnect}
  onInit={setRfInstance}
  onDragOver={onDragOver}
  onDrop={onDrop}
  nodeTypes={nodeTypes}
  edgeTypes={edgeTypes}
  defaultEdgeOptions={{ type: 'custom' }}
  connectionLineComponent={CustomConnectionLine}
  fitView
>
```

Keep React Flow state close to the canvas. Use Zustand for shared editor
metadata and UI flags, not for every node drag event.

## Canvas Components

Workflow canvas components live under:

- `src/modules/workflow/components/`

Observed components:

- `WorkflowNode`
- `NodeHandle`
- `CustomEdge`
- `CustomConnectionLine`
- `CanvasToolbar`
- `NodeSelector`
- `NodeConfigTabs`
- `FieldRenderer`
- `ConditionBranchEditor`
- `AgentConfigPanel`
- `EditorHeader`

Canvas-specific components may import from `@xyflow/react`. Non-canvas business
components should not depend on React Flow types.

## Props

Prefer explicit prop interfaces for components with non-trivial props.

Rules:

1. Name the props interface `<ComponentName>Props`.
2. Keep callback names action-oriented, such as `onSave`, `onPublish`,
   `onToggle`, `onChange`.
3. Avoid passing large store objects as props.
4. Avoid accepting `any` for node data, API data, or form values.
5. Keep backend DTOs out of leaf components when the UI needs a mapped shape.

## Icons

The app already depends on `@ant-design/icons`. Use it for common UI icons
inside AntD surfaces.

Observed examples:

- `LockOutlined`
- `MailOutlined`
- `RobotOutlined`
- `UserOutlined`
- `SafetyOutlined`

Do not add a second icon library without a concrete need.

## Tests For Components

Component tests are colocated near owners.

Observed tests:

- `workflow/components/__tests__/WorkflowNode.test.tsx`
- `workflow/components/__tests__/CanvasToolbar.test.tsx`
- `workflow/components/__tests__/CustomEdge.test.tsx`
- `workflow/__tests__/workflow.editor-interaction.test.tsx`
- `auth/__tests__/authRoutes.test.tsx`
- `dashboard/__tests__/dashboard.new-agent-route.test.tsx`

When touching React Flow components, tests often mock `@xyflow/react`. Keep
those mocks aligned with the callbacks used by the component.

## Component Anti-Patterns

Avoid these patterns:

1. Creating a new flat `src/components` directory for feature UI.
2. Mixing route definitions inside module page components.
3. Adding one-off global CSS for module-only presentation.
4. Wrapping AntD components without repeated usage or behavior.
5. Passing store objects deep through props.
6. Using `any` for React Flow node data when a specific shape exists.
7. Keeping EventSource or stream parsing logic inside visual components.
8. Importing backend adapters directly into deeply nested leaf components.
9. Duplicating AntD validation rules across pages when a shared validator is
   warranted.
10. Replacing AntD controls with raw HTML controls for standard form behavior.
