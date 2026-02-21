import {
  getAgentDetail,
  publishAgent,
  updateAgent,
  type AgentDetail,
  type UpdateAgentPayload
} from '../../../shared/api/adapters/agentAdapter'

export interface WorkflowGraph {
  [key: string]: unknown
}

export interface WorkflowDetail {
  agentId: number
  version: number
  name: string
  description?: string
  icon?: string
  graphJson?: string
  graph: WorkflowGraph | null
}

export interface SaveWorkflowInput {
  agentId: number
  version: number
  graph: WorkflowGraph
  name: string
  description?: string
  icon?: string
}

function parseGraphJson(graphJson?: string): WorkflowGraph | null {
  if (!graphJson) {
    return null
  }

  try {
    return JSON.parse(graphJson) as WorkflowGraph
  } catch {
    return null
  }
}

function toWorkflowDetail(agent: AgentDetail): WorkflowDetail {
  return {
    agentId: agent.id,
    version: agent.version,
    name: agent.name,
    description: agent.description,
    icon: agent.icon,
    graphJson: agent.graphJson,
    graph: parseGraphJson(agent.graphJson)
  }
}

export async function fetchWorkflowDetail(agentId: number): Promise<WorkflowDetail> {
  const agent = await getAgentDetail(agentId)
  return toWorkflowDetail(agent)
}

export async function saveWorkflow(input: SaveWorkflowInput): Promise<WorkflowDetail> {
  const payload: UpdateAgentPayload = {
    id: input.agentId,
    name: input.name,
    description: input.description,
    icon: input.icon,
    version: input.version,
    graphJson: JSON.stringify(input.graph)
  }

  await updateAgent(payload)

  return fetchWorkflowDetail(input.agentId)
}

export async function publishWorkflow(agentId: number): Promise<WorkflowDetail> {
  await publishAgent({ id: agentId })
  return fetchWorkflowDetail(agentId)
}
