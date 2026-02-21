import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, vi } from 'vitest'
import KnowledgePage from '../pages/KnowledgePage'

const {
  createDatasetMock,
  fetchDatasetListMock,
  fetchDocumentListMock,
  uploadDocumentMock
} = vi.hoisted(() => ({
  createDatasetMock: vi.fn(),
  fetchDatasetListMock: vi.fn(),
  fetchDocumentListMock: vi.fn(),
  uploadDocumentMock: vi.fn()
}))

vi.mock('../api/knowledgeService', () => ({
  createDataset: (...args: unknown[]) => createDatasetMock(...args),
  fetchDatasetList: (...args: unknown[]) => fetchDatasetListMock(...args),
  fetchDocumentList: (...args: unknown[]) => fetchDocumentListMock(...args),
  uploadDocument: (...args: unknown[]) => uploadDocumentMock(...args)
}))

describe('knowledge main flow', () => {
  beforeEach(() => {
    createDatasetMock.mockReset()
    fetchDatasetListMock.mockReset()
    fetchDocumentListMock.mockReset()
    uploadDocumentMock.mockReset()

    fetchDatasetListMock.mockResolvedValue([
      {
        datasetId: 'dataset-1',
        name: '技术文档库',
        description: '用于主链测试',
        userId: 1,
        documentCount: 1,
        totalChunks: 12,
        createdAt: '2026-02-20 10:00:00',
        updatedAt: '2026-02-20 10:10:00'
      }
    ])

    fetchDocumentListMock.mockResolvedValue([
      {
        documentId: 'doc-1',
        datasetId: 'dataset-1',
        filename: 'api.pdf',
        fileSize: 1024,
        status: 'PROCESSING',
        totalChunks: 20,
        processedChunks: 7,
        uploadedAt: '2026-02-20 11:00:00'
      }
    ])

    createDatasetMock.mockResolvedValue({
      datasetId: 'dataset-2',
      name: '产品文档库',
      description: '新建成功',
      userId: 1,
      documentCount: 0,
      totalChunks: 0,
      createdAt: '2026-02-20 11:30:00',
      updatedAt: '2026-02-20 11:30:00'
    })

    uploadDocumentMock.mockResolvedValue({
      documentId: 'doc-2',
      datasetId: 'dataset-1',
      filename: 'guide.txt',
      fileSize: 20,
      status: 'PENDING',
      totalChunks: 0,
      processedChunks: 0,
      uploadedAt: '2026-02-20 12:00:00'
    })
  })

  it('首屏加载知识库并展示文档状态', async () => {
    render(<KnowledgePage />)

    expect(await screen.findByRole('option', { name: '技术文档库' })).toBeInTheDocument()
    expect(await screen.findByText('api.pdf')).toBeInTheDocument()
    expect(screen.getByText('状态: PROCESSING')).toBeInTheDocument()
  })

  it('创建知识库成功后刷新列表并展示成功反馈', async () => {
    render(<KnowledgePage />)

    await screen.findByRole('option', { name: '技术文档库' })
    fireEvent.change(screen.getByLabelText('dataset-name'), { target: { value: '产品文档库' } })
    fireEvent.change(screen.getByLabelText('dataset-description'), { target: { value: '新建成功' } })
    fireEvent.click(screen.getByRole('button', { name: '创建知识库' }))

    await waitFor(() => {
      expect(createDatasetMock).toHaveBeenCalledWith({
        name: '产品文档库',
        description: '新建成功'
      })
    })

    expect(await screen.findByText('知识库创建成功')).toBeInTheDocument()
    expect(screen.getByText('新建成功')).toBeInTheDocument()
  })

  it('创建失败时显示显式错误反馈', async () => {
    createDatasetMock.mockRejectedValueOnce(new Error('create failed'))
    render(<KnowledgePage />)

    await screen.findByRole('option', { name: '技术文档库' })
    fireEvent.change(screen.getByLabelText('dataset-name'), { target: { value: '失败知识库' } })
    fireEvent.click(screen.getByRole('button', { name: '创建知识库' }))

    expect(await screen.findByText('创建知识库失败，请稍后重试')).toBeInTheDocument()
  })

  it('上传文档成功后显示反馈并刷新文档列表', async () => {
    render(<KnowledgePage />)

    await screen.findByRole('option', { name: '技术文档库' })
    const file = new File(['hello knowledge'], 'guide.txt', { type: 'text/plain' })
    fireEvent.change(screen.getByLabelText('document-file'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: '上传文档' }))

    await waitFor(() => {
      expect(uploadDocumentMock).toHaveBeenCalledWith({
        datasetId: 'dataset-1',
        file
      })
    })

    expect(await screen.findByText('上传成功')).toBeInTheDocument()
    expect(fetchDocumentListMock).toHaveBeenCalledWith('dataset-1')
  })

  it('上传失败时显示显式错误反馈', async () => {
    uploadDocumentMock.mockRejectedValueOnce(new Error('upload failed'))
    render(<KnowledgePage />)

    await screen.findByRole('option', { name: '技术文档库' })
    const file = new File(['hello knowledge'], 'guide.txt', { type: 'text/plain' })
    fireEvent.change(screen.getByLabelText('document-file'), { target: { files: [file] } })
    fireEvent.click(screen.getByRole('button', { name: '上传文档' }))

    expect(await screen.findByText('上传失败，请稍后重试')).toBeInTheDocument()
  })

  it('知识库列表加载失败时显示最小错误提示', async () => {
    fetchDatasetListMock.mockRejectedValueOnce(new Error('list failed'))
    render(<KnowledgePage />)

    expect(await screen.findByText('知识库列表加载失败，请稍后重试')).toBeInTheDocument()
  })
})
