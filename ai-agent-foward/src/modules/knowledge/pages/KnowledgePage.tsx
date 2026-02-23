import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Badge,
  Button,
  Empty,
  Form,
  Input,
  message,
  Modal,
  Popconfirm,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  Upload,
} from 'antd'
import {
  DatabaseOutlined,
  DeleteOutlined,
  FileTextOutlined,
  FolderOutlined,
  InboxOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
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

const { Text } = Typography

const STATUS_MAP: Record<string, { color: string; label: string }> = {
  PENDING: { color: 'default', label: '等待中' },
  PROCESSING: { color: 'processing', label: '处理中' },
  COMPLETED: { color: 'green', label: '已完成' },
  FAILED: { color: 'red', label: '失败' },
}

export default function KnowledgePage() {
  const [datasets, setDatasets] = useState<DatasetListItem[]>([])
  const [selectedDataset, setSelectedDataset] = useState<DatasetListItem | null>(null)
  const [documents, setDocuments] = useState<DocumentListItem[]>([])
  const [loadingDatasets, setLoadingDatasets] = useState(false)
  const [loadingDocs, setLoadingDocs] = useState(false)
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [form] = Form.useForm()
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // -- Load dataset list --
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

  // -- Load document list --
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

  // -- Polling when dataset selected --
  useEffect(() => {
    if (pollingRef.current) { clearInterval(pollingRef.current); pollingRef.current = null }
    if (!selectedDataset) { setDocuments([]); return }
    void loadDocuments(selectedDataset.datasetId)
    pollingRef.current = setInterval(() => { void loadDocuments(selectedDataset.datasetId) }, 5000)
    return () => { if (pollingRef.current) clearInterval(pollingRef.current) }
  }, [selectedDataset, loadDocuments])

  // -- Create dataset --
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

  // -- Delete dataset --
  const handleDeleteDataset = async (ds: DatasetListItem) => {
    try {
      await removeDataset(ds.datasetId)
      setDatasets(prev => prev.filter(d => d.datasetId !== ds.datasetId))
      if (selectedDataset?.datasetId === ds.datasetId) { setSelectedDataset(null) }
      message.success('已删除')
    } catch { message.error('删除失败') }
  }

  // -- Upload document --
  const handleUpload = async (file: File) => {
    if (!selectedDataset) { message.warning('请先选择知识库'); return false }
    setUploading(true)
    try {
      await uploadDocument({ datasetId: selectedDataset.datasetId, file })
      void loadDocuments(selectedDataset.datasetId)
      void loadDatasets()
      message.success('上传成功，正在处理...')
    } catch { message.error('上传失败') } finally { setUploading(false) }
    return false
  }

  // -- Delete document --
  const handleDeleteDoc = async (doc: DocumentListItem) => {
    try {
      await removeDocument(doc.documentId)
      setDocuments(prev => prev.filter(d => d.documentId !== doc.documentId))
      void loadDatasets()
      message.success('文档已删除')
    } catch { message.error('删除失败') }
  }

  // -- Retry document --
  const handleRetry = async (doc: DocumentListItem) => {
    try {
      await retryDocument(doc.documentId)
      void loadDocuments(selectedDataset!.datasetId)
      message.success('已重新提交处理')
    } catch { message.error('重试失败') }
  }

  // -- Filtered datasets --
  const filteredDatasets = datasets.filter(ds =>
    ds.name.toLowerCase().includes(searchText.toLowerCase())
  )

  // -- Document table columns --
  const docColumns: ColumnsType<DocumentListItem> = [
    {
      title: '文件名', dataIndex: 'filename', key: 'filename', ellipsis: true,
      render: (name: string) => (
        <Space><FileTextOutlined style={{ color: '#1677ff' }} />{name}</Space>
      ),
    },
    {
      title: '状态', dataIndex: 'status', key: 'status', width: 120,
      render: (status: string) => {
        const info = STATUS_MAP[status] ?? { color: 'default', label: status }
        return <Tag color={info.color}>{info.label}</Tag>
      },
    },
    {
      title: '分片数', dataIndex: 'totalChunks', key: 'totalChunks', width: 90,
      align: 'center',
    },
    {
      title: '上传时间', dataIndex: 'uploadedAt', key: 'uploadedAt', width: 170,
    },
    {
      title: '操作', key: 'actions', width: 120,
      render: (_: unknown, record: DocumentListItem) => (
        <Space size="small">
          {record.status === 'FAILED' && (
            <Button type="link" size="small" icon={<ReloadOutlined />}
              onClick={() => void handleRetry(record)}>重试</Button>
          )}
          <Popconfirm title="确认删除此文档？" onConfirm={() => void handleDeleteDoc(record)}
            okText="删除" cancelText="取消">
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  // -- Render --
  return (
    <div style={{ display: 'flex', height: '100%' }}>
      {/* Left Panel */}
      <div style={{
        width: 300, flexShrink: 0, background: '#fff', borderRight: '1px solid #f0f0f0',
        padding: 16, display: 'flex', flexDirection: 'column', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <Text strong style={{ fontSize: 16 }}>知识库</Text>
          <Button type="primary" size="small" icon={<PlusOutlined />}
            onClick={() => setCreateModalOpen(true)} />
        </div>

        <Input.Search
          placeholder="搜索知识库"
          prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
          allowClear
          value={searchText}
          onChange={e => setSearchText(e.target.value)}
          style={{ marginBottom: 12 }}
        />

        <div style={{ flex: 1, overflowY: 'auto' }}>
          {loadingDatasets ? (
            <div style={{ textAlign: 'center', paddingTop: 40 }}><Spin /></div>
          ) : filteredDatasets.length === 0 ? (
            <Empty description="暂无知识库" image={Empty.PRESENTED_IMAGE_SIMPLE} />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {filteredDatasets.map(ds => {
                const isSelected = selectedDataset?.datasetId === ds.datasetId
                return (
                  <div
                    key={ds.datasetId}
                    onClick={() => setSelectedDataset(ds)}
                    className="knowledge-dataset-item"
                    style={{
                      padding: '10px 12px', borderRadius: 6, cursor: 'pointer',
                      background: isSelected ? '#e6f7ff' : 'transparent',
                      borderLeft: isSelected ? '3px solid #1677ff' : '3px solid transparent',
                      transition: 'all 0.2s',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <div style={{
                        width: 32, height: 32, borderRadius: '50%', background: isSelected ? '#1677ff' : '#f0f0f0',
                        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
                      }}>
                        <DatabaseOutlined style={{ color: isSelected ? '#fff' : '#8c8c8c', fontSize: 14 }} />
                      </div>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontWeight: 600, fontSize: 14, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {ds.name}
                        </div>
                        <div style={{ color: '#8c8c8c', fontSize: 12 }}>
                          {ds.documentCount} 个文档
                        </div>
                      </div>
                      <Popconfirm title="确认删除此知识库？" onConfirm={() => void handleDeleteDataset(ds)}
                        okText="删除" cancelText="取消">
                        <Button type="text" size="small" danger icon={<DeleteOutlined />}
                          onClick={e => e.stopPropagation()}
                          className="knowledge-delete-btn"
                          style={{ opacity: 0, transition: 'opacity 0.2s' }} />
                      </Popconfirm>
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>

      {/* Right Panel */}
      <div style={{ flex: 1, padding: 24, overflowY: 'auto', background: '#fafafa' }}>
        {!selectedDataset ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
            <Empty
              image={<FolderOutlined style={{ fontSize: 64, color: '#d9d9d9' }} />}
              description={<Text type="secondary">选择或创建一个知识库</Text>}
            />
          </div>
        ) : (
          <>
            <div style={{ marginBottom: 20, display: 'flex', alignItems: 'center', gap: 12 }}>
              <Text strong style={{ fontSize: 18 }}>{selectedDataset.name}</Text>
              <Badge count={documents.length} style={{ backgroundColor: '#1677ff' }}
                overflowCount={999} showZero />
            </div>

            <Upload.Dragger
              beforeUpload={(file) => { void handleUpload(file as unknown as File); return false }}
              showUploadList={false}
              accept=".pdf,.txt,.md,.doc,.docx,.csv"
              disabled={uploading}
              style={{ marginBottom: 20, background: '#fff' }}
            >
              {uploading ? <Spin /> : (
                <>
                  <p className="ant-upload-drag-icon"><InboxOutlined /></p>
                  <p className="ant-upload-text">点击或拖拽文件上传</p>
                  <p className="ant-upload-hint">支持 PDF, TXT, MD, DOC, DOCX, CSV 格式</p>
                </>
              )}
            </Upload.Dragger>

            <Table<DocumentListItem>
              columns={docColumns}
              dataSource={documents}
              rowKey="documentId"
              loading={loadingDocs}
              size="middle"
              pagination={{ pageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 条` }}
              locale={{ emptyText: <Empty description="暂无文档" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
              style={{ background: '#fff', borderRadius: 8 }}
            />
          </>
        )}
      </div>

      {/* Create dataset modal */}
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

      {/* Hover style for delete button */}
      <style>{`
        .knowledge-dataset-item:hover .knowledge-delete-btn {
          opacity: 1 !important;
        }
      `}</style>
    </div>
  )
}
