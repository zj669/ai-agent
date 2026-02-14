import apiClient from './apiClient';
import type { ApiResponse } from '../types/auth';
import type { NodeTemplate } from '../types/workflow';

class MetadataService {
  async getNodeTemplates(): Promise<NodeTemplate[]> {
    const response = await apiClient.get<ApiResponse<NodeTemplate[]>>('/meta/node-types');
    return response.data.data;
  }
}

export const metadataService = new MetadataService();
