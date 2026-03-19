import { apiClient } from "../../../shared/api/client";
import { unwrapResponse, type ApiResponse } from "../../../shared/api/response";
import type {
  SwarmWorkspace,
  WorkspaceDefaults,
  SwarmAgent,
  SwarmGroup,
  SwarmMessage,
  SwarmSearchResult,
  WritingSessionOverview,
  WritingSessionSummary,
} from "../types/swarm";

// --- Workspace ---

export async function createWorkspace(
  name: string,
  llmConfigId: number,
): Promise<WorkspaceDefaults> {
  const res = await apiClient.post<ApiResponse<WorkspaceDefaults>>(
    "/api/swarm/workspace",
    { name, llmConfigId },
  );
  return unwrapResponse(res);
}

export async function listWorkspaces(): Promise<SwarmWorkspace[]> {
  const res = await apiClient.get<ApiResponse<SwarmWorkspace[]>>(
    "/api/swarm/workspace",
  );
  return unwrapResponse(res);
}

export async function getWorkspace(id: number): Promise<SwarmWorkspace> {
  const res = await apiClient.get<ApiResponse<SwarmWorkspace>>(
    `/api/swarm/workspace/${id}`,
  );
  return unwrapResponse(res);
}

export async function deleteWorkspace(id: number): Promise<void> {
  const res = await apiClient.delete<ApiResponse<null>>(
    `/api/swarm/workspace/${id}`,
  );
  unwrapResponse(res);
}

export async function getWorkspaceDefaults(
  id: number,
): Promise<WorkspaceDefaults> {
  const res = await apiClient.get<ApiResponse<WorkspaceDefaults>>(
    `/api/swarm/workspace/${id}/defaults`,
  );
  return unwrapResponse(res);
}

// --- Agents ---

export async function listAgents(workspaceId: number): Promise<SwarmAgent[]> {
  const res = await apiClient.get<ApiResponse<SwarmAgent[]>>(
    `/api/swarm/workspace/${workspaceId}/agents`,
  );
  return unwrapResponse(res);
}

export async function createAgent(
  workspaceId: number,
  role: string,
  parentId?: number,
): Promise<WorkspaceDefaults> {
  const res = await apiClient.post<ApiResponse<WorkspaceDefaults>>(
    `/api/swarm/workspace/${workspaceId}/agents`,
    { role, parentId },
  );
  return unwrapResponse(res);
}

// --- Groups / Messages ---

export async function listGroups(
  workspaceId: number,
  agentId?: number,
): Promise<SwarmGroup[]> {
  const params = agentId ? `?agentId=${agentId}` : "";
  const res = await apiClient.get<ApiResponse<SwarmGroup[]>>(
    `/api/swarm/workspace/${workspaceId}/groups${params}`,
  );
  return unwrapResponse(res);
}

export async function getMessages(
  groupId: number,
  markRead = false,
  readerId?: number,
): Promise<SwarmMessage[]> {
  const params = new URLSearchParams();
  if (markRead) params.set("markRead", "true");
  if (readerId) params.set("readerId", String(readerId));
  const res = await apiClient.get<ApiResponse<SwarmMessage[]>>(
    `/api/swarm/group/${groupId}/messages?${params}`,
  );
  return unwrapResponse(res);
}

export async function sendMessage(
  groupId: number,
  senderId: number,
  content: string,
): Promise<SwarmMessage> {
  const res = await apiClient.post<ApiResponse<SwarmMessage>>(
    `/api/swarm/group/${groupId}/messages`,
    {
      senderId,
      content,
      contentType: "text",
    },
  );
  return unwrapResponse(res);
}

// --- Search ---

export async function searchWorkspace(
  workspaceId: number,
  query: string,
): Promise<SwarmSearchResult> {
  const res = await apiClient.get<ApiResponse<SwarmSearchResult>>(
    `/api/swarm/workspace/${workspaceId}/search?q=${encodeURIComponent(query)}`,
  );
  return unwrapResponse(res);
}

// --- Writing ---

export async function listWritingSessions(
  workspaceId: number,
): Promise<WritingSessionSummary[]> {
  const res = await apiClient.get<ApiResponse<WritingSessionSummary[]>>(
    `/api/writing/workspace/${workspaceId}/sessions`,
  );
  return unwrapResponse(res);
}

export async function getWritingSessionOverview(
  sessionId: number,
): Promise<WritingSessionOverview> {
  const res = await apiClient.get<ApiResponse<WritingSessionOverview>>(
    `/api/writing/session/${sessionId}/overview`,
  );
  return unwrapResponse(res);
}

// --- Agent Control ---

export async function stopAgent(agentId: number): Promise<void> {
  const res = await apiClient.post<ApiResponse<null>>(
    `/api/swarm/agent/${agentId}/stop`,
  );
  unwrapResponse(res);
}

export async function createHumanAgentGroup(
  workspaceId: number,
  agentId1: number,
  agentId2: number,
): Promise<SwarmGroup> {
  const res = await apiClient.post<ApiResponse<SwarmGroup>>(
    `/api/swarm/workspace/${workspaceId}/groups/p2p`,
    { agentId1, agentId2 },
  );
  return unwrapResponse(res);
}

// --- SSE ---

export function subscribeAgentStream(
  agentId: number,
  onEvent: (event: MessageEvent) => void,
): EventSource {
  const token =
    sessionStorage.getItem("accessToken") ||
    localStorage.getItem("accessToken") ||
    "";
  const es = new EventSource(
    `/api/swarm/agent/${agentId}/stream?token=${encodeURIComponent(token)}`,
  );
  es.onmessage = onEvent;
  es.addEventListener("agent.stream", onEvent);
  es.addEventListener("agent.done", onEvent);
  es.addEventListener("agent.error", onEvent);
  return es;
}

export function subscribeUIStream(
  workspaceId: number,
  onEvent: (event: MessageEvent) => void,
): EventSource {
  const token =
    sessionStorage.getItem("accessToken") ||
    localStorage.getItem("accessToken") ||
    "";
  const es = new EventSource(
    `/api/swarm/workspace/${workspaceId}/ui-stream?token=${encodeURIComponent(token)}`,
  );
  es.addEventListener("ui.agent.created", onEvent);
  es.addEventListener("ui.message.created", onEvent);
  es.addEventListener("ui.agent.llm.start", onEvent);
  es.addEventListener("ui.agent.llm.done", onEvent);
  es.addEventListener("ui.agent.stream.start", onEvent);
  es.addEventListener("ui.agent.stream.chunk", onEvent);
  es.addEventListener("ui.agent.stream.done", onEvent);
  es.addEventListener("ui.agent.tool_call.start", onEvent);
  es.addEventListener("ui.agent.tool_call.done", onEvent);
  es.addEventListener("ui.agent.waiting", onEvent);
  es.addEventListener("ui.agent.waiting.done", onEvent);
  return es;
}
