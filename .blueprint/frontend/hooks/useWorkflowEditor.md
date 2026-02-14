# useWorkflowEditor Blueprint

## 职责契约
- **做什么**: 封装工作流编辑器核心逻辑,管理节点/连接状态、撤销/重做、执行控制
- **不做什么**: 不处理 UI 渲染、不直接操作 DOM、不管理页面级状态(如选中节点)

## 接口摘要 (Hook API)

### 输入参数
| 名称 | 类型 | 说明 | 必需 |
|------|------|------|------|
| initialGraph | WorkflowGraph | 初始工作流图 | 否 |

### 返回值
| 名称 | 类型 | 说明 |
|------|------|------|
| nodes | ReactFlowNode[] | 节点列表 |
| edges | ReactFlowEdge[] | 连接线列表 |
| onNodesChange | (changes) => void | 节点变更处理 |
| onEdgesChange | (changes) => void | 连接线变更处理 |
| onConnect | (connection) => void | 连接节点 |
| addNode | (type, position) => void | 添加节点 |
| deleteNode | (nodeId) => void | 删除节点 |
| updateNodeData | (nodeId, data) => void | 更新节点数据 |
| duplicateNode | (nodeId) => void | 复制节点 |
| loadGraph | (graph) => void | 加载工作流图 |
| clearGraph | () => void | 清空画布 |
| convertToWorkflowGraph | () => WorkflowGraph | 转换为工作流图 |
| undo | () => void | 撤销 |
| redo | () => void | 重做 |
| canUndo | boolean | 是否可撤销 |
| canRedo | boolean | 是否可重做 |
| autoLayout | (direction) => void | 自动布局 |
| isExecuting | boolean | 是否正在执行 |
| executionId | string \| null | 执行 ID |
| executionLogs | string[] | 执行日志 |
| startExecution | (request) => Promise<void> | 启动执行 |
| stopExecution | () => Promise<void> | 停止执行 |

## 依赖拓扑
- **上游**: WorkflowEditorPage
- **下游**: 
  - ReactFlow hooks (useNodesState, useEdgesState)
  - workflowService (执行相关)
  - dagre (自动布局)

## 状态管理

### ReactFlow 状态
```typescript
const [nodes, setNodes, onNodesChangeRaw] = useNodesState<ReactFlowNode>([]);
const [edges, setEdges, onEdgesChangeRaw] = useEdgesState<ReactFlowEdge>([]);
```

### 历史记录状态
```typescript
const [undoStack, setUndoStack] = useState<HistoryState[]>([]);
const [redoStack, setRedoStack] = useState<HistoryState[]>([]);
const isUndoRedoAction = useRef(false);
```

### 执行状态
```typescript
const [isExecuting, setIsExecuting] = useState(false);
const [executionId, setExecutionId] = useState<string | null>(null);
const [executionLogs, setExecutionLogs] = useState<string[]>([]);
const abortControllerRef = useRef<AbortController | null>(null);
```

## 核心逻辑

### 1. 节点变更处理
```typescript
onNodesChange(changes) {
  // 检查是否需要保存历史快照
  const shouldSnapshot = changes.some(change => 
    change.type === 'position' && !change.dragging || 
    change.type === 'remove'
  );
  
  if (shouldSnapshot) saveToHistory();
  onNodesChangeRaw(changes);
}
```

### 2. 撤销/重做
```typescript
undo() {
  if (undoStack.length === 0) return;
  
  const previousState = undoStack[undoStack.length - 1];
  setRedoStack(prev => [...prev, { nodes, edges }]);
  
  isUndoRedoAction.current = true;
  setNodes(previousState.nodes);
  setEdges(previousState.edges);
  setUndoStack(prev => prev.slice(0, -1));
}
```

### 3. 工作流执行
```typescript
startExecution(request) {
  setIsExecuting(true);
  setExecutionLogs([]);
  
  const controller = await workflowService.startExecution(request, {
    onConnected: (data) => {
      setExecutionId(data.executionId);
      setExecutionLogs(logs => [...logs, `[连接成功] ${data.executionId}`]);
    },
    onStart: (data) => {
      updateNodeData(data.nodeId, { status: NodeExecutionStatus.RUNNING });
    },
    onFinish: (data) => {
      updateNodeData(data.nodeId, { status: data.status });
      if (data.status === NodeExecutionStatus.SUCCEEDED) {
        setIsExecuting(false);
      }
    },
    onError: (data) => {
      setIsExecuting(false);
      message.error(`执行失败: ${data.message}`);
    }
  });
  
  abortControllerRef.current = controller;
}
```

### 4. 自动布局
```typescript
autoLayout(direction = 'TB') {
  const graph = new dagre.graphlib.Graph();
  graph.setGraph({ rankdir: direction, ranksep: 100, nodesep: 44 });
  
  nodes.forEach(node => {
    graph.setNode(node.id, { width: 220, height: 104 });
  });
  
  edges.forEach(edge => {
    graph.setEdge(edge.source, edge.target);
  });
  
  dagre.layout(graph);
  
  setNodes(prev => prev.map(node => {
    const layoutNode = graph.node(node.id);
    return {
      ...node,
      position: {
        x: layoutNode.x - 110,
        y: layoutNode.y - 52
      }
    };
  }));
}
```

## 设计约束

### 历史记录约束
- 最大历史记录数: 50
- 拖拽结束时才保存快照(避免频繁保存)
- 撤销/重做操作不触发新的历史记录

### 执行约束
- 同时只能有一个执行实例
- 执行中禁止编辑节点
- 停止执行时清理 SSE 连接

### 性能约束
- 使用 useCallback 缓存函数
- 避免不必要的状态更新
- 节点数 > 100 时禁用动画

## 错误处理

### 执行失败
- 捕获 SSE 错误
- 更新节点状态为 FAILED
- 显示错误日志

### 布局失败
- 捕获 dagre 错误
- 保持原有布局
- 显示错误提示

## 变更日志
- [2026-02-13] 创建 useWorkflowEditor 蓝图
- [2026-02-13] 定义 Hook API、状态管理、核心逻辑
