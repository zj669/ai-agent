import { apiClient, type ApiClientLike } from '../client'
import { unwrapResponse, type ApiResponse } from '../response'

export interface KnowledgeDataset {
  datasetId: string
  name: string
  description?: string
  userId: number
  agentId?: number
  documentCount: number
  totalChunks: number
  createdAt: string
  updatedAt: string
}

export interface CreateKnowledgeDatasetPayload {
  name: string
  description?: string
  agentId?: number
}

export interface KnowledgeDocument {
  documentId: string
  datasetId: string
  filename: string
  fileUrl?: string
  fileSize: number
  contentType?: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | string
  totalChunks: number
  processedChunks: number
  errorMessage?: string
  uploadedAt: string
  completedAt?: string
}

interface PageResponse<T> {
  content: T[]
}

export async function getKnowledgeDatasetList(client: ApiClientLike = apiClient): Promise<KnowledgeDataset[]> {
  const response = await client.get<ApiResponse<KnowledgeDataset[]>>('/api/knowledge/dataset/list')
  return unwrapResponse(response)
}

export async function createKnowledgeDataset(
  payload: CreateKnowledgeDatasetPayload,
  client: ApiClientLike = apiClient
): Promise<KnowledgeDataset> {
  const response = await client.post<ApiResponse<KnowledgeDataset>>('/api/knowledge/dataset', payload)
  return unwrapResponse(response)
}

export async function deleteKnowledgeDataset(
  datasetId: string,
  client: ApiClientLike = apiClient
): Promise<void> {
  const c = client as ApiClientLike & { delete: ApiClientLike['get'] }
  await c.delete<ApiResponse<void>>(`/api/knowledge/dataset/${datasetId}`)
}

export async function uploadKnowledgeDocument(
  input: {
    datasetId: string
    file: File
    chunkSize?: number
    chunkOverlap?: number
  },
  client: ApiClientLike = apiClient
): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('datasetId', input.datasetId)
  formData.append('file', input.file)

  if (input.chunkSize !== undefined) {
    formData.append('chunkSize', String(input.chunkSize))
  }

  if (input.chunkOverlap !== undefined) {
    formData.append('chunkOverlap', String(input.chunkOverlap))
  }

  const response = await client.post<ApiResponse<KnowledgeDocument>>('/api/knowledge/document/upload', formData)
  return unwrapResponse(response)
}

export async function getKnowledgeDocumentList(
  datasetId: string,
  client: ApiClientLike = apiClient
): Promise<KnowledgeDocument[]> {
  const response = await client.get<ApiResponse<PageResponse<KnowledgeDocument>>>('/api/knowledge/document/list', {
    params: {
      datasetId,
      page: 0,
      size: 100
    }
  })
  return unwrapResponse(response).content
}

export async function deleteKnowledgeDocument(
  documentId: string,
  client: ApiClientLike = apiClient
): Promise<void> {
  const c = client as ApiClientLike & { delete: ApiClientLike['get'] }
  await c.delete<ApiResponse<void>>(`/api/knowledge/document/${documentId}`)
}

export async function retryKnowledgeDocument(
  documentId: string,
  client: ApiClientLike = apiClient
): Promise<KnowledgeDocument> {
  const response = await client.post<ApiResponse<KnowledgeDocument>>(`/api/knowledge/document/${documentId}/retry`)
  return unwrapResponse(response)
}
