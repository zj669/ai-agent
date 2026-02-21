import { createAgent as createAgentApi, getAgentList, type AgentSummary } from '../../../shared/api/adapters/agentAdapter'

export type AgentListItem = AgentSummary

export type CreateAgentResult = {
  id: string
}

export async function fetchAgentList(): Promise<AgentListItem[]> {
  return getAgentList()
}

export async function createAgent(): Promise<CreateAgentResult> {
  const id = await createAgentApi({ name: '未命名 Agent' })
  return { id: String(id) }
}
