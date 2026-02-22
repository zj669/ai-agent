# 工作流编辑器：水平流向 + 多 Handle 分支节点

> 日期：2026-02-21
> 参考：Dify `dify-reference/web/app/components/workflow/`

## 目标

将工作流编辑器从垂直（上下）布局改为水平（左右）布局，CONDITION 节点支持多个独立输出连接点（每个分支一个 Handle），连线使用自定义贝塞尔曲线。

## 参考图

`ai-agent-foward/screenshots/image.png` — Dify 选择器节点，展示：
- 左进右出的 Handle 布局
- 每个条件分支（如果/否则如果/否则）各有独立的右侧蓝色连接点
- 水平贝塞尔曲线连线

## 设计

### 1. Handle 系统

新建 `NodeHandle.tsx`，导出 `NodeTargetHandle` 和 `NodeSourceHandle`：

```tsx
// NodeTargetHandle — 左侧输入
<Handle type="target" position={Position.Left} id={handleId}
  className="!h-4 !w-4 !bg-transparent after:absolute after:left-1.5 after:top-1 after:h-2 after:w-0.5 after:rounded-full after:bg-blue-500" />

// NodeSourceHandle — 右侧输出
<Handle type="source" position={Position.Right} id={handleId}
  className="!h-4 !w-4 !bg-transparent after:absolute after:right-1.5 after:top-1 after:h-2 after:w-0.5 after:rounded-full after:bg-blue-500" />
```

规则：
- START 节点：无 target handle，有 1 个 source handle（`handleId="source"`）
- END 节点：有 1 个 target handle（`handleId="target"`），无 source handle
- 普通节点（LLM/TOOL/HTTP）：1 个 target + 1 个 source
- CONDITION 节点：1 个 target + N 个 source（每个分支一个，`handleId` = branch ID）

### 2. 自定义 Edge

新建 `CustomEdge.tsx`，参照 Dify 的 `custom-edge.tsx`：

```tsx
const [edgePath] = getBezierPath({
  sourceX: sourceX - 8,
  sourceY,
  sourcePosition: Position.Right,
  targetX: targetX + 8,
  targetY,
  targetPosition: Position.Left,
  curvature: 0.16,
})

<BaseEdge id={id} path={edgePath} style={{ stroke: '#2970FF', strokeWidth: 2 }} />
```

注册为 ReactFlow 的 edgeTypes：`{ custom: CustomEdge }`，所有新建边的 `type` 设为 `'custom'`。

### 3. CONDITION 节点多分支 Handle

`WorkflowNodeData` 扩展：

```tsx
export type WorkflowNodeData = {
  label: string
  nodeType: WorkflowNodeType
  branches?: Branch[]  // 仅 CONDITION 节点使用
  // ... 其他字段不变
}
```

CONDITION 节点默认 branches：
```tsx
[
  { name: '如果', id: 'branch-0' },
  { name: '否则', id: 'else' },
]
```

WorkflowNode 渲染逻辑：
- 非 CONDITION 节点：渲染 1 个 `NodeSourceHandle`（`handleId="source"`）
- CONDITION 节点：遍历 `branches`，每个分支渲染一行标签 + 对应的 `NodeSourceHandle`（`handleId={branch.id}`）

### 4. WorkflowEditorPage 调整

- INITIAL_NODES 水平排列：START `{x:50, y:250}` → END `{x:600, y:250}`
- `defaultEdgeOptions` 设置 `type: 'custom'`
- `onConnect` 回调保存 `sourceHandle` / `targetHandle` 到 Edge 数据
- `buildGraphPayload` 序列化时携带 `sourceHandle` 信息（用于后端识别走哪个分支）

### 5. 连线拖拽样式

新建 `CustomConnectionLine.tsx`：
```tsx
// 拖拽中的连线使用相同的贝塞尔曲线 + 蓝色虚线
const [edgePath] = getBezierPath({
  sourceX, sourceY, sourcePosition: Position.Right,
  targetX: toX, targetY: toY, targetPosition: Position.Left,
  curvature: 0.16,
})
<path d={edgePath} stroke="#2970FF" strokeWidth={2} strokeDasharray="6 4" fill="none" />
```

### 6. 不改动的部分

- `EditorHeader` — 保持不变
- `AgentConfigPanel` — 保持不变
- `CanvasToolbar` — 保持不变（拖放逻辑已基于鼠标位置）
- `validateConnection` / `validateWorkflowGraph` — 验证逻辑不变
- `useEditorStore` — 状态结构不变
- `NodeConfigTabs` / `FieldRenderer` — 保持不变
- `ConditionBranchEditor` — 保持不变（在展开面板中使用，与 Handle 独立）

## 文件变更清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `components/NodeHandle.tsx` | 新建 | NodeTargetHandle / NodeSourceHandle |
| `components/CustomEdge.tsx` | 新建 | 自定义贝塞尔曲线边 |
| `components/CustomConnectionLine.tsx` | 新建 | 拖拽中的连线样式 |
| `components/WorkflowNode.tsx` | 修改 | Handle 改为 Left/Right，CONDITION 多 Handle |
| `pages/WorkflowEditorPage.tsx` | 修改 | 水平布局、edgeTypes、defaultEdgeOptions |
| 测试文件 | 修改 | 适配 Handle 位置变化 |
