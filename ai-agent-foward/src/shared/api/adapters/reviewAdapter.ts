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

export interface NodeContext {
  nodeId: string
  nodeName: string
  nodeType: string
  status: string
  inputs?: Record<string, unknown>
  outputs?: Record<string, unknown>
}

export interface ReviewDetail {
  executionId: string
  nodeId: string
  nodeName: string
  triggerPhase: 'BEFORE_EXECUTION' | 'AFTER_EXECUTION'
  nodes: NodeContext[]
}

export async function getPendingReviews(client: ApiClientLike = apiClient): Promise<PendingReview[]> {
  const response = await client.get<PendingReview[]>('/api/workflow/reviews/pending')
  return response.data
}

export async function getReviewDetail(executionId: string, client: ApiClientLike = apiClient): Promise<ReviewDetail> {
  const response = await client.get<ReviewDetail>(`/api/workflow/reviews/${executionId}`)
  return response.data
}

export interface ResumeReviewInput {
  executionId: string
  nodeId: string
  edits?: Record<string, unknown>
  comment?: string
  nodeEdits?: Record<string, Record<string, unknown>>
}

export async function resumeReview(input: ResumeReviewInput, client: ApiClientLike = apiClient): Promise<void> {
  await client.post('/api/workflow/reviews/resume', input)
}
