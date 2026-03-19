import {
  createConversation,
  getConversationList,
  getConversationMessages,
  stopWorkflowExecution,
  getReviewDetail,
  resumeExecution,
  rejectExecution,
  getExecution,
  type ConversationSummary,
  type ExecutionData,
  type MessageDTO,
  type MessageStatus,
  type ThoughtStepDTO,
  type ReviewDetailData,
  type ResumeExecutionInput,
  type RejectExecutionInput,
} from "../../../shared/api/adapters/chatAdapter";

export type ChatConversation = ConversationSummary;

export interface ChatMessage {
  id: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  status: MessageStatus;
  createdAt: string;
  thinkingSteps?: {
    nodeId: string;
    nodeName: string;
    nodeType: string;
    content: string;
    status: "running" | "done" | "failed";
  }[];
}

export interface SendMessageInput {
  agentId: number;
  userId: number;
  conversationId: string;
  content: string;
  versionId?: number;
}

export interface StartExecutionEvent {
  event: string;
  data: Record<string, unknown> | null;
}

export interface StartExecutionHandlers {
  onConnected?: (executionId: string) => void;
  onDelta?: (delta: string) => void;
  onThought?: (
    delta: string,
    nodeId: string,
    nodeName: string,
    nodeType: string,
  ) => void;
  onNodeStart?: (nodeId: string, nodeName: string, nodeType: string) => void;
  onNodeFinish?: (
    nodeId: string,
    nodeName: string,
    nodeType: string,
    status: string,
  ) => void;
  onFinish?: () => void;
  onError?: (message: string) => void;
  onPaused?: (executionId: string, nodeId: string) => void;
}

interface StreamRuntimeState {
  hasAssistantDelta: boolean;
  hasFinished: boolean;
}

const textDecoder = new TextDecoder();

function getAccessToken(): string | null {
  return (
    localStorage.getItem("accessToken") ?? sessionStorage.getItem("accessToken")
  );
}

function parseSseBlock(block: string): StartExecutionEvent | null {
  const lines = block
    .split("\n")
    .map((line) => line.trim())
    .filter(Boolean);

  if (lines.length === 0) {
    return null;
  }

  let event = "message";
  const dataLines: string[] = [];

  lines.forEach((line) => {
    if (line.startsWith("event:")) {
      event = line.slice(6).trim();
      return;
    }

    if (line.startsWith("data:")) {
      dataLines.push(line.slice(5).trim());
    }
  });

  const rawData = dataLines.join("\n");

  if (!rawData) {
    return { event, data: null };
  }

  if (rawData === "pong") {
    return { event, data: { pong: true } };
  }

  try {
    return {
      event,
      data: JSON.parse(rawData) as Record<string, unknown>,
    };
  } catch {
    return {
      event,
      data: { raw: rawData },
    };
  }
}

function extractDelta(data: Record<string, unknown> | null): string {
  if (!data) {
    return "";
  }

  const directDelta = data.delta;
  if (typeof directDelta === "string") {
    return directDelta;
  }

  const payload = data.payload;
  if (typeof payload === "object" && payload !== null && "delta" in payload) {
    const value = (payload as { delta?: unknown }).delta;
    return typeof value === "string" ? value : "";
  }

  const content = data.content;
  return typeof content === "string" ? content : "";
}

export async function fetchConversationList(
  userId: number,
  agentId: number,
): Promise<ChatConversation[]> {
  const data = await getConversationList({ userId, agentId });
  return data.list;
}

export async function createChatConversation(
  userId: number,
  agentId: number,
): Promise<string> {
  return createConversation({ userId, agentId });
}

/** 角色排序权重：同一秒内 USER 在前，ASSISTANT 在后，SYSTEM 最后 */
const ROLE_ORDER: Record<string, number> = { USER: 0, ASSISTANT: 1, SYSTEM: 2 };

export async function fetchConversationMessages(
  userId: number,
  conversationId: string,
): Promise<ChatMessage[]> {
  const messages = await getConversationMessages({
    conversationId,
    userId,
    order: "asc",
  });

  const mapped = messages.map(toChatMessage);

  // 后端按 createdAt 排序，但同一秒内 UUID 主键无序，
  // 需要二次排序：同一秒内 USER 消息排在 ASSISTANT 前面
  mapped.sort((a, b) => {
    const ta = new Date(a.createdAt).getTime();
    const tb = new Date(b.createdAt).getTime();
    if (ta !== tb) return ta - tb;
    return (ROLE_ORDER[a.role] ?? 9) - (ROLE_ORDER[b.role] ?? 9);
  });

  return mapped;
}

