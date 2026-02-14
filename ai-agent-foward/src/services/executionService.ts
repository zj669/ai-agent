/**
 * 工作流执行服务（精简版，仅用于聊天功能）
 */
import apiClient from './apiClient';
import type {
  StartExecutionRequest,
  SSEEventType,
  SSEConnectedEvent,
  SSEStartEvent,
  SSEUpdateEvent,
  SSEFinishEvent,
  SSEErrorEvent
} from '../types/execution';

interface SSECallbacks {
  onConnected?: (data: SSEConnectedEvent) => void;
  onStart?: (data: SSEStartEvent) => void;
  onUpdate?: (data: SSEUpdateEvent) => void;
  onFinish?: (data: SSEFinishEvent) => void;
  onError?: (data: SSEErrorEvent) => void;
}

class ExecutionService {
  /**
   * 启动工作流执行（SSE 流式响应）
   */
  async startExecution(
    request: StartExecutionRequest,
    callbacks: SSECallbacks
  ): Promise<AbortController> {
    const controller = new AbortController();

    try {
      const response = await fetch(
        `${apiClient.defaults.baseURL}/workflow/execution/start`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
            ...apiClient.defaults.headers.common
          },
          body: JSON.stringify(request),
          signal: controller.signal
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('Response body is not readable');
      }

      // Read SSE stream
      const readStream = async () => {
        try {
          while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });
            const lines = chunk.split('\n');

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.slice(6);
                if (data === '[DONE]') {
                  return;
                }

                try {
                  const event = JSON.parse(data);
                  this.handleSSEEvent(event, callbacks);
                } catch (e) {
                  console.error('[ExecutionService] Failed to parse SSE event:', e);
                }
              }
            }
          }
        } catch (error: any) {
          if (error.name !== 'AbortError') {
            console.error('[ExecutionService] Stream read error:', error);
            callbacks.onError?.({
              executionId: '',
              message: error.message || '流读取失败'
            });
          }
        }
      };

      readStream();
    } catch (error: any) {
      if (error.name !== 'AbortError') {
        console.error('[ExecutionService] Start execution error:', error);
        callbacks.onError?.({
          executionId: '',
          message: error.message || '启动执行失败'
        });
      }
    }

    return controller;
  }

  private handleSSEEvent(event: any, callbacks: SSECallbacks) {
    const eventType = event.event as SSEEventType;

    switch (eventType) {
      case 'connected':
        callbacks.onConnected?.(event.data);
        break;
      case 'start':
        callbacks.onStart?.(event.data);
        break;
      case 'update':
        callbacks.onUpdate?.(event.data);
        break;
      case 'finish':
        callbacks.onFinish?.(event.data);
        break;
      case 'error':
        callbacks.onError?.(event.data);
        break;
      case 'ping':
        // Ignore ping events
        break;
      default:
        console.warn('[ExecutionService] Unknown SSE event type:', eventType);
    }
  }
}

export const executionService = new ExecutionService();
