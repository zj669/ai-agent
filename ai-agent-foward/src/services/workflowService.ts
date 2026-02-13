import apiClient from './apiClient';
import {
  StartExecutionRequest,
  StopExecutionRequest,
  ExecutionDTO,
  WorkflowNodeExecutionLog,
  WorkflowNodeExecutionLogDTO,
  ExecutionContextDTO,
  SSEEventType,
  SSEConnectedEvent,
  SSEStartEvent,
  SSEUpdateEvent,
  SSEFinishEvent,
  SSEErrorEvent
} from '../types/workflow';

/**
 * 工作流执行服务
 */
class WorkflowService {
  /**
   * 启动工作流执行 (SSE 流式)
   *
   * 注意: 这个方法返回一个 AbortController,用于取消连接
   *
   * @param request 启动请求
   * @param onEvent 事件回调
   * @returns AbortController 对象
   */
  async startExecution(
    request: StartExecutionRequest,
    onEvent: {
      onConnected?: (data: SSEConnectedEvent) => void;
      onStart?: (data: SSEStartEvent) => void;
      onUpdate?: (data: SSEUpdateEvent) => void;
      onFinish?: (data: SSEFinishEvent) => void;
      onError?: (data: SSEErrorEvent) => void;
      onPing?: () => void;
    }
  ): Promise<AbortController> {
    const abortController = new AbortController();
    const token = localStorage.getItem('auth_token');

    try {
      const response = await fetch('/api/workflow/execution/start', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { Authorization: `Bearer ${token}` })
        },
        body: JSON.stringify(request),
        signal: abortController.signal
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Response body is null');
      }

      // 读取 SSE 流
      const readStream = async () => {
        let buffer = '';

        try {
          while (true) {
            const { done, value } = await reader.read();

            if (done) {
              break;
            }

            buffer += decoder.decode(value, { stream: true });

            // 处理完整的 SSE 消息
            const lines = buffer.split('\n\n');
            buffer = lines.pop() || '';

            for (const line of lines) {
              if (!line.trim()) continue;

              const eventMatch = line.match(/^event:\s*(.+)$/m);
              const dataMatch = line.match(/^data:\s*(.+)$/m);

              if (eventMatch && dataMatch) {
                const eventType = eventMatch[1].trim();
                const data = dataMatch[1].trim();

                try {
                  switch (eventType) {
                    case SSEEventType.CONNECTED:
                      if (onEvent.onConnected) {
                        onEvent.onConnected(JSON.parse(data));
                      }
                      break;
                    case SSEEventType.START:
                      if (onEvent.onStart) {
                        onEvent.onStart(JSON.parse(data));
                      }
                      break;
                    case SSEEventType.UPDATE:
                      if (onEvent.onUpdate) {
                        onEvent.onUpdate(JSON.parse(data));
                      }
                      break;
                    case SSEEventType.FINISH:
                      if (onEvent.onFinish) {
                        onEvent.onFinish(JSON.parse(data));
                      }
                      break;
                    case SSEEventType.ERROR:
                      if (onEvent.onError) {
                        onEvent.onError(JSON.parse(data));
                      }
                      break;
                    case SSEEventType.PING:
                      if (onEvent.onPing) {
                        onEvent.onPing();
                      }
                      break;
                  }
                } catch (e) {
                  console.error('[WorkflowService] Failed to parse SSE data:', e);
                }
              }
            }
          }
        } catch (error: any) {
          if (error.name !== 'AbortError') {
            console.error('[WorkflowService] Stream reading error:', error);
            if (onEvent.onError) {
              onEvent.onError({
                executionId: '',
                message: '连接中断，请重试'
              });
            }
          }
        }
      };

      // 异步读取流
      readStream();
    } catch (error: any) {
      if (error.name !== 'AbortError') {
        console.error('[WorkflowService] Failed to start execution:', error);
        if (onEvent.onError) {
          onEvent.onError({
            executionId: '',
            message: error.message || '启动失败，请重试'
          });
        }
      }
    }

    return abortController;
  }

  /**
   * 停止/取消执行
   */
  async stopExecution(request: StopExecutionRequest): Promise<void> {
    await apiClient.post('/workflow/execution/stop', request);
  }

  /**
   * 获取执行详情
   */
  async getExecution(executionId: string): Promise<ExecutionDTO> {
    const response = await apiClient.get<ExecutionDTO>(`/workflow/execution/${executionId}`);
    return response.data;
  }

  /**
   * 获取节点执行日志
   */
  async getNodeExecutionLog(executionId: string, nodeId: string): Promise<WorkflowNodeExecutionLog> {
    const response = await apiClient.get<WorkflowNodeExecutionLog>(
      `/workflow/execution/${executionId}/node/${nodeId}`
    );
    return response.data;
  }

  /**
   * 获取执行思维链日志
   */
  async getExecutionLogs(executionId: string): Promise<WorkflowNodeExecutionLogDTO[]> {
    const response = await apiClient.get<WorkflowNodeExecutionLogDTO[]>(
      `/workflow/execution/${executionId}/logs`
    );
    return response.data;
  }

  /**
   * 获取会话执行历史
   */
  async getHistory(conversationId: string): Promise<ExecutionDTO[]> {
    const response = await apiClient.get<ExecutionDTO[]>(
      `/workflow/execution/history/${conversationId}`
    );
    return response.data;
  }

  /**
   * 获取执行上下文快照
   */
  async getExecutionContext(executionId: string): Promise<ExecutionContextDTO> {
    const response = await apiClient.get<ExecutionContextDTO>(
      `/workflow/execution/${executionId}/context`
    );
    return response.data;
  }
}

export const workflowService = new WorkflowService();
