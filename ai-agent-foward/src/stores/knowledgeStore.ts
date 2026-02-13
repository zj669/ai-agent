import { create } from 'zustand';
import { KnowledgeDataset, KnowledgeDocument } from '../types/knowledge';

interface KnowledgeState {
  datasets: KnowledgeDataset[];
  currentDatasetId: string | null;
  documents: KnowledgeDocument[];
  documentPage: number;
  documentPageSize: number;
  documentTotal: number;
  isLoading: boolean;
  isUploading: boolean;
  uploadProgress: number;

  // Actions
  setDatasets: (datasets: KnowledgeDataset[]) => void;
  setCurrentDataset: (datasetId: string | null) => void;
  setDocuments: (documents: KnowledgeDocument[]) => void;
  setDocumentPagination: (page: number, pageSize: number, total: number) => void;
  addDocument: (document: KnowledgeDocument) => void;
  updateDocument: (documentId: string, updates: Partial<KnowledgeDocument>) => void;
  removeDocument: (documentId: string) => void;
  setLoading: (isLoading: boolean) => void;
  setUploading: (isUploading: boolean) => void;
  setUploadProgress: (progress: number) => void;
}

export const useKnowledgeStore = create<KnowledgeState>((set) => ({
  datasets: [],
  currentDatasetId: null,
  documents: [],
  documentPage: 0,
  documentPageSize: 10,
  documentTotal: 0,
  isLoading: false,
  isUploading: false,
  uploadProgress: 0,

  setDatasets: (datasets) => set({ datasets }),

  setCurrentDataset: (datasetId) => set({ currentDatasetId: datasetId }),

  setDocuments: (documents) => set({ documents }),

  setDocumentPagination: (page, pageSize, total) =>
    set({
      documentPage: page,
      documentPageSize: pageSize,
      documentTotal: total
    }),

  addDocument: (document) =>
    set((state) => ({
      documents: [...state.documents, document]
    })),

  updateDocument: (documentId, updates) =>
    set((state) => ({
      documents: state.documents.map((doc) =>
        doc.documentId === documentId ? { ...doc, ...updates } : doc
      )
    })),

  removeDocument: (documentId) =>
    set((state) => ({
      documents: state.documents.filter((doc) => doc.documentId !== documentId)
    })),

  setLoading: (isLoading) => set({ isLoading }),

  setUploading: (isUploading) => set({ isUploading }),

  setUploadProgress: (progress) => set({ uploadProgress: progress })
}));
