import { useEffect, useState } from 'react';
import { Card, List, Button, Modal, Form, Input, Table, Tag, Space, Empty, Tooltip } from 'antd';
import { PlusOutlined, DeleteOutlined, FileTextOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { useKnowledge } from '../hooks/useKnowledge';
import { DocumentUpload } from '../components/DocumentUpload';
import { CreateDatasetRequest, DocumentStatus, KnowledgeDocument } from '../types/knowledge';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';

const { TextArea } = Input;

export const KnowledgePage: React.FC = () => {
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
    loadDocuments,
    createDataset,
    deleteDataset,
    uploadDocument,
    deleteDocument,
    retryDocument,
    searchKnowledge,
    setCurrentDataset
  } = useKnowledge();

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isSearchModalOpen, setIsSearchModalOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<string[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [form] = Form.useForm();

  const currentDataset = datasets.find((d) => d.datasetId === currentDatasetId);

  useEffect(() => {
    if (!currentDatasetId) {
      return;
    }
    const hasProcessing = documents.some(
      (doc) =>
        doc.status === DocumentStatus.PENDING || doc.status === DocumentStatus.PROCESSING
    );
    if (!hasProcessing) {
      return;
    }

    const timer = window.setInterval(() => {
      loadDocuments(currentDatasetId, documentPage, documentPageSize).catch(() => undefined);
    }, 3000);

    return () => window.clearInterval(timer);
  }, [currentDatasetId, documents, documentPage, documentPageSize, loadDocuments]);

  const handleCreateDataset = async () => {
    try {
      const values = await form.validateFields();
      const data: CreateDatasetRequest = {
        name: values.name,
        description: values.description
      };
      await createDataset(data);
      setIsCreateModalOpen(false);
      form.resetFields();
    } catch (error) {
      // Error handled in hook
    }
  };

  const handleUpload = async (file: File, chunkSize: number, chunkOverlap: number) => {
    if (!currentDatasetId) return;
    await uploadDocument(file, currentDatasetId, chunkSize, chunkOverlap);
  };

  const handleSearch = async () => {
    if (!currentDatasetId || !searchQuery.trim()) return;

    setSearchLoading(true);
    try {
      const results = await searchKnowledge({
        datasetId: currentDatasetId,
        query: searchQuery.trim(),
        topK: 5
      });
      setSearchResults(results);
    } catch (error) {
      // Error handled in hook
    } finally {
      setSearchLoading(false);
    }
  };

  const getStatusTag = (document: KnowledgeDocument) => {
    const status = document.status;
    const statusConfig = {
      [DocumentStatus.PENDING]: { color: 'default', text: '待处理' },
      [DocumentStatus.PROCESSING]: { color: 'processing', text: '处理中' },
      [DocumentStatus.COMPLETED]: { color: 'success', text: '已完成' },
      [DocumentStatus.FAILED]: { color: 'error', text: '失败' }
    };
    const config = statusConfig[status];
    const tag = <Tag color={config.color}>{config.text}</Tag>;

    if (status === DocumentStatus.FAILED && document.errorMessage) {
      return <Tooltip title={document.errorMessage}>{tag}</Tooltip>;
    }
    return tag;
  };

  const documentColumns: ColumnsType<KnowledgeDocument> = [
    {
      title: '文件名',
      dataIndex: 'filename',
      key: 'filename',
      render: (filename: string) => (
        <Space>
          <FileTextOutlined />
          {filename}
        </Space>
      )
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (_: DocumentStatus, record: KnowledgeDocument) => getStatusTag(record)
    },
    {
      title: '分块数',
      dataIndex: 'totalChunks',
      key: 'totalChunks',
      width: 100,
      render: (total: number, record: KnowledgeDocument) => (
        <span>
          {record.processedChunks} / {total}
        </span>
      )
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 100,
      render: (size: number) => `${(size / 1024).toFixed(2)} KB`
    },
    {
      title: '上传时间',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss')
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: KnowledgeDocument) => (
        <Space>
          {record.status === DocumentStatus.FAILED && (
            <Tooltip title="重试处理">
              <Button
                type="text"
                icon={<ReloadOutlined />}
                onClick={() => retryDocument(record.documentId)}
              />
            </Tooltip>
          )}
          <Tooltip title="删除">
            <Button
              type="text"
              danger
              icon={<DeleteOutlined />}
              onClick={() => deleteDocument(record.documentId, record.filename)}
            />
          </Tooltip>
        </Space>
      )
    }
  ];

  return (
    <div style={{ display: 'flex', height: 'calc(100vh - 150px)', gap: 16 }}>
      {/* Dataset List */}
      <Card
        title="知识库列表"
        style={{ width: 300, overflow: 'auto' }}
        extra={
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => setIsCreateModalOpen(true)}
          >
            新建
          </Button>
        }
      >
        <List
          dataSource={datasets}
          loading={isLoading}
          renderItem={(dataset) => (
            <List.Item
              style={{
                cursor: 'pointer',
                backgroundColor:
                  currentDatasetId === dataset.datasetId ? '#e6f7ff' : 'transparent',
                padding: '8px 12px',
                borderRadius: 4
              }}
              onClick={() => setCurrentDataset(dataset.datasetId)}
              actions={[
                <Button
                  type="text"
                  danger
                  size="small"
                  icon={<DeleteOutlined />}
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteDataset(dataset.datasetId, dataset.name);
                  }}
                />
              ]}
            >
              <List.Item.Meta
                title={dataset.name}
                description={
                  <div>
                    <div>{dataset.description}</div>
                    <div style={{ marginTop: 4, fontSize: 12, color: '#999' }}>
                      {dataset.documentCount} 个文档 · {dataset.totalChunks} 个分块
                    </div>
                  </div>
                }
              />
            </List.Item>
          )}
        />
      </Card>

      {/* Document Area */}
      <Card
        title={currentDataset ? currentDataset.name : '文档管理'}
        style={{ flex: 1, display: 'flex', flexDirection: 'column' }}
        bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column' }}
        extra={
          currentDatasetId && (
            <Button
              icon={<SearchOutlined />}
              onClick={() => setIsSearchModalOpen(true)}
            >
              检索测试
            </Button>
          )
        }
      >
        {!currentDatasetId ? (
          <Empty description="请选择或创建知识库" style={{ marginTop: 100 }} />
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, flex: 1 }}>
            {/* Upload Area */}
            <DocumentUpload
              onUpload={handleUpload}
              uploading={isUploading}
              uploadProgress={uploadProgress}
            />

            {/* Document List */}
            <Table
              columns={documentColumns}
              dataSource={documents}
              rowKey="documentId"
              loading={isLoading}
              pagination={{
                current: documentPage + 1,
                pageSize: documentPageSize,
                total: documentTotal,
                showSizeChanger: true,
                showTotal: (total) => `共 ${total} 条`,
                onChange: (page, size) => {
                  if (currentDatasetId) {
                    loadDocuments(currentDatasetId, page - 1, size);
                  }
                }
              }}
            />
          </div>
        )}
      </Card>

      {/* Create Dataset Modal */}
      <Modal
        title="创建知识库"
        open={isCreateModalOpen}
        onOk={handleCreateDataset}
        onCancel={() => {
          setIsCreateModalOpen(false);
          form.resetFields();
        }}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="名称"
            name="name"
            rules={[{ required: true, message: '请输入知识库名称' }]}
          >
            <Input placeholder="请输入知识库名称" />
          </Form.Item>

          <Form.Item
            label="描述"
            name="description"
            rules={[{ required: true, message: '请输入知识库描述' }]}
          >
            <TextArea placeholder="请输入知识库描述" rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Search Modal */}
      <Modal
        title="知识检索测试"
        open={isSearchModalOpen}
        onOk={handleSearch}
        onCancel={() => {
          setIsSearchModalOpen(false);
          setSearchQuery('');
          setSearchResults([]);
        }}
        okText="检索"
        cancelText="关闭"
        confirmLoading={searchLoading}
        width={700}
      >
        <div style={{ marginBottom: 16 }}>
          <Input.TextArea
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="输入查询内容..."
            rows={3}
          />
        </div>

        {searchResults.length > 0 && (
          <div>
            <h4>检索结果（Top {searchResults.length}）：</h4>
            <List
              dataSource={searchResults}
              renderItem={(result, index) => (
                <List.Item>
                  <div style={{ width: '100%' }}>
                    <div style={{ fontWeight: 'bold', marginBottom: 8 }}>
                      结果 {index + 1}
                    </div>
                    <div style={{ whiteSpace: 'pre-wrap', color: '#666' }}>
                      {result}
                    </div>
                  </div>
                </List.Item>
              )}
            />
          </div>
        )}
      </Modal>
    </div>
  );
};
