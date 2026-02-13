import { AxiosProgressEvent } from 'axios';
import {
  KnowledgeDataset,
  KnowledgeDocument,
  CreateDatasetRequest,
  SearchRequest,
  SearchResult,
  ApiResponse,
  PaginatedResponse
} from '../types/knowledge';
import apiClient from './apiClient';

class KnowledgeService {
  // ========== Dataset Management ==========

  async createDataset(data: CreateDatasetRequest): Promise<KnowledgeDataset> {
    const response = await apiClient.post<ApiResponse<KnowledgeDataset>>('/knowledge/dataset', data);
    return response.data.data;
  }

  async listDatasets(): Promise<KnowledgeDataset[]> {
    const response = await apiClient.get<ApiResponse<KnowledgeDataset[]>>('/knowledge/dataset/list');
    return response.data.data;
  }

  async getDataset(datasetId: string): Promise<KnowledgeDataset> {
    const response = await apiClient.get<ApiResponse<KnowledgeDataset>>(`/knowledge/dataset/${datasetId}`);
    return response.data.data;
  }

  async deleteDataset(datasetId: string): Promise<void> {
    await apiClient.delete(`/knowledge/dataset/${datasetId}`);
  }

  // ========== Document Management ==========

  async uploadDocument(
    file: File,
    datasetId: string,
    chunkSize: number = 500,
    chunkOverlap: number = 50,
    onProgress?: (progress: number) => void
  ): Promise<KnowledgeDocument> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('datasetId', datasetId);
    formData.append('chunkSize', chunkSize.toString());
    formData.append('chunkOverlap', chunkOverlap.toString());

    const response = await apiClient.post<ApiResponse<KnowledgeDocument>>(
      '/knowledge/document/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent: AxiosProgressEvent) => {
          if (progressEvent.total && onProgress) {
            const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            onProgress(percentCompleted);
          }
        }
      }
    );

    return response.data.data;
  }

  async listDocuments(
    datasetId: string,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<KnowledgeDocument>> {
    const response = await apiClient.get<ApiResponse<PaginatedResponse<KnowledgeDocument>>>(
      '/knowledge/document/list',
      {
        params: { datasetId, page, size }
      }
    );
    return response.data.data;
  }

  async getDocument(documentId: string): Promise<KnowledgeDocument> {
    const response = await apiClient.get<ApiResponse<KnowledgeDocument>>(`/knowledge/document/${documentId}`);
    return response.data.data;
  }

  async deleteDocument(documentId: string): Promise<void> {
    await apiClient.delete(`/knowledge/document/${documentId}`);
  }

  async retryDocument(documentId: string): Promise<KnowledgeDocument> {
    const response = await apiClient.post<ApiResponse<KnowledgeDocument>>(
      `/knowledge/document/${documentId}/retry`
    );
    return response.data.data;
  }

  // ========== Knowledge Retrieval ==========

  async search(data: SearchRequest): Promise<string[]> {
    const response = await apiClient.post<ApiResponse<string[]>>('/knowledge/search', data);
    return response.data.data;
  }
}

export const knowledgeService = new KnowledgeService();
