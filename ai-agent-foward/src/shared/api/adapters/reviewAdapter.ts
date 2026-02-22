import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface PendingReview {
  executionId: string
  nodeId: string
  nodeName: string
  agentName: string
  content: string
  createdAt: string
}

export async function getPendingReviews(client: ApiClientLike = apiClient): Promise<PendingReview[]> {
  const response = await client.get<ApiResponse<PendingReview[]>>('/api/workflow/reviews/pending')
  return unwrapResponse(response)
}

export interface ResumeReviewInput {
  executionId: string
  nodeId: string
  approved: boolean
  comment?: string
}

export async function resumeReview(input: ResumeReviewInput, client: ApiClientLike = apiClient): Promise<void> {
  const response = await client.post<ApiResponse<null>>('/api/workflow/reviews/resume', input)
  unwrapResponse(response)
}
