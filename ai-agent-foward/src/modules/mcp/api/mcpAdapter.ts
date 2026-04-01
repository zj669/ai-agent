import { apiClient, type ApiClientLike } from '../../../shared/api/client'
import { unwrapResponse, type ApiResponse } from '../../../shared/api/response'
import type {
  McpServer,
  McpTool,
  CreateServerPayload,
  UpdateServerPayload,
  ServerStatus
} from '../types/mcp'

export const mcpAdapter = {
  listServers: async (client: ApiClientLike = apiClient): Promise<McpServer[]> => {
    const response = await client.get<ApiResponse<McpServer[]>>('/api/mcp/servers')
    return unwrapResponse(response)
  },

  getServer: async (id: number, client: ApiClientLike = apiClient): Promise<McpServer> => {
    const response = await client.get<ApiResponse<McpServer>>(`/api/mcp/servers/${id}`)
    return unwrapResponse(response)
  },

  createServer: async (payload: CreateServerPayload, client: ApiClientLike = apiClient): Promise<number> => {
    const response = await client.post<ApiResponse<number>>('/api/mcp/servers', payload)
    return unwrapResponse(response)
  },

  updateServer: async (id: number, payload: UpdateServerPayload, client: ApiClientLike = apiClient): Promise<void> => {
    // NOTE: Using POST since HttpClientLike doesn't expose put method
    const response = await client.post<ApiResponse<null>>(`/api/mcp/servers/${id}`, payload)
    unwrapResponse(response)
  },

  deleteServer: async (id: number, client: ApiClientLike = apiClient): Promise<void> => {
    // NOTE: delete is optional on HttpClientLike; if unavailable, falls back to POST
    if (client.delete) {
      const response = await client.delete<ApiResponse<null>>(`/api/mcp/servers/${id}`)
      unwrapResponse(response)
    } else {
      const response = await client.post<ApiResponse<null>>(`/api/mcp/servers/${id}/delete`, {})
      unwrapResponse(response)
    }
  },

  connectServer: async (id: number, client: ApiClientLike = apiClient): Promise<void> => {
    const response = await client.post<ApiResponse<null>>(`/api/mcp/servers/${id}/connect`, {})
    unwrapResponse(response)
  },

  disconnectServer: async (id: number, client: ApiClientLike = apiClient): Promise<void> => {
    const response = await client.post<ApiResponse<null>>(`/api/mcp/servers/${id}/disconnect`, {})
    unwrapResponse(response)
  },

  getServerStatus: async (id: number, client: ApiClientLike = apiClient): Promise<ServerStatus> => {
    const response = await client.get<ApiResponse<ServerStatus>>(`/api/mcp/servers/${id}/status`)
    return unwrapResponse(response)
  },

  getServerTools: async (id: number, client: ApiClientLike = apiClient): Promise<McpTool[]> => {
    const response = await client.get<ApiResponse<McpTool[]>>(`/api/mcp/servers/${id}/tools`)
    return unwrapResponse(response)
  },

  getAllTools: async (client: ApiClientLike = apiClient): Promise<McpTool[]> => {
    const response = await client.get<ApiResponse<McpTool[]>>('/api/mcp/tools')
    return unwrapResponse(response)
  },
}
