import { useCallback, useEffect } from 'react';
import { message as antdMessage, Modal } from 'antd';
import { knowledgeService } from '../services/knowledgeService';
import { useKnowledgeStore } from '../stores/knowledgeStore';
import { CreateDatasetRequest, SearchRequest } from '../types/knowledge';

export const useKnowledge = () => {
  const {
    datasets,
    currentDatasetId,
    documents,
    documentPage,
    documentPageSize,
    documentTotal,
    isLoading,
    isUploading,
    uploadProgress,
    setDatasets,
    setCurrentDataset,
    setDocuments,
    setDocumentPagination,
    setLoading,
    setUploading,
    setUploadProgress
  } = useKnowledgeStore();

  // Load datasets
  const loadDatasets = useCallback(async () => {
    setLoading(true);
    try {
      const data = await knowledgeService.listDatasets();
      setDatasets(data);
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '加载知识库列表失败');
    } finally {
      setLoading(false);
    }
  }, [setDatasets, setLoading]);

  // Create dataset
  const createDataset = async (data: CreateDatasetRequest) => {
    try {
      const dataset = await knowledgeService.createDataset(data);
      await loadDatasets();
      setCurrentDataset(dataset.datasetId);
      antdMessage.success('创建成功');
      return dataset;
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '创建失败');
      throw error;
    }
  };

  // Delete dataset
  const deleteDataset = async (datasetId: string, name: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除知识库 "${name}" 吗？此操作将删除所有文档且不可恢复。`,
      okText: '确认',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await knowledgeService.deleteDataset(datasetId);
          antdMessage.success('删除成功');
          await loadDatasets();
          if (currentDatasetId === datasetId) {
            setCurrentDataset(null);
            setDocuments([]);
            setDocumentPagination(0, documentPageSize, 0);
          }
        } catch (error: any) {
          antdMessage.error(error.response?.data?.message || '删除失败');
        }
      }
    });
  };

  // Load documents
  const loadDocuments = useCallback(async (
    datasetId: string,
    page: number = 0,
    size: number = documentPageSize
  ) => {
    setLoading(true);
    try {
      const result = await knowledgeService.listDocuments(datasetId, page, size);
      setDocuments(result.content);
      setDocumentPagination(result.number, result.size, result.totalElements);
      return result;
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '加载文档列表失败');
      throw error;
    } finally {
      setLoading(false);
    }
  }, [documentPageSize, setDocumentPagination, setDocuments, setLoading]);

  // Upload document
  const uploadDocument = async (
    file: File,
    datasetId: string,
    chunkSize: number = 500,
    chunkOverlap: number = 50
  ) => {
    setUploading(true);
    setUploadProgress(0);

    try {
      const document = await knowledgeService.uploadDocument(
        file,
        datasetId,
        chunkSize,
        chunkOverlap,
        (progress) => {
          setUploadProgress(progress);
        }
      );

      await loadDocuments(datasetId, 0, documentPageSize);
      antdMessage.success('上传成功');
      return document;
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '上传失败');
      throw error;
    } finally {
      setUploading(false);
      setUploadProgress(0);
    }
  };

  // Delete document
  const deleteDocument = async (documentId: string, filename: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除文档 "${filename}" 吗？此操作不可恢复。`,
      okText: '确认',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await knowledgeService.deleteDocument(documentId);
          antdMessage.success('删除成功');
          if (currentDatasetId) {
            const nextPage = documents.length === 1 && documentPage > 0
              ? documentPage - 1
              : documentPage;
            await loadDocuments(currentDatasetId, nextPage, documentPageSize);
          }
        } catch (error: any) {
          antdMessage.error(error.response?.data?.message || '删除失败');
        }
      }
    });
  };

  // Retry failed document
  const retryDocument = async (documentId: string) => {
    try {
      await knowledgeService.retryDocument(documentId);
      antdMessage.success('已重新提交处理');
      if (currentDatasetId) {
        await loadDocuments(currentDatasetId, documentPage, documentPageSize);
      }
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '重试失败');
      throw error;
    }
  };

  // Search knowledge
  const searchKnowledge = async (data: SearchRequest) => {
    try {
      const results = await knowledgeService.search(data);
      return results;
    } catch (error: any) {
      antdMessage.error(error.response?.data?.message || '检索失败');
      throw error;
    }
  };

  // Load datasets on mount
  useEffect(() => {
    loadDatasets();
  }, [loadDatasets]);

  // Load documents when dataset changes
  useEffect(() => {
    if (currentDatasetId) {
      loadDocuments(currentDatasetId, 0, documentPageSize);
    } else {
      setDocuments([]);
      setDocumentPagination(0, documentPageSize, 0);
    }
  }, [currentDatasetId, documentPageSize, loadDocuments, setDocumentPagination, setDocuments]);

  return {
    datasets,
    currentDatasetId,
    documents,
    documentPage,
    documentPageSize,
    documentTotal,
    isLoading,
    isUploading,
    uploadProgress,
    loadDatasets,
    createDataset,
    deleteDataset,
    loadDocuments,
    uploadDocument,
    deleteDocument,
    retryDocument,
    searchKnowledge,
    setCurrentDataset
  };
};
