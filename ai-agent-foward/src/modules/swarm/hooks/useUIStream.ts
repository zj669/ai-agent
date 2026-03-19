import { useEffect, useRef } from "react";
import { subscribeUIStream } from "../api/swarmService";

interface UIStreamCallbacks {
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