function mapThoughtStatus(status: string): "running" | "done" | "failed" {
  if (status === "RUNNING") return "running";
  if (status === "FAILED") return "failed";
  return "done";
}

function flattenThoughtSteps(
  steps: ThoughtStepDTO[],
): ChatMessage["thinkingSteps"] {
  const result: NonNullable<ChatMessage["thinkingSteps"]> = [];
  for (const step of steps) {
    result.push({
      nodeId: step.stepId ?? "",
      nodeName: step.title ?? "",
      nodeType: step.type ?? "",
      content: step.content ?? "",
      status: mapThoughtStatus(step.status),
    });
    if (step.children?.length) {
      result.push(...(flattenThoughtSteps(step.children!) ?? []));
    }
  }
  return result;
}

function toChatMessage(message: MessageDTO): ChatMessage {
  const msg: ChatMessage = {
    id: message.id,
    role: message.role,
    content: message.content,
    status: message.status,
    createdAt: message.createdAt,
  };
  if (message.thoughtProcess?.length) {
    msg.thinkingSteps = flattenThoughtSteps(message.thoughtProcess);
  }
  return msg;
}

export async function stopChatExecution(executionId: string): Promise<void> {
  await stopWorkflowExecution({ executionId });
}

export async function fetchReviewDetail(
  executionId: string,
): Promise<ReviewDetailData> {
  return getReviewDetail(executionId);
}

export async function submitResumeExecution(
  input: ResumeExecutionInput,
): Promise<void> {
  return resumeExecution(input);
}

export async function submitRejectExecution(
  input: RejectExecutionInput,
): Promise<void> {
  return rejectExecution(input);
}

export interface ChatExecution {
  executionId: string;
  status: string;
  conversationId: string;
  nodeStatuses: Record<string, string>;
}

export async function fetchExecution(
  executionId: string,
): Promise<ChatExecution> {
  const execution: ExecutionData = await getExecution(executionId);
  return {
    executionId: execution.executionId,
    status: execution.status,
    conversationId: execution.conversationId,
    nodeStatuses: execution.nodeStatuses,
  };
}

export type { ReviewDetailData };

async function consumeExecutionStream(
  response: Response,
  handlers: StartExecutionHandlers,
): Promise<void> {
  if (!response.ok || !response.body) {
    throw new Error("启动流式会话失败");
  }

  const reader = response.body.getReader();
  let buffer = "";
  const runtimeState: StreamRuntimeState = {
    hasAssistantDelta: false,
    hasFinished: false,
  };

  try {
    while (true) {
      const { done, value } = await reader.read();

      if (done) {
        if (!runtimeState.hasFinished) {
          runtimeState.hasFinished = true;
          handlers.onFinish?.();
        }
        break;
      }

      buffer += textDecoder.decode(value, { stream: true });

      let delimiterIndex = buffer.indexOf("\n\n");
      while (delimiterIndex >= 0) {
        const rawBlock = buffer.slice(0, delimiterIndex);
        buffer = buffer.slice(delimiterIndex + 2);

        const parsed = parseSseBlock(rawBlock);
        if (parsed) {
          handleSseEvent(parsed, handlers, runtimeState);
        }

        delimiterIndex = buffer.indexOf("\n\n");
      }
    }
  } finally {
    try {
      await reader.cancel();
    } catch {
      // noop
    }
    reader.releaseLock();
  }
}

export async function startChatStream(
  input: SendMessageInput,
  handlers: StartExecutionHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = getAccessToken();

  const response = await fetch("/api/workflow/execution/start", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "debug-user": "1",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({
      agentId: input.agentId,
      userId: input.userId,
      conversationId: input.conversationId,
      versionId: input.versionId,
      inputs: {
        query: input.content,
      },
      mode: "STANDARD",
    }),
    signal,
  });

  await consumeExecutionStream(response, handlers);
}

