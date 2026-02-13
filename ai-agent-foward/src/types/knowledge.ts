export interface KnowledgeDataset {
  datasetId: string;
  name: string;
  description: string;
  userId: number;
  agentId?: number;
  documentCount: number;
  totalChunks: number;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocument {
  documentId: string;
  datasetId: string;
  filename: string;
  fileUrl: string;
  fileSize: number;
  contentType: string;
  status: DocumentStatus;
  totalChunks: number;
  processedChunks: number;
  errorMessage?: string;
  uploadedAt: string;
  completedAt?: string;
}

export enum DocumentStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export interface CreateDatasetRequest {
  name: string;
  description: string;
  agentId?: number;
}

export interface UploadDocumentRequest {
  file: File;
  datasetId: string;
  chunkSize?: number;
  chunkOverlap?: number;
}

export interface SearchRequest {
  datasetId: string;
  query: string;
  topK: number;
}

export interface SearchResult {
  content: string;
  score: number;
  documentId: string;
  filename: string;
  chunkIndex: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
