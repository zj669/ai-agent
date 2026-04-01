import { useEffect, useRef } from "react";
import { subscribeUIStream, subscribeAgentStream } from "../api/swarmService";

export interface TaskNotificationPayload {
  agentId: number;
  agentRole?: string;
  status: "completed" | "failed" | "killed" | "coordinator_turn_complete";
  summary?: string;
  result?: string;
  phase?: string;
  taskUuid?: string;
  toolCallCount?: number;
  durationMs?: number;
  totalTokens?: number;
  timestamp?: number;
}

export interface UIStreamCallbacks {
  onAgentCreated?: (data: string) => void;
  onMessageCreated?: (data: string) => void;
  onLlmStart?: (data: string) => void;
  onLlmDone?: (data: string) => void;
  onStreamStart?: (data: string) => void;
  onStreamChunk?: (data: string) => void;
  onStreamDone?: (data: string) => void;
  onToolCallStart?: (data: string) => void;
  onToolCallDone?: (data: string) => void;
  onWaiting?: (data: string) => void;
  onWaitingDone?: (data: string) => void;
  /** task-notification 事件：Worker 完成时触发 */
  onTaskNotification?: (payload: TaskNotificationPayload) => void;
}

/**
 * 解析 SSE data 中的 task-notification XML
 */
function parseTaskNotificationXml(xmlData: string): TaskNotificationPayload | null {
  try {
    const getTag = (tag: string): string => {
      const match = xmlData.match(new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`));
      return match ? match[1].trim() : "";
    };

    const agentId = parseInt(getTag("task-id"), 10);
    if (isNaN(agentId)) return null;

    const statusStr = getTag("status");
    const status = statusStr as TaskNotificationPayload["status"];
    const phase = getTag("phase") || undefined;
    const taskUuid = getTag("taskUuid") || undefined;

    // 解析 usage
    const totalTokensMatch = getTag("total_tokens");
    const toolUsesMatch = getTag("tool_uses");
    const durationMsMatch = getTag("duration_ms");

    return {
      agentId,
      status: ["completed", "failed", "killed", "coordinator_turn_complete"].includes(status)
        ? status
        : "completed",
      summary: getTag("summary") || undefined,
      result: getTag("result") || undefined,
      phase: phase || undefined,
      taskUuid: taskUuid || undefined,
      totalTokens: totalTokensMatch ? parseInt(totalTokensMatch, 10) : undefined,
      toolCallCount: toolUsesMatch ? parseInt(toolUsesMatch, 10) : undefined,
      durationMs: durationMsMatch ? parseInt(durationMsMatch, 10) : undefined,
      timestamp: Date.now(),
    };
  } catch {
    return null;
  }
}

export function useUIStream(
  workspaceId: number | null,
  callbacks: UIStreamCallbacks,
) {
  const callbacksRef = useRef(callbacks);
  callbacksRef.current = callbacks;

  useEffect(() => {
    if (!workspaceId) return;

    console.log("[SSE] connecting to workspace", workspaceId);

    const handler = (event: MessageEvent) => {
      console.log("[SSE] event received:", event.type);
      const cb = callbacksRef.current;
      switch (event.type) {
        case "ui.agent.created":
          cb.onAgentCreated?.(event.data);
          break;
        case "ui.message.created":
          cb.onMessageCreated?.(event.data);
          break;
        case "ui.agent.llm.start":
          cb.onLlmStart?.(event.data);
          break;
        case "ui.agent.llm.done":
          cb.onLlmDone?.(event.data);
          break;
        case "ui.agent.stream.start":
          cb.onStreamStart?.(event.data);
          break;
        case "ui.agent.stream.chunk":
          cb.onStreamChunk?.(event.data);
          break;
        case "ui.agent.stream.done":
          cb.onStreamDone?.(event.data);
          break;
        case "ui.agent.tool_call.start":
          cb.onToolCallStart?.(event.data);
          break;
        case "ui.agent.tool_call.done":
          cb.onToolCallDone?.(event.data);
          break;
        case "ui.agent.waiting":
          cb.onWaiting?.(event.data);
          break;
        case "ui.agent.waiting.done":
          cb.onWaitingDone?.(event.data);
          break;
        case "ui.agent.task-notification":
          try {
            const parsed = JSON.parse(event.data);
            cb.onTaskNotification?.(parsed);
          } catch {
            // ignore malformed payload
          }
          break;
        case "agent.task-notification":
          // XML 格式的 task-notification from agent stream
          try {
            const payload = parseTaskNotificationXml(event.data);
            if (payload) {
              cb.onTaskNotification?.(payload);
            }
          } catch {
            // ignore
          }
          break;
      }
    };

    const es = subscribeUIStream(workspaceId, handler);

    es.onopen = () =>
      console.log("[SSE] connection opened, readyState:", es.readyState);
    es.onerror = (e) =>
      console.error("[SSE] connection error, readyState:", es.readyState, e);

    return () => {
      console.log(
        "[SSE] disconnecting from workspace",
        workspaceId,
        new Error().stack,
      );
      es.close();
    };
  }, [workspaceId]);
}

/**
 * 订阅单个 Agent 的 SSE 流（用于接收 task-notification）
 */
export function useAgentStream(
  agentId: number | null,
  callbacks: {
    onTaskNotification?: (payload: TaskNotificationPayload) => void;
  },
) {
  const callbacksRef = useRef(callbacks);
  callbacksRef.current = callbacks;

  useEffect(() => {
    if (!agentId) return;

    console.log("[SSE] connecting to agent stream", agentId);

    const handler = (event: MessageEvent) => {
      const cb = callbacksRef.current;
      if (event.type === "agent.task-notification") {
        const payload = parseTaskNotificationXml(event.data);
        if (payload) {
          cb.onTaskNotification?.(payload);
        }
      }
    };

    const es = subscribeAgentStream(agentId, handler);

    es.onopen = () =>
      console.log("[SSE] agent stream opened, readyState:", es.readyState);
    es.onerror = (e) =>
      console.error("[SSE] agent stream error, readyState:", es.readyState, e);

    return () => {
      console.log("[SSE] disconnecting from agent stream", agentId);
      es.close();
    };
  }, [agentId]);
}
