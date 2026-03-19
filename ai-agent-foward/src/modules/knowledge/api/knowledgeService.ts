import {
  createKnowledgeDataset,
  deleteKnowledgeDataset,
  deleteKnowledgeDocument,
  getKnowledgeDatasetList,
  getKnowledgeDocumentList,
  retryKnowledgeDocument,
  uploadKnowledgeDocument,
  type ChunkStrategy,
  type CreateKnowledgeDatasetPayload,
  type FixedChunkingUploadConfig,
  type KnowledgeDataset,
  type KnowledgeDocument,
  type SemanticChunkingUploadConfig,
} from "../../../shared/api/adapters/knowledgeAdapter";

export type DatasetListItem = KnowledgeDataset;
export type DocumentListItem = KnowledgeDocument;
export type KnowledgeChunkStrategy = ChunkStrategy;
export type FixedChunkConfig = FixedChunkingUploadConfig;
export type SemanticChunkConfig = SemanticChunkingUploadConfig;

export async function fetchDatasetList(): Promise<DatasetListItem[]> {
  return getKnowledgeDatasetList();
}

export async function createDataset(
  payload: CreateKnowledgeDatasetPayload,
): Promise<DatasetListItem> {
  return createKnowledgeDataset(payload);
}

export async function removeDataset(datasetId: string): Promise<void> {
  return deleteKnowledgeDataset(datasetId);
}

export async function fetchDocumentList(
  datasetId: string,
): Promise<DocumentListItem[]> {
  return getKnowledgeDocumentList(datasetId);
}

export async function uploadDocument(
  input: { datasetId: string; file: File } & (
    | FixedChunkConfig
    | SemanticChunkConfig
  ),
): Promise<DocumentListItem> {
  return uploadKnowledgeDocument(input);
}

export async function removeDocument(documentId: string): Promise<void> {
  return deleteKnowledgeDocument(documentId);
}

export async function retryDocument(
  documentId: string,
): Promise<DocumentListItem> {
  return retryKnowledgeDocument(documentId);
}
