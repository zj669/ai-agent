// 蜂群模块类型定义

export interface SwarmWorkspace {
  id: number
  name: string
  userId: number
  agentCount: number
  maxRoundsPerTurn: number
  llmConfigId?: number
  createdAt: string
  updatedAt: string
}

export interface WorkspaceDefaults {
  workspaceId: number
  humanAgentId: number
  assistantAgentId: number
  defaultGroupId: number
}

export interface SwarmAgent {
  id: number
  workspaceId: number
  agentId?: number
  role: string
  parentId?: number
  status: AgentStatus
  createdAt: string
}

export type AgentStatus = 'IDLE' | 'BUSY' | 'WAKING' | 'STOPPED'

export interface SwarmGroup {
  id: number
  workspaceId: number
  name?: string
  memberIds: number[]
  unreadCount: number
  lastMessage?: SwarmMessage
  contextTokens: number
}

export interface SwarmMessage {
  id: number
  groupId: number
  senderId: number
  content: string
  contentType: string
  sendTime: string
}

export interface SwarmGraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export interface GraphNode {
  id: number
  role: string
  parentId?: number
  status: AgentStatus
}

export interface GraphEdge {
  from: number
  to: number
  count: number
}

export interface SwarmSearchResult {
  agents: SwarmAgent[]
  groups: SwarmGroup[]
}

// SSE 事件类型
export type AgentEventType = 'agent.stream' | 'agent.done' | 'agent.error' | 'agent.wakeup'
export type UIEventType = 'ui.agent.created' | 'ui.message.created' | 'ui.agent.llm.start' | 'ui.agent.llm.done' | 'ui.agent.tool_call.start' | 'ui.agent.tool_call.done'
