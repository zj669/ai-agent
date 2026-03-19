// 蜂群模块类型定义

export interface SwarmWorkspace {
  id: number;
  name: string;
  userId: number;
  agentCount: number;
  maxRoundsPerTurn: number;
  llmConfigId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface WorkspaceDefaults {
  workspaceId: number;
  humanAgentId: number;
  assistantAgentId: number;
  defaultGroupId: number;
}

export interface SwarmAgent {
  id: number;
  workspaceId: number;
  agentId?: number;
  role: string;
  description?: string;
  parentId?: number;
  status: AgentStatus;
  createdAt: string;
}

export type AgentStatus = "IDLE" | "BUSY" | "WAITING" | "WAKING" | "STOPPED";

export interface SwarmGroup {
  id: number;
  workspaceId: number;
  name?: string;
  memberIds: number[];
  unreadCount: number;
  lastMessage?: SwarmMessage;
  contextTokens: number;
}

export interface SwarmMessage {
  id: number;
  groupId: number;
  senderId: number;
  content: string;
  contentType: string;
  sendTime: string;
}

export interface SwarmSearchResult {
  agents: SwarmAgent[];
  groups: SwarmGroup[];
}

export interface WritingSessionSummary {
  id: number;
  workspaceId: number;
  rootAgentId: number;
  humanAgentId: number;
  defaultGroupId: number;
  title?: string;
  goal?: string;
  status: string;
  currentDraftId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface WritingMessageView {
  id: number;
  senderId: number;
  senderRole?: string | null;
  contentType: string;
  content: string;
  sendTime: string;
}

export interface WritingTaskSummary {
  id: number;
  taskUuid?: string;
  taskType: string;
  title: string;
  instruction?: string;
  status: string;
  priority?: number;
  createdAt?: string;
  startedAt?: string;
  finishedAt?: string;
}

export interface WritingResultSummary {
  id: number;
  taskId: number;
  resultType: string;
  summary?: string;
  content?: string;
  createdAt?: string;
}

export interface WritingDraftSummary {
  id: number;
  versionNo: number;
  title?: string;
  content?: string;
  status: string;
  createdAt?: string;
}

export interface WritingCollaborationCard {
  writingAgentId: number;
  swarmAgentId: number;
  role: string;
  description?: string;
  status: string;
  sortOrder?: number;
  currentTask?: WritingTaskSummary | null;
  latestResult?: WritingResultSummary | null;
  updatedAt?: string;
}

export interface WritingSessionOverview {
  session: WritingSessionSummary;
  rootConversation: WritingMessageView[];
  collaborationCards: WritingCollaborationCard[];
  latestDraft?: WritingDraftSummary | null;
}

export interface LiveToolCallStep {
  toolCallId: string;
  agentId: number;
  groupId: number;
  tool: string;
  argsPreview?: string;
  resultPreview?: string;
  status: "running" | "done";
  startedAt: number;
  finishedAt?: number;
}

// SSE 事件类型
export type AgentEventType =
  | "agent.stream"
  | "agent.done"
  | "agent.error"
  | "agent.wakeup";
export type UIEventType =
  | "ui.agent.created"
  | "ui.message.created"
  | "ui.agent.llm.start"
  | "ui.agent.llm.done"
  | "ui.agent.stream.start"
  | "ui.agent.stream.chunk"
  | "ui.agent.stream.done"
  | "ui.agent.tool_call.start"
  | "ui.agent.tool_call.done"
  | "ui.agent.waiting"
  | "ui.agent.waiting.done";
