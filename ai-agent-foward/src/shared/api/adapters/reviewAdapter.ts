import { apiClient, type ApiClientLike } from '../client'

export interface PendingReview {
  executionId: string
  nodeId: string
  nodeName: string
  agentName: string
  triggerPhase: 'BEFORE_EXECUTION' | 'AFTER_EXECUTION'
  pausedAt: string
  userId: string | null
  content?: string
}

export async function getPendingReviews(client: ApiClientLike = apiClient): Promise<PendingReview[]> {
  const response = await client.get<PendingReview[]>('/api/workflow/reviews/pending')
  return response.data
}

export interface ResumeReviewInput {
  executionId: string
  nodeId: string
  approved: boolean
  comment?: string
}

export async function resumeReview(input: ResumeReviewInput, client: ApiClientLike = apiClient): Promise<void> {
  await client.post('/api/workflow/reviews/resume', input)
}
