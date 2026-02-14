# workflowService Blueprint

## 职责契约
- **做什么**: 封装工作流相关 HTTP/SSE 请求,提供执行、停止、查询接口
- **不做什么**: 不处理业务逻辑、不管理状态、不直接操作 DOM

## 接口摘要 (Service API)

| 方法 | 输入 | 输出 | 副作用 | 说明 |
|------|------|------|--------|------|
| startExecution | StartExecutionRequest, SSECallbacks | Promise<AbortController> | 建立 SSE 连接 | 启动工作流执行 |
| stopExecution | StopExecutionRequest | Promise<void> | 发送停止请求 | 停止工作流执行 |
| getExecutionContext | executionId: string | Promise<ExecutionContextDTO> | 无 | 获取执行上下文 |
| getExecutionLogs | executionId: string | Promise<string[]> | 无 | 获取执行日志 |

## 依赖拓扑
- **上游**: useWorkflowEditor, WorkflowEditorPage
- **下游**: apiClient (HTTP 客户端)

## 核心实现

### 1. 启动执行 (SSE)
```typescript
async startExecution(
  request: StartExecutionRequest,
  callbacks: SSECallbacks
): Promise<AbortController> {
  const controller = new AbortController();
  
  const response = await fetch('/api/workflow/execute', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(request),
    signal: controller.signal
  });
  
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');
    
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = JSON.parse(line.slice(6));
        
        switch (data.type) {
          case 'CONNECTED':
            callbacks.onConnected?.(data);
            break;
          case 'NODE_START':
            callbacks.onStart?.(data);
            break;
          case 'NODE_UPDATE':
            callbacks.onUpdate?.(data);
            break;
          case 'NODE_FINISH':
            callbacks.onFinish?.(data);
            break;
          case 'ERROR':
            callbacks.onError?.(data);
            break;
        }
      }
    }
  }
  
  return controller;
}
```

### 2. 停止执行
```typescript
async stopExecution(request: StopExecutionRequest): Promise<void> {
  await apiClient.post('/api/workflow/stop', request);
}
```

### 3. 获取执行上下文
```typescript
async getExecutionContext(executionId: string): Promise<ExecutionContextDTO> {
  const response = await apiClient.get(`/api/workflow/execution/${executionId}/context`);
  return response.data;
}
```

## 类型定义

### StartExecutionRequest
```typescript
interface StartExecutionRequest {
  agentId: number;
  userId: number;
  conversationId: string;
  inputs: Record<string, any>;
  mode: ExecutionMode;
}
```

### SSECallbacks
```typescript
interface SSECallbacks {
  onConnected?: (data: SSEConnectedEvent) => void;
  onStart?: (data: SSEStartEvent) => void;
  onUpdate?: (data: SSEUpdateEvent) => void;
  onFinish?: (data: SSEFinishEvent) => void;
  onError?: (data: SSEErrorEvent) => void;
  onPing?: () => void;
}
```

### ExecutionContextDTO
```typescript
interface ExecutionContextDTO {
  executionId: string;
  variables: Record<string, any>;
  longTermMemories: string[];
  shortTermMemories: Message[];
  awareness: Record<string, any>;
}
```

## 错误处理

### 网络错误
- 捕获 fetch 异常
- 重试机制(可选)
- 返回友好错误信息

### SSE 连接中断
- 检测连接状态
- 自动重连(可选)
- 通知上层处理

### 超时处理
- 设置请求超时
- 清理资源
- 返回超时错误

## 设计约束

### SSE 约束
- 使用 AbortController 管理连接
- 支持手动中断
- 处理重连逻辑

### 性能约束
- 避免频繁轮询
- 使用 SSE 流式推送
- 合理设置超时时间

## 变更日志
- [2026-02-13] 创建 workflowService 蓝图
- [2026-02-13] 定义 SSE 执行接口和回调机制
