# 工作流编辑器水平流向 + 多 Handle 分支节点 实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将工作流编辑器从垂直布局改为水平（左→右）布局，CONDITION 节点支持每个分支独立输出 Handle，连线使用自定义贝塞尔曲线。

**Architecture:** 参照 Dify 的实现模式 — Handle 位置 Left/Right 驱动水平流向，自定义 Edge 使用 `getBezierPath` + `curvature: 0.16`，CONDITION 节点遍历 `branches` 数组渲染多个 `NodeSourceHandle`。

**Tech Stack:** React 19, @xyflow/react 12, TypeScript 5.8, Tailwind CSS 4, Vitest

**Design Doc:** `docs/plans/2026-02-21-workflow-horizontal-flow-design.md`

---

### Task 1: 创建 NodeHandle 组件

**Files:**
- Create: `ai-agent-foward/src/modules/workflow/components/NodeHandle.tsx`
- Test: `ai-agent-foward/src/modules/workflow/components/__tests__/NodeHandle.test.tsx`

**Step 1: Write the failing test**

```tsx
// NodeHandle.test.tsx
import { render } from '@testing-library/react'
import { vi } from 'vitest'

vi.mock('@xyflow/react', () => ({
  Handle: (props: Record<string, unknown>) => (
    <div data-testid={`handle-${props.id}`} data-type={props.type} data-position={props.position} />
  ),
  Position: { Left: 'left', Right: 'right' },
}))

const { NodeTargetHandle, NodeSourceHandle } = await import('../NodeHandle')

describe('NodeHandle', () => {
  it('NodeTargetHandle renders left-side target handle', () => {
    const { getByTestId } = render(<NodeTargetHandle handleId="target" />)
    const el = getByTestId('handle-target')
    expect(el.dataset.type).toBe('target')
    expect(el.dataset.position).toBe('left')
  })

  it('NodeSourceHandle renders right-side source handle', () => {
    const { getByTestId } = render(<NodeSourceHandle handleId="source" />)
    const el = getByTestId('handle-source')
    expect(el.dataset.type).toBe('source')
    expect(el.dataset.position).toBe('right')
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/NodeHandle.test.tsx`
Expected: FAIL — module not found

**Step 3: Write minimal implementation**

```tsx
// NodeHandle.tsx
import { memo } from 'react'
import { Handle, Position } from '@xyflow/react'
import { cn } from '../../../lib/utils'

const handleBase = '!h-4 !w-4 !rounded-none !border-none !bg-transparent !outline-none'

export const NodeTargetHandle = memo(({ handleId, className }: { handleId: string; className?: string }) => (
  <Handle
    id={handleId}
    type="target"
    position={Position.Left}
    className={cn(
      handleBase,
      'after:absolute after:left-1.5 after:top-1 after:h-2 after:w-0.5 after:rounded-full after:bg-blue-500',
      className,
    )}
  />
))
NodeTargetHandle.displayName = 'NodeTargetHandle'

export const NodeSourceHandle = memo(({ handleId, className }: { handleId: string; className?: string }) => (
  <Handle
    id={handleId}
    type="source"
    position={Position.Right}
    className={cn(
      handleBase,
      'after:absolute after:right-1.5 after:top-1 after:h-2 after:w-0.5 after:rounded-full after:bg-blue-500',
      className,
    )}
  />
))
NodeSourceHandle.displayName = 'NodeSourceHandle'
```

**Step 4: Run test to verify it passes**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/NodeHandle.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add ai-agent-foward/src/modules/workflow/components/NodeHandle.tsx ai-agent-foward/src/modules/workflow/components/__tests__/NodeHandle.test.tsx
git commit -m "feat(workflow): add NodeTargetHandle and NodeSourceHandle components"
```

---

### Task 2: 创建 CustomEdge 组件

**Files:**
- Create: `ai-agent-foward/src/modules/workflow/components/CustomEdge.tsx`
- Test: `ai-agent-foward/src/modules/workflow/components/__tests__/CustomEdge.test.tsx`

**Step 1: Write the failing test**

```tsx
// CustomEdge.test.tsx
import { render } from '@testing-library/react'
import { vi } from 'vitest'

const mockGetBezierPath = vi.fn().mockReturnValue(['M 0 0 C 50 0 50 100 100 100', 50, 50])

vi.mock('@xyflow/react', () => ({
  getBezierPath: (...args: unknown[]) => mockGetBezierPath(...args),
  BaseEdge: (props: Record<string, unknown>) => (
    <path data-testid="base-edge" d={props.path as string} style={props.style as Record<string, unknown>} />
  ),
  Position: { Left: 'left', Right: 'right' },
}))

const { default: CustomEdge } = await import('../CustomEdge')

