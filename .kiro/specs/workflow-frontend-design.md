# 工作流编排前端设计方案

## 1. 技术栈

### 核心框架
- **React 18** + **TypeScript 5**
- **Vite** (构建工具)
- **React Flow** (可视化流程图)

### 状态管理
- **Zustand** (轻量级状态管理)
- **React Query** (服务端状态管理)

### UI 组件库
- **Ant Design 5** (企业级 UI 组件)
- **Tailwind CSS** (样式工具)

### 实时通信
- **EventSource** (SSE 客户端)
- **Axios** (HTTP 客户端)

## 2. 项目结构

```
app/frontend/
├── src/
│   ├── features/
│   │   └── orchestration/
│   │       ├── components/
│   │       │   ├── WorkflowCanvas.tsx       # React Flow 画布
│   │       │   ├── NodeConfigPanel.tsx      # 节点配置面板
│   │       │   ├── ExecutionDebugger.tsx    # 执行调试器
│   │       │   ├── ThoughtChainViewer.tsx   # 思维链查看器
│   │       │   └── nodes/
│   │       │       ├── StartNode.tsx
│   │       │       ├── EndNode.tsx
│   │       │       ├── LlmNode.tsx
│   │       │       ├── ToolNode.tsx
│   │       │       ├── ConditionNode.tsx
│   │       │       └── HttpNode.tsx
│   │       ├── hooks/
│   │       │   ├── useWorkflowExecution.ts  # 执行状态管理
│   │       │   ├── useSSEConnection.ts      # SSE 连接管理
│   │       │   └── useNodeTemplates.ts      # 节点模板加载
│   │       ├── services/
│   │       │   ├── workflowService.ts       # API 服务
│   │       │   └── sseClient.ts             # SSE 客户端
│   │       ├── stores/
│   │       │   └── workflowStore.ts         # Zustand Store
│   │       └── types/
│   │           └── workflow.types.ts        # TypeScript 类型定义
│   ├── shared/
│   │   ├── components/                      # 共享组件
│   │   └── utils/                           # 工具函数
│   └── main.tsx
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 3. 核心功能设计

### 3.1 可视化编排器 (WorkflowCanvas)

**功能**:
- 拖拽节点创建
- 连线创建/删除
- 节点配置
- 画布缩放/平移
- 自动布局

**技术实现**:
```typescript
import ReactFlow, {
  Node,
  Edge,
  Controls,
  Background
} from 'reactflow';

const WorkflowCanvas = () => {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);

  const nodeTypes = {
    start: StartNode,
    end: EndNode,
    llm: LlmNode,
    tool: ToolNode,
    condition: ConditionNode,
    http: HttpNode,
  };

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      nodeTypes={nodeTypes}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
    >
      <Controls />
      <Background />
    </ReactFlow>
  );
};
```

### 3.2 节点配置面板 (NodeConfigPanel)

**功能**:
- 动态表单渲染（根据节点类型）
- 输入映射配置（支持 SpEL 表达式）
- 条件分支配置
- 人工审核配置

**表单字段示例**:
- **LLM 节点**: model, prompt, temperature, maxTokens
- **HTTP 节点**: url, method, headers, body
- **CONDITION 节点**: routingStrategy, branches

### 3.3 SSE 流式执行 (useSSEConnection)

**功能**:
- 建立 SSE 连接
- 接收实时事件（node_start, node_finish, workflow_paused, etc.）
- 更新执行状态
- 错误处理和重连

**技术实现**:
```typescript
const useSSEConnection = (executionId: string) => {
  const [events, setEvents] = useState<ExecutionEvent[]>([]);

  useEffect(() => {
    const eventSource = new EventSource(
      `/api/workflow/execution/start`,
      { withCredentials: true }
    );

    eventSource.addEventListener('node_start', (e) => {
      const data = JSON.parse(e.data);
      setEvents(prev => [...prev, data]);
    });

    eventSource.addEventListener('node_finish', (e) => {
      const data = JSON.parse(e.data);
      setEvents(prev => [...prev, data]);
    });

    eventSource.onerror = () => {
      eventSource.close();
      // 重连逻辑
    };

    return () => eventSource.close();
  }, [executionId]);

  return { events };
};
```

### 3.4 执行调试器 (ExecutionDebugger)

**功能**:
- 实时显示执行进度
- 节点状态可视化（PENDING, RUNNING, SUCCEEDED, FAILED）
- 执行日志查看
- 暂停/恢复/停止控制

**UI 布局**:
```
┌─────────────────────────────────────┐
│ 执行控制栏                           │
│ [▶ 启动] [⏸ 暂停] [⏹ 停止]         │
├─────────────────────────────────────┤
│ 执行进度                             │
│ ████████░░░░░░░░░░░░ 40% (4/10)     │
├─────────────────────────────────────┤
│ 节点状态列表                         │
│ ✅ START - 已完成                    │
│ ⏳ LLM_1 - 执行中...                 │
│ ⏸ CONDITION_1 - 等待审核             │
│ ⏱ HTTP_1 - 等待执行                  │
└─────────────────────────────────────┘
```

### 3.5 思维链查看器 (ThoughtChainViewer)

**功能**:
- 显示节点执行日志
- 输入/输出查看
- 执行时长统计
- 错误信息展示

**数据结构**:
```typescript
interface ThoughtStep {
  nodeId: string;
  nodeName: string;
  nodeType: string;
  status: 'RUNNING' | 'SUCCESS' | 'FAILED';
  inputs: Record<string, any>;
  outputs: Record<string, any>;
  errorMessage?: string;
  startTime: string;
  endTime: string;
  durationMs: number;
}
```

## 4. API 对接

### 4.1 工作流执行 API

```typescript
// workflowService.ts
export const workflowService = {
  // 启动执行 (SSE)
  startExecution: (request: StartExecutionRequest) => {
    return new EventSource(
      `/api/workflow/execution/start`,
      {
        method: 'POST',
        body: JSON.stringify(request),
      }
    );
  },

  // 停止执行
  stopExecution: (executionId: string) => {
    return axios.post(`/api/workflow/execution/stop`, {
      executionId,
    });
  },

  // 获取执行详情
  getExecution: (executionId: string) => {
    return axios.get(`/api/workflow/execution/${executionId}`);
  },

  // 获取思维链日志
  getExecutionLogs: (executionId: string) => {
    return axios.get(`/api/workflow/execution/${executionId}/logs`);
  },
};
```

### 4.2 节点模板 API

```typescript
// 获取节点模板
export const getNodeTemplates = () => {
  return axios.get('/api/meta/node-templates');
};
```

## 5. 状态管理 (Zustand)

```typescript
// workflowStore.ts
interface WorkflowState {
  // 工作流图
  nodes: Node[];
  edges: Edge[];

