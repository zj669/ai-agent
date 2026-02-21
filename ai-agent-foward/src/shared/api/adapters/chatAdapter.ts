import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface ConversationSummary {
  id: string
  userId: string
  agentId: string
  title: string
  createdAt: string
  updatedAt: string
}

export interface ConversationListData {
  total: number
  pages: number
  list: ConversationSummary[]
}

export type MessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM'
export type MessageStatus = 'PENDING' | 'STREAMING' | 'COMPLETED' | 'FAILED'

export interface MessageDTO {
  id: string
  conversationId: string
  role: MessageRole
  content: string
  status: MessageStatus
  createdAt: string
}

export interface CreateConversationInput {
  userId: number
  agentId: number
}

export interface ListConversationInput {
  userId: number
  agentId: number
  page?: number
  size?: number
}

export interface ListMessagesInput {
  conversationId: string
  userId: number
  page?: number
  size?: number
  order?: 'asc' | 'desc'
}

export interface StopExecutionInput {
  executionId: string
}

export async function createConversation(
  input: CreateConversationInput,
  client: ApiClientLike = apiClient
): Promise<string> {
  const response = await client.post<ApiResponse<string>>('/api/chat/conversations', undefined, {
    params: {
      userId: input.userId,
      agentId: input.agentId
    }
  })

  return unwrapResponse(response)
}

export async function getConversationList(
  input: ListConversationInput,
  client: ApiClientLike = apiClient
): Promise<ConversationListData> {
  const response = await client.get<ApiResponse<ConversationListData>>('/api/chat/conversations', {
    params: {
      userId: input.userId,
      agentId: input.agentId,
      page: input.page ?? 1,
      size: input.size ?? 20
    }
  })

  return unwrapResponse(response)
}

export async function getConversationMessages(
  input: ListMessagesInput,
  client: ApiClientLike = apiClient
): Promise<MessageDTO[]> {
  const response = await client.get<ApiResponse<MessageDTO[]>>(`/api/chat/conversations/${input.conversationId}/messages`, {
    params: {
      userId: input.userId,
      page: input.page ?? 1,
      size: input.size ?? 50,
      order: input.order ?? 'asc'
    }
  })

  return unwrapResponse(response)
}

export async function stopWorkflowExecution(
  input: StopExecutionInput,
  client: ApiClientLike = apiClient
): Promise<void> {
  const response = await client.post<ApiResponse<null>>('/api/workflow/execution/stop', input)
  unwrapResponse(response)
}
