import { apiClient } from '../../../shared/api/client'
import { unwrapResponse, type ApiResponse } from '../../../shared/api/response'

// --- Types ---

export interface LlmConfig {
  id: number
  name: string
  provider: string
  baseUrl: string
  model: string
  isDefault: boolean
  status: number
  createdAt: string
  updatedAt: string
}

export interface CreateLlmConfigPayload {
  name: string
  provider: string
  baseUrl: string
  apiKey: string
  model: string
}

export interface UpdateLlmConfigPayload {
  name?: string
  baseUrl?: string
  apiKey?: string
  model?: string
  isDefault?: boolean
}

export interface TestResult {
  ok: boolean
  latencyMs?: number
  error?: string
}

// --- API ---

export async function getLlmConfigs(): Promise<LlmConfig[]> {
  const response = await apiClient.get<ApiResponse<LlmConfig[]>>('/api/llm-config')
  return unwrapResponse(response)
}

export async function createLlmConfig(payload: CreateLlmConfigPayload): Promise<number> {
  const response = await apiClient.post<ApiResponse<number>>('/api/llm-config', payload)
  return unwrapResponse(response)
}

export async function updateLlmConfig(id: number, payload: UpdateLlmConfigPayload): Promise<void> {
  const response = await apiClient.put<ApiResponse<null>>(`/api/llm-config/${id}`, payload)
  unwrapResponse(response)
}

export async function deleteLlmConfig(id: number): Promise<void> {
  const response = await apiClient.delete<ApiResponse<null>>(`/api/llm-config/${id}`)
  unwrapResponse(response)
}

export async function testLlmConfig(id: number): Promise<TestResult> {
  const response = await apiClient.post<ApiResponse<TestResult>>(`/api/llm-config/${id}/test`)
  return unwrapResponse(response)
}