  // 执行状态
  executionId: string | null;
  executionStatus: 'IDLE' | 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED';
  nodeStatuses: Record<string, ExecutionStatus>;

  // 选中状态
  selectedNodeId: string | null;

  // Actions
  setNodes: (nodes: Node[]) => void;
  setEdges: (edges: Edge[]) => void;
  updateNodeStatus: (nodeId: string, status: ExecutionStatus) => void;
  selectNode: (nodeId: string) => void;
}

export const useWorkflowStore = create<WorkflowState>((set) => ({
  nodes: [],
  edges: [],
  executionId: null,
  executionStatus: 'IDLE',
  nodeStatuses: {},
  selectedNodeId: null,

  setNodes: (nodes) => set({ nodes }),
  setEdges: (edges) => set({ edges }),
  updateNodeStatus: (nodeId, status) =>
    set((state) => ({
      nodeStatuses: { ...state.nodeStatuses, [nodeId]: status },
    })),
  selectNode: (nodeId) => set({ selectedNodeId: nodeId }),
}));
```

## 6. 样式设计

### 6.1 节点样式

```css
/* 节点基础样式 */
.workflow-node {
  padding: 12px 16px;
  border-radius: 8px;
  border: 2px solid #d9d9d9;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 180px;
}

/* 节点状态样式 */
.node-pending { border-color: #d9d9d9; }
.node-running { border-color: #1890ff; animation: pulse 1.5s infinite; }
.node-succeeded { border-color: #52c41a; }
.node-failed { border-color: #ff4d4f; }
.node-paused { border-color: #faad14; }

/* 节点类型图标 */
.node-icon {
  width: 24px;
  height: 24px;
  margin-right: 8px;
}
```

### 6.2 边样式

```css
/* 条件边样式 */
.edge-condition {
  stroke: #1890ff;
  stroke-width: 2;
  stroke-dasharray: 5, 5;
}

/* 默认边样式 */
.edge-default {
  stroke: #d9d9d9;
  stroke-width: 2;
}

/* 激活边样式 */
.edge-active {
  stroke: #52c41a;
  stroke-width: 3;
  animation: flow 1s linear infinite;
}
```

## 7. 测试计划

### 7.1 单元测试
- 节点配置表单验证
- 工作流图序列化/反序列化
- SSE 事件处理逻辑

### 7.2 集成测试
- 工作流保存/加载
- SSE 流式执行
- 条件分支路由
- 人工审核流程

### 7.3 E2E 测试
- 创建工作流 → 配置节点 → 执行 → 查看结果
- 条件分支测试
- 错误处理测试

## 8. 性能优化

### 8.1 React Flow 优化
- 使用 `memo` 优化节点渲染
- 使用 `useCallback` 优化事件处理
- 大图性能优化（虚拟化渲染）

### 8.2 SSE 优化
- 事件节流（避免频繁更新）
- 自动重连机制
- 心跳检测

### 8.3 状态管理优化
- 使用 `immer` 优化不可变更新
- 选择性订阅（避免不必要的重渲染）

## 9. 部署方案

### 9.1 开发环境
```bash
npm run dev  # Vite 开发服务器 (http://localhost:5173)
```

### 9.2 生产构建
```bash
npm run build  # 构建到 dist/ 目录
```

### 9.3 集成到 Spring Boot
将构建产物复制到 `ai-agent-interfaces/src/main/resources/static/` 目录

## 10. 下一步行动

1. ✅ 后端 Review 完成
2. ⏳ 创建前端项目脚手架
3. ⏳ 实现 React Flow 可视化编排器
4. ⏳ 实现节点配置面板
5. ⏳ 实现 SSE 客户端
6. ⏳ 实现执行调试器
7. ⏳ 前后端联调测试
