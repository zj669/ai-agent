import { describe, expect, it, vi } from 'vitest'
import {
  createKnowledgeDataset,
  getKnowledgeDatasetList,
  getKnowledgeDocumentList,
  uploadKnowledgeDocument,
  type KnowledgeDataset
} from '../knowledgeAdapter'
import type { ApiClientLike } from '../../client'

describe('knowledgeAdapter', () => {
  it('getKnowledgeDatasetList 应该解包后端响应', async () => {
    const data: KnowledgeDataset[] = [
      {
        datasetId: 'dataset-1',
        name: '技术文档库',
        description: '用于测试',
        userId: 1,
        documentCount: 2,
        totalChunks: 10,
        createdAt: '2026-02-20 10:00:00',
        updatedAt: '2026-02-20 10:10:00'
      }
    ]

    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data
        }
      }),
      post: vi.fn()
    }

    const result = await getKnowledgeDatasetList(client)

    expect(client.get).toHaveBeenCalledWith('/api/knowledge/dataset/list')
    expect(result).toEqual(data)
  })

  it('createKnowledgeDataset 应该调用创建接口并解包响应', async () => {
    const payload = {
      name: '产品文档库',
      description: '用于上传产品文档'
    }

    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: {
            datasetId: 'dataset-2',
            name: '产品文档库',
            description: '用于上传产品文档',
            userId: 1,
            documentCount: 0,
            totalChunks: 0,
            createdAt: '2026-02-20 11:00:00',
            updatedAt: '2026-02-20 11:00:00'
          }
        }
      })
    }

    const result = await createKnowledgeDataset(payload, client)

    expect(client.post).toHaveBeenCalledWith('/api/knowledge/dataset', payload)
    expect(result.datasetId).toBe('dataset-2')
  })

  it('uploadKnowledgeDocument 应该提交 FormData 并解包响应', async () => {
    const file = new File(['hello'], 'hello.txt', { type: 'text/plain' })

    const client: ApiClientLike = {
      get: vi.fn(),
      post: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: {
            documentId: 'doc-1',
            datasetId: 'dataset-1',
            filename: 'hello.txt',
            fileSize: 5,
            status: 'PENDING',
            totalChunks: 0,
            processedChunks: 0,
            uploadedAt: '2026-02-20 11:30:00'
          }
        }
      })
    }

    const result = await uploadKnowledgeDocument({ datasetId: 'dataset-1', file }, client)

    const calledBody = (client.post as ReturnType<typeof vi.fn>).mock.calls[0][1]
    expect(client.post).toHaveBeenCalledWith('/api/knowledge/document/upload', expect.any(FormData))
    expect(calledBody).toBeInstanceOf(FormData)
    expect((calledBody as FormData).get('datasetId')).toBe('dataset-1')
    expect((calledBody as FormData).get('file')).toBe(file)
    expect(result.documentId).toBe('doc-1')
  })

  it('getKnowledgeDocumentList 应该解包分页响应并返回 content', async () => {
    const client: ApiClientLike = {
      get: vi.fn().mockResolvedValue({
        data: {
          code: 200,
          message: 'success',
          data: {
            content: [
              {
                documentId: 'doc-1',
                datasetId: 'dataset-1',
                filename: 'api.pdf',
                fileSize: 1024,
                status: 'COMPLETED',
                totalChunks: 20,
                processedChunks: 20,
                uploadedAt: '2026-02-20 12:00:00'
              }
            ]
          }
        }
      }),
      post: vi.fn()
    }

    const result = await getKnowledgeDocumentList('dataset-1', client)

    expect(client.get).toHaveBeenCalledWith('/api/knowledge/document/list', {
      params: {
        datasetId: 'dataset-1',
        page: 0,
        size: 100
      }
    })
    expect(result).toEqual([
      {
        documentId: 'doc-1',
        datasetId: 'dataset-1',
        filename: 'api.pdf',
        fileSize: 1024,
        status: 'COMPLETED',
        totalChunks: 20,
        processedChunks: 20,
        uploadedAt: '2026-02-20 12:00:00'
      }
    ])
  })
})
