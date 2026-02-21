import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface AgentSummary {
  id: number
  userId: number
  name: string
  description?: string
  icon?: string
  status: string
  publishedVersionId?: number
  updateTime: string
}

export interface AgentDetail {
  id: number
  name: string
  description?: string
  icon?: string
  graphJson?: string
  version: number
  publishedVersionId?: number
  status: number
}

export interface CreateAgentPayload {
  name: string
  description?: string
  icon?: string
}

export interface UpdateAgentPayload {
  id: number
  name: string
  description?: string
  icon?: string
  graphJson?: string
  version: number
}

export interface PublishAgentPayload {
  id: number
}

export async function getAgentList(client: ApiClientLike = apiClient): Promise<AgentSummary[]> {
  const response = await client.get<ApiResponse<AgentSummary[]>>('/api/agent/list')
  return unwrapResponse(response)
}

export async function getAgentDetail(id: number, client: ApiClientLike = apiClient): Promise<AgentDetail> {
  const response = await client.get<ApiResponse<AgentDetail>>(`/api/agent/${id}`)
  return unwrapResponse(response)
}

export async function createAgent(payload: CreateAgentPayload, client: ApiClientLike = apiClient): Promise<number> {
  const response = await client.post<ApiResponse<number>>('/api/agent/create', payload)
  return unwrapResponse(response)
}

export async function updateAgent(payload: UpdateAgentPayload, client: ApiClientLike = apiClient): Promise<void> {
  const response = await client.post<ApiResponse<null>>('/api/agent/update', payload)
  unwrapResponse(response)
}

export async function publishAgent(payload: PublishAgentPayload, client: ApiClientLike = apiClient): Promise<void> {
  const response = await client.post<ApiResponse<null>>('/api/agent/publish', payload)
  unwrapResponse(response)
}
