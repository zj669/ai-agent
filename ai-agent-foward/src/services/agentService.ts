import apiClient from './apiClient';
import type { ApiResponse } from '../types/auth';
import type {
  AgentSummary,
  AgentDetail,
  CreateAgentRequest,
  UpdateAgentRequest,
  PublishAgentRequest,
  RollbackAgentRequest,
  VersionHistory
} from '../types/agent';

class AgentService {
  // Query
  async listAgents(): Promise<AgentSummary[]> {
    const response = await apiClient.get<ApiResponse<AgentSummary[]>>('/agent/list');
    return response.data.data;
  }

  async getAgent(id: number): Promise<AgentDetail> {
    const response = await apiClient.get<ApiResponse<AgentDetail>>(`/agent/${id}`);
    return response.data.data;
  }

  async getVersionHistory(id: number): Promise<VersionHistory> {
    const response = await apiClient.get<ApiResponse<VersionHistory>>(`/agent/${id}/versions`);
    return response.data.data;
  }

  // Command
  async createAgent(data: CreateAgentRequest): Promise<number> {
    const response = await apiClient.post<ApiResponse<number>>('/agent/create', data);
    return response.data.data;
  }

  async updateAgent(data: UpdateAgentRequest): Promise<void> {
    await apiClient.post<ApiResponse<void>>('/agent/update', data);
  }

  async publishAgent(data: PublishAgentRequest): Promise<void> {
    await apiClient.post<ApiResponse<void>>('/agent/publish', data);
  }

  async rollbackAgent(data: RollbackAgentRequest): Promise<void> {
    await apiClient.post<ApiResponse<void>>('/agent/rollback', data);
  }

  async deleteAgentVersion(id: number, version: number): Promise<void> {
    await apiClient.delete(`/agent/${id}/versions/${version}`);
  }

  async forceDeleteAgent(id: number): Promise<void> {
    await apiClient.delete(`/agent/${id}/force`);
  }
}

export const agentService = new AgentService();
