import {
  createKnowledgeDataset,
  getKnowledgeDatasetList,
  getKnowledgeDocumentList,
  uploadKnowledgeDocument,
  type CreateKnowledgeDatasetPayload,
  type KnowledgeDataset,
  type KnowledgeDocument
} from '../../../shared/api/adapters/knowledgeAdapter'

export type DatasetListItem = KnowledgeDataset
export type DocumentListItem = KnowledgeDocument

export async function fetchDatasetList(): Promise<DatasetListItem[]> {
  return getKnowledgeDatasetList()
}

export async function createDataset(payload: CreateKnowledgeDatasetPayload): Promise<DatasetListItem> {
  return createKnowledgeDataset(payload)
}

export async function fetchDocumentList(datasetId: string): Promise<DocumentListItem[]> {
  return getKnowledgeDocumentList(datasetId)
}

export async function uploadDocument(input: { datasetId: string; file: File }): Promise<DocumentListItem> {
  return uploadKnowledgeDocument(input)
}