export async function resumeChatStream(
  executionId: string,
  handlers: StartExecutionHandlers,
  signal?: AbortSignal,
): Promise<void> {
  const token = getAccessToken();

  const response = await fetch(
    `/api/workflow/execution/${executionId}/stream`,
    {
      method: "GET",
      headers: {
        "debug-user": "1",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      signal,
    },
  );

  await consumeExecutionStream(response, handlers);
}

function handleSseEvent(
  event: StartExecutionEvent,
  handlers: StartExecutionHandlers,
  runtimeState: StreamRuntimeState,
): void {
  if (!event.data) {
    return;
  }

  if (event.event === "connected") {
    const executionId = event.data.executionId;
    if (typeof executionId === "string") {
      handlers.onConnected?.(executionId);
    }
    return;
  }

  if (event.event === "start") {
    const nodeId = event.data.nodeId as string | undefined;
    const nodeType = event.data.nodeType as string | undefined;
    const payload = event.data.payload as Record<string, unknown> | undefined;
    const nodeName = (payload?.title as string) ?? nodeId ?? "";
    if (nodeId && nodeType) {
      handlers.onNodeStart?.(nodeId, nodeName, nodeType);
    }
    return;
  }

  if (event.event === "update") {
    // Check for custom JSON events (e.g., workflow_paused)
    const payload = event.data?.payload as Record<string, unknown> | undefined;
    if (
      payload?.renderMode === "JSON_EVENT" &&
      typeof payload?.title === "string"
    ) {
      const eventTitle = payload.title;
      if (eventTitle === "workflow_paused") {
        try {
          const content =
            typeof payload.content === "string"
              ? JSON.parse(payload.content)
              : payload.content;
          const executionId = content?.executionId;
          const nodeId = content?.nodeId;
          if (typeof executionId === "string" && typeof nodeId === "string") {
            handlers.onPaused?.(executionId, nodeId);
          }
        } catch {
          // ignore parse errors
        }
      }
      return;
    }

    const delta = extractDelta(event.data);
    if (delta) {
      const isThought =
        payload?.isThought === true || payload?.renderMode === "THOUGHT";
      if (isThought) {
        const nodeId = (event.data.nodeId as string) ?? "";
        const nodeName = (payload?.title as string) ?? nodeId;
        const nodeType = (event.data.nodeType as string) ?? "";
        handlers.onThought?.(delta, nodeId, nodeName, nodeType);
      } else {
        runtimeState.hasAssistantDelta = true;
        handlers.onDelta?.(delta);
      }
    }
    return;
  }

  if (event.event === "finish") {
    const nodeId = event.data.nodeId as string | undefined;
    const nodeType = event.data.nodeType as string | undefined;
    const status = event.data.status as string | undefined;
    const payload = event.data.payload as Record<string, unknown> | undefined;
    const nodeName = (payload?.title as string) ?? nodeId ?? "";

    // 推送节点完成事件
    if (nodeId && nodeType) {
      handlers.onNodeFinish?.(
        nodeId,
        nodeName,
        nodeType,
        status ?? "SUCCEEDED",
      );
    }

    // END 节点完成时，把最终输出内容作为正文推送
    if (nodeType === "END") {
      const content = payload?.content;
      if (
        !runtimeState.hasAssistantDelta &&
        typeof content === "string" &&
        content
      ) {
        runtimeState.hasAssistantDelta = true;
        handlers.onDelta?.(content);
      }
    }

    // 仅在 END 节点完成 或 执行级完成事件（无 nodeId）时触发 onFinish
    // 注意：status 是节点状态，不是执行状态，所以不能用 status 判断执行是否完成
    const isExecutionLevelFinish = !nodeId;
    if (isExecutionLevelFinish || nodeType === "END") {
      runtimeState.hasFinished = true;
      handlers.onFinish?.();
    }
    return;
  }

  if (event.event === "execution_complete") {
    runtimeState.hasFinished = true;
    handlers.onFinish?.();
    return;
  }

  if (event.event === "error") {
    const message = event.data.message;
    handlers.onError?.(typeof message === "string" ? message : "流式执行失败");
  }

  if (event.event === "workflow_paused") {
    const executionId = event.data.executionId;
    const nodeId = event.data.nodeId;
    if (typeof executionId === "string" && typeof nodeId === "string") {
      handlers.onPaused?.(executionId, nodeId);
    }
  }
}
