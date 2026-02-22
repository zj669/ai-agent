import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface DashboardStats {
  agentCount: number
  publishedAgentCount: number
  conversationCount: number
  executionCount: number
  pendingReviewCount: number
}

export async function getDashboardStats(client: ApiClientLike = apiClient): Promise<DashboardStats> {
  const response = await client.get<ApiResponse<DashboardStats>>('/api/dashboard/stats')
  return unwrapResponse(response)
}