describe('CustomEdge', () => {
  const baseProps = {
    id: 'e1',
    source: 'a',
    target: 'b',
    sourceX: 100,
    sourceY: 200,
    targetX: 300,
    targetY: 200,
    sourcePosition: 'right' as const,
    targetPosition: 'left' as const,
  }

  it('calls getBezierPath with horizontal positions and curvature 0.16', () => {
    render(<svg><CustomEdge {...baseProps} /></svg>)
    expect(mockGetBezierPath).toHaveBeenCalledWith(
      expect.objectContaining({
        sourcePosition: 'right',
        targetPosition: 'left',
        curvature: 0.16,
      })
    )
  })

  it('renders BaseEdge with blue stroke', () => {
    const { getByTestId } = render(<svg><CustomEdge {...baseProps} /></svg>)
    const edge = getByTestId('base-edge')
    expect(edge).toBeInTheDocument()
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/CustomEdge.test.tsx`
Expected: FAIL — module not found

**Step 3: Write minimal implementation**

```tsx
// CustomEdge.tsx
import { memo } from 'react'
import { BaseEdge, getBezierPath, Position, type EdgeProps } from '@xyflow/react'

function CustomEdge({ id, sourceX, sourceY, targetX, targetY }: EdgeProps) {
  const [edgePath] = getBezierPath({
    sourceX: sourceX - 8,
    sourceY,
    sourcePosition: Position.Right,
    targetX: targetX + 8,
    targetY,
    targetPosition: Position.Left,
    curvature: 0.16,
  })

  return (
    <BaseEdge
      id={id}
      path={edgePath}
      style={{ stroke: '#2970FF', strokeWidth: 2 }}
    />
  )
}

export default memo(CustomEdge)
```

**Step 4: Run test to verify it passes**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/CustomEdge.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add ai-agent-foward/src/modules/workflow/components/CustomEdge.tsx ai-agent-foward/src/modules/workflow/components/__tests__/CustomEdge.test.tsx
git commit -m "feat(workflow): add CustomEdge with horizontal bezier curve"
```

---

### Task 3: 创建 CustomConnectionLine 组件

**Files:**
- Create: `ai-agent-foward/src/modules/workflow/components/CustomConnectionLine.tsx`
- Test: `ai-agent-foward/src/modules/workflow/components/__tests__/CustomConnectionLine.test.tsx`

**Step 1: Write the failing test**

```tsx
// CustomConnectionLine.test.tsx
import { render } from '@testing-library/react'
import { vi } from 'vitest'

vi.mock('@xyflow/react', () => ({
  getBezierPath: () => ['M 0 0 C 50 0 50 100 100 100'],
  Position: { Left: 'left', Right: 'right' },
}))

const { default: CustomConnectionLine } = await import('../CustomConnectionLine')

describe('CustomConnectionLine', () => {
  it('renders a path and a target indicator rect', () => {
    const { container } = render(
      <svg>
        <CustomConnectionLine fromX={0} fromY={0} toX={100} toY={100} fromPosition="right" toPosition="left" />
      </svg>
    )
    const path = container.querySelector('path')
    expect(path).toBeInTheDocument()
    expect(path?.getAttribute('stroke')).toBe('#D0D5DD')

    const rect = container.querySelector('rect')
    expect(rect).toBeInTheDocument()
    expect(rect?.getAttribute('fill')).toBe('#2970FF')
  })
})
```

**Step 2: Run test to verify it fails**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/CustomConnectionLine.test.tsx`
Expected: FAIL — module not found

**Step 3: Write minimal implementation**

```tsx
// CustomConnectionLine.tsx
import { memo } from 'react'
import { getBezierPath, Position } from '@xyflow/react'

interface CustomConnectionLineProps {
  fromX: number
  fromY: number
  toX: number
  toY: number
  fromPosition?: string
  toPosition?: string
}

function CustomConnectionLine({ fromX, fromY, toX, toY }: CustomConnectionLineProps) {
  const [edgePath] = getBezierPath({
    sourceX: fromX,
    sourceY: fromY,
    sourcePosition: Position.Right,
    targetX: toX,
    targetY: toY,
    targetPosition: Position.Left,
    curvature: 0.16,
  })

  return (
    <g>
      <path fill="none" stroke="#D0D5DD" strokeWidth={2} d={edgePath} />
      <rect x={toX} y={toY - 4} width={2} height={8} fill="#2970FF" />
    </g>
  )
}

export default memo(CustomConnectionLine)
```

**Step 4: Run test to verify it passes**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/CustomConnectionLine.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add ai-agent-foward/src/modules/workflow/components/CustomConnectionLine.tsx ai-agent-foward/src/modules/workflow/components/__tests__/CustomConnectionLine.test.tsx
git commit -m "feat(workflow): add CustomConnectionLine for drag preview"
```

---

### Task 4: 改造 WorkflowNode — Handle 方向改为 Left/Right

**Files:**
- Modify: `ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx`
- Modify: `ai-agent-foward/src/modules/workflow/components/__tests__/WorkflowNode.test.tsx`

**Step 1: Update the test mock to use Left/Right positions**

在 `WorkflowNode.test.tsx` 中，更新 `@xyflow/react` mock：

```tsx
vi.mock('@xyflow/react', () => ({
  Handle: (props: Record<string, unknown>) => (
    <div data-testid={`handle-${props.id ?? props.type}`} data-type={props.type} data-position={props.position} />
  ),
  Position: { Left: 'left', Right: 'right', Top: 'top', Bottom: 'bottom' },
}))
```

添加新测试：

```tsx
it('renders left target handle and right source handle for LLM node', () => {
  const { getByTestId } = render(
    <WorkflowNode id="llm-1" data={{ label: 'LLM 节点', nodeType: 'LLM' }} selected={false} />
  )
  expect(getByTestId('handle-target').dataset.position).toBe('left')
  expect(getByTestId('handle-source').dataset.position).toBe('right')
})

it('CONDITION node renders multiple source handles from branches', () => {
  const data = {
    label: '条件节点',
    nodeType: 'CONDITION',
    branches: [
      { id: 'branch-0', name: '如果' },
      { id: 'else', name: '否则' },
    ],
  }
  const { getByTestId } = render(
    <WorkflowNode id="cond-1" data={data} selected={false} />
  )
  expect(getByTestId('handle-target')).toBeInTheDocument()
  expect(getByTestId('handle-branch-0')).toBeInTheDocument()
  expect(getByTestId('handle-else')).toBeInTheDocument()
})
```

**Step 2: Run test to verify it fails**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/WorkflowNode.test.tsx`
Expected: FAIL — handle positions are still top/bottom, no multi-handle

**Step 3: Update WorkflowNode implementation**

Replace the Handle imports and rendering in `WorkflowNode.tsx`:

1. Replace `import { Handle, Position } from '@xyflow/react'` with:
   ```tsx
   import { NodeTargetHandle, NodeSourceHandle } from './NodeHandle'
   ```

2. Replace the target Handle line (`{!isStart && <Handle type="target" position={Position.Top} ...`):
   ```tsx
   {!isStart && <NodeTargetHandle handleId="target" />}
   ```

3. Replace the source Handle line (`{!isEnd && <Handle type="source" position={Position.Bottom} ...`):
   ```tsx
   {!isEnd && !isCondition && <NodeSourceHandle handleId="source" />}
   ```

4. Add CONDITION branch handles after the expanded config section:
   ```tsx
   {isCondition && (
     <div className="flex flex-col gap-1 border-t border-slate-200 py-2 px-3">
       {(nodeData.branches ?? []).map((branch) => (
         <div key={branch.id} className="relative flex items-center justify-between py-1">
           <span className="text-xs text-slate-600">{branch.name}</span>
           <NodeSourceHandle handleId={branch.id} className="!relative !top-0 !right-0 !translate-x-0 !translate-y-0" />
         </div>
       ))}
     </div>
   )}
   ```

5. Add `isCondition` variable:
   ```tsx
   const isCondition = nodeData.nodeType === 'CONDITION'
   ```

6. Add `Branch` type to `WorkflowNodeData`:
   ```tsx
   export type Branch = { id: string; name: string }

   export type WorkflowNodeData = {
     label: string
     nodeType: WorkflowNodeType
     branches?: Branch[]
     // ... existing fields
   }
   ```

**Step 4: Run test to verify it passes**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/components/__tests__/WorkflowNode.test.tsx`
Expected: PASS

**Step 5: Commit**

```bash
git add ai-agent-foward/src/modules/workflow/components/WorkflowNode.tsx ai-agent-foward/src/modules/workflow/components/__tests__/WorkflowNode.test.tsx
git commit -m "feat(workflow): convert WorkflowNode to horizontal handles with multi-branch support"
```

---

### Task 5: 改造 WorkflowEditorPage — 水平布局 + edgeTypes + connectionLine

**Files:**
- Modify: `ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx`
- Modify: `ai-agent-foward/src/modules/workflow/__tests__/workflow.editor-interaction.test.tsx`

**Step 1: Update the editor interaction test mock**

在 `workflow.editor-interaction.test.tsx` 中，更新 `@xyflow/react` mock 的 Position：

```tsx
Position: { Top: 'top', Bottom: 'bottom', Left: 'left', Right: 'right' },
```

添加新测试：

```tsx
it('INITIAL_NODES 使用水平布局位置', async () => {
  render(<WorkflowEditorPage />)
  // START 和 END 节点应该在同一水平线上（y 相近），x 不同
  const startNode = await screen.findByText('开始节点（START）')
  const endNode = screen.getByText('结束节点（END）')
  expect(startNode).toBeInTheDocument()
  expect(endNode).toBeInTheDocument()
})
```

**Step 2: Run test to verify existing tests still pass**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/__tests__/workflow.editor-interaction.test.tsx`
Expected: PASS (existing tests should still work)

**Step 3: Update WorkflowEditorPage implementation**

1. Add imports at top:
   ```tsx
   import CustomEdge from '../components/CustomEdge'
   import CustomConnectionLine from '../components/CustomConnectionLine'
   ```

2. Add edge types constant after `nodeTypes`:
   ```tsx
   const edgeTypes = { custom: CustomEdge }
   ```

3. Change INITIAL_NODES to horizontal layout:
   ```tsx
   const INITIAL_NODES: Node[] = [
     {
       id: 'start',
       type: 'workflowNode',
       position: { x: 50, y: 250 },
       data: { label: '开始节点', nodeType: 'START' } satisfies WorkflowNodeData,
     },
     {
       id: 'end',
       type: 'workflowNode',
       position: { x: 600, y: 250 },
       data: { label: '结束节点', nodeType: 'END' } satisfies WorkflowNodeData,
     },
   ]
   ```

4. Update `onConnect` to preserve `sourceHandle` / `targetHandle`:
   ```tsx
   const onConnect = useCallback(
     (connection: Connection) => {
       const validation = validateConnection({ source: connection.source, target: connection.target })
       if (!validation.ok) { setConnectError(validation.message); return }
       const duplicated = edges.some(
         (e) => e.source === connection.source && e.target === connection.target
           && (e.sourceHandle ?? null) === (connection.sourceHandle ?? null)
       )
       if (duplicated) { setConnectError('该连线已存在，请勿重复添加'); return }
       setEdges((eds) => addEdge({ ...connection, type: 'custom' }, eds))
       store.markDirty()
       setConnectError('')
       setSaveMessage('')
       setPublishMessage('')
     },
     [edges, setEdges, store],
   )
   ```

5. Update `onDrop` — for CONDITION nodes, add default branches:
   ```tsx
   const data: WorkflowNodeData = nodeType === 'CONDITION'
     ? {
         label: `${nodeType} 节点`,
         nodeType,
         branches: [
           { id: `branch-${Date.now()}-0`, name: '如果' },
           { id: `else-${Date.now()}`, name: '否则' },
         ],
       }
     : { label: `${nodeType} 节点`, nodeType }
   ```

6. Update `buildGraphPayload` to include `sourceHandle`:
   ```tsx
   edges: edges.map((edge) => ({
     edgeId: edge.id,
     source: edge.source,
     target: edge.target,
     sourceHandle: edge.sourceHandle ?? null,
     edgeType: 'DEPENDENCY',
   })),
   ```

7. Add `edgeTypes`, `defaultEdgeOptions`, and `connectionLineComponent` to ReactFlow:
   ```tsx
   <ReactFlow
     nodes={nodes}
     edges={edges}
     onNodesChange={onNodesChange}
     onEdgesChange={onEdgesChange}
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

**Step 4: Run all workflow tests**

Run: `cd ai-agent-foward && npx vitest run src/modules/workflow/`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add ai-agent-foward/src/modules/workflow/pages/WorkflowEditorPage.tsx ai-agent-foward/src/modules/workflow/__tests__/workflow.editor-interaction.test.tsx
git commit -m "feat(workflow): integrate horizontal layout, custom edge, and connection line"
```

---

### Task 6: 全量测试 + 截图验证

**Files:**
- No new files

**Step 1: Run full test suite**

Run: `cd ai-agent-foward && npx vitest run`
Expected: ALL PASS

**Step 2: Fix any broken tests**

If any tests fail due to Position mock changes (e.g., tests still expecting `Position.Top`/`Position.Bottom`), update those mocks to include `Left`/`Right`.

**Step 3: Commit any test fixes**

```bash
git add -A
git commit -m "test(workflow): fix tests for horizontal flow layout"
```

---

## Wave 并行策略

- **Wave 1 (Task 1, 2, 3):** NodeHandle、CustomEdge、CustomConnectionLine — 三个独立新组件，可并行
- **Wave 2 (Task 4):** WorkflowNode 改造 — 依赖 Task 1 的 NodeHandle
- **Wave 3 (Task 5):** WorkflowEditorPage 集成 — 依赖 Task 2, 3, 4
- **Wave 4 (Task 6):** 全量测试验证 — 依赖所有前置任务
