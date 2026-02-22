import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Progress,
  Space,
  Table,
  Tag,
  Tooltip,
  Upload,
} from 'antd'
import {
  DeleteOutlined,
  FileTextOutlined,
  InboxOutlined,
  PlusOutlined,
  ReloadOutlined,
  UploadOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import {
  createDataset,
  fetchDatasetList,
  fetchDocumentList,
  removeDataset,
  removeDocument,
  retryDocument,
  uploadDocument,
  type DatasetListItem,
  type DocumentListItem,
} from '../api/knowledgeService'

const STATUS_MAP: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'default', label: '等待处理' },
  PROCESSING: { color: 'processing', label: '处理中' },
  COMPLETED: { color: 'success', label: '已完成' },
  FAILED: { color: 'error', label: '失败' },
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function KnowledgePage() {
  const [datasets, setDatasets] = useState<DatasetListItem[]>([])
  const [selectedDataset, setSelectedDataset] = useState<DatasetListItem | null>(null)
  const [documents, setDocuments] = useState<DocumentListItem[]>([])
  const [loadingDatasets, setLoadingDatasets] = useState(false)
  const [loadingDocs, setLoadingDocs] = useState(false)
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [form] = Form.useForm()
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // ── 加载知识库列表 ──
  const loadDatasets = useCallback(async () => {
    setLoadingDatasets(true)
    try {
      const list = await fetchDatasetList()
      setDatasets(list)
    } catch {
      message.error('加载知识库列表失败')
    } finally {
      setLoadingDatasets(false)
    }
  }, [])

  useEffect(() => { void loadDatasets() }, [loadDatasets])

  // ── 加载文档列表 ──
  const loadDocuments = useCallback(async (datasetId: string) => {
    setLoadingDocs(true)
    try {
      const list = await fetchDocumentList(datasetId)
      setDocuments(list)
    } catch {
      message.error('加载文档列表失败')
    } finally {
      setLoadingDocs(false)
    }
  }, [])

  // ── 选中知识库时加载文档 + 轮询处理中的文档 ──
  useEffect(() => {
    if (pollingRef.current) { clearInterval(pollingRef.current); pollingRef.current = null }
    if (!selectedDataset) { setDocuments([]); return }
    void loadDocuments(selectedDataset.datasetId)
    pollingRef.current = setInterval(() => { void loadDocuments(selectedDataset.datasetId) }, 5000)
    return () => { if (pollingRef.current) clearInterval(pollingRef.current) }
  }, [selectedDataset, loadDocuments])

  // ── 创建知识库 ──
  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      setCreating(true)
      const created = await createDataset({ name: values.name, description: values.description })
      setDatasets(prev => [created, ...prev])
      setSelectedDataset(created)
      setCreateModalOpen(false)
      form.resetFields()
      message.success('知识库创建成功')
    } catch {
      message.error('创建失败')
    } finally {
      setCreating(false)
    }
  }

  // ── 删除知识库 ──
  const handleDeleteDataset = async (ds: DatasetListItem) => {
    try {
      await removeDataset(ds.datasetId)
      setDatasets(prev => prev.filter(d => d.datasetId !== ds.datasetId))
      if (selectedDataset?.datasetId === ds.datasetId) { setSelectedDataset(null) }
      message.success('已删除')
    } catch { message.error('删除失败') }
  }

  // ── 上传文档 ──
  const handleUpload = async (file: File) => {
    if (!selectedDataset) { message.warning('请先选择知识库'); return false }
    setUploading(true)
    try {
      await uploadDocument({ datasetId: selectedDataset.datasetId, file })
      void loadDocuments(selectedDataset.datasetId)
      void loadDatasets()
      message.success('上传成功，正在处理...')
    } catch { message.error('上传失败') } finally { setUploading(false) }
    return false // prevent antd auto upload
  }

  // ── 删除文档 ──
  const handleDeleteDoc = async (doc: DocumentListItem) => {
    try {
      await removeDocument(doc.documentId)
      setDocuments(prev => prev.filter(d => d.documentId !== doc.documentId))
      void loadDatasets()
      message.success('文档已删除')
    } catch { message.error('删除失败') }
  }

  // ── 重试文档 ──
  const handleRetry = async (doc: DocumentListItem) => {
    try {
      await retryDocument(doc.documentId)
      void loadDocuments(selectedDataset!.datasetId)
      message.success('已重新提交处理')
    } catch { message.error('重试失败') }
  }

  // ── 文档表格列定义 ──
  const docColumns: ColumnsType<DocumentListItem> = [
    {
      title: '文件名', dataIndex: 'filename', key: 'filename', ellipsis: true,
      render: (name: string) => <Space><FileTextOutlined />{name}</Space>,
    },
    {
      title: '大小', dataIndex: 'fileSize', key: 'fileSize', width: 100,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 120,
      render: (status: string) => {
        const info = STATUS_MAP[status] ?? { color: 'default', label: status }
        return <Tag color={info.color}>{info.label}</Tag>
      },
    },
    {
      title: '进度', key: 'progress', width: 180,
      render: (_: unknown, record: DocumentListItem) => {
        if (record.status === 'COMPLETED') return <Progress percent={100} size="small" />
        if (record.status === 'FAILED') return <Tooltip title={record.errorMessage}><Progress percent={0} status="exception" size="small" /></Tooltip>
        if (record.totalChunks > 0) {
          const pct = Math.round((record.processedChunks / record.totalChunks) * 100)
          return <Progress percent={pct} size="small" status="active" />
        }
        return <Progress percent={0} size="small" />
      },
    },
    {
      title: '上传时间', dataIndex: 'uploadedAt', key: 'uploadedAt', width: 170,
    },
    {
      title: '操作', key: 'actions', width: 120,
      render: (_: unknown, record: DocumentListItem) => (
        <Space size="small">
          {record.status === 'FAILED' && (
            <Tooltip title="重试"><Button type="link" size="small" icon={<ReloadOutlined />} onClick={() => void handleRetry(record)} /></Tooltip>
          )}
          <Popconfirm title="确认删除此文档？" onConfirm={() => void handleDeleteDoc(record)} okText="删除" cancelText="取消">
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // ── 渲染 ──
  return (
    <div style={{ display: 'flex', gap: 16, height: '100%' }}>
      {/* 左侧：知识库列表 */}
      <Card
        title="知识库"
        style={{ width: 300, flexShrink: 0, overflow: 'auto' }}
        extra={<Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => setCreateModalOpen(true)}>新建</Button>}
        loading={loadingDatasets}
        bodyStyle={{ padding: 8 }}
      >
        {datasets.length === 0 ? (
          <Empty description="暂无知识库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {datasets.map(ds => (
              <div
                key={ds.datasetId}
                onClick={() => setSelectedDataset(ds)}
                style={{
                  padding: '10px 12px',
                  borderRadius: 6,
                  cursor: 'pointer',
                  background: selectedDataset?.datasetId === ds.datasetId ? '#e6f4ff' : 'transparent',
                  border: selectedDataset?.datasetId === ds.datasetId ? '1px solid #91caff' : '1px solid transparent',
                  transition: 'all 0.2s',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 500, fontSize: 14 }}>{ds.name}</span>
                  <Popconfirm title="确认删除此知识库？" onConfirm={() => void handleDeleteDataset(ds)} okText="删除" cancelText="取消">
                    <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={e => e.stopPropagation()} />
                  </Popconfirm>
                </div>
                {ds.description && <div style={{ color: '#888', fontSize: 12, marginTop: 2 }}>{ds.description}</div>}
                <div style={{ color: '#aaa', fontSize: 12, marginTop: 4 }}>
                  {ds.documentCount} 文档 · {ds.totalChunks} 分块
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {/* 右侧：文档管理 */}
      <Card
        title={selectedDataset ? `${selectedDataset.name} - 文档管理` : '请选择知识库'}
        style={{ flex: 1, overflow: 'auto' }}
        extra={
          selectedDataset && (
            <Upload
              beforeUpload={(file) => { void handleUpload(file as unknown as File); return false }}
              showUploadList={false}
              accept=".pdf,.txt,.md,.doc,.docx,.csv"
            >
              <Button icon={<UploadOutlined />} loading={uploading} type="primary">上传文档</Button>
            </Upload>
          )
        }
      >
        {!selectedDataset ? (
          <Empty description="请从左侧选择一个知识库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        ) : (
          <>
            <Upload.Dragger
              beforeUpload={(file) => { void handleUpload(file as unknown as File); return false }}
              showUploadList={false}
              accept=".pdf,.txt,.md,.doc,.docx,.csv"
              style={{ marginBottom: 16 }}
            >
              <p className="ant-upload-drag-icon"><InboxOutlined /></p>
              <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
              <p className="ant-upload-hint">支持 PDF、TXT、Markdown、Word、CSV 格式</p>
            </Upload.Dragger>
            <Table<DocumentListItem>
              columns={docColumns}
              dataSource={documents}
              rowKey="documentId"
              loading={loadingDocs}
              size="small"
              pagination={false}
              locale={{ emptyText: <Empty description="暂无文档" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
            />
          </>
        )}
      </Card>

      {/* 创建知识库弹窗 */}
      <Modal
        title="新建知识库"
        open={createModalOpen}
        onOk={() => void handleCreate()}
        onCancel={() => { setCreateModalOpen(false); form.resetFields() }}
        confirmLoading={creating}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入知识库名称' }]}>
            <Input placeholder="输入知识库名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="可选描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default KnowledgePage
