import { useEffect, useState } from 'react'
import {
  Card, Table, Button, Modal, Form, Input, Select, Space, Tag, Popconfirm, message, Spin
} from 'antd'
import {
  PlusOutlined, DeleteOutlined, StarOutlined, StarFilled, ThunderboltOutlined
} from '@ant-design/icons'
import {
  getLlmConfigs, createLlmConfig, updateLlmConfig, deleteLlmConfig, testLlmConfig,
  type LlmConfig, type CreateLlmConfigPayload, type UpdateLlmConfigPayload
} from '../api/llmConfigService'

const PROVIDERS = [
  { label: 'OpenAI', value: 'openai' },
  { label: '智谱 (Zhipu)', value: 'zhipu' },
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Ollama', value: 'ollama' },
  { label: '其他', value: 'other' },
]

export default function LlmConfigPage() {
  const [configs, setConfigs] = useState<LlmConfig[]>([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [testingId, setTestingId] = useState<number | null>(null)
  const [form] = Form.useForm()

  const loadConfigs = async () => {
    setLoading(true)
    try {
      const data = await getLlmConfigs()
      setConfigs(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadConfigs() }, [])

  const handleCreate = () => {
    setEditingId(null)
    form.resetFields()
    setModalOpen(true)
  }

  const handleEdit = (record: LlmConfig) => {
    setEditingId(record.id)
    form.setFieldsValue({
      name: record.name,
      provider: record.provider,
      baseUrl: record.baseUrl,
      model: record.model,
      apiKey: '', // 不回显
    })
    setModalOpen(true)
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    if (editingId) {
      const payload: UpdateLlmConfigPayload = {}
      if (values.name) payload.name = values.name
      if (values.baseUrl) payload.baseUrl = values.baseUrl
      if (values.apiKey) payload.apiKey = values.apiKey
      if (values.model) payload.model = values.model
      await updateLlmConfig(editingId, payload)
      message.success('更新成功')
    } else {
      await createLlmConfig(values as CreateLlmConfigPayload)
      message.success('创建成功')
    }
    setModalOpen(false)
    loadConfigs()
  }

  const handleDelete = async (id: number) => {
    await deleteLlmConfig(id)
    message.success('已删除')
    loadConfigs()
  }

  const handleSetDefault = async (id: number) => {
    await updateLlmConfig(id, { isDefault: true })
    message.success('已设为默认')
    loadConfigs()
  }

  const handleTest = async (id: number) => {
    setTestingId(id)
    try {
      const result = await testLlmConfig(id)
      if (result.ok) {
        message.success(`连通成功，延迟 ${result.latencyMs}ms`)
      } else {
        message.error(`连通失败: ${result.error}`)
      }
    } finally {
      setTestingId(null)
    }
  }

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '供应商', dataIndex: 'provider', key: 'provider' },
    { title: '模型', dataIndex: 'model', key: 'model' },
    { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true },
    {
      title: '默认',
      key: 'isDefault',
      render: (_: unknown, record: LlmConfig) =>
        record.isDefault ? <StarFilled style={{ color: '#faad14' }} /> : null,
    },
    {
      title: '状态',
      key: 'status',
      render: (_: unknown, record: LlmConfig) =>
        record.status === 1 ? <Tag color="green">启用</Tag> : <Tag color="red">禁用</Tag>,
    },
    {
      title: '操作',
      key: 'actions',
      render: (_: unknown, record: LlmConfig) => (
        <Space size="small">
          <Button size="small" onClick={() => handleEdit(record)}>编辑</Button>
          {!record.isDefault && (
            <Button size="small" icon={<StarOutlined />} onClick={() => handleSetDefault(record.id)}>
              设为默认
            </Button>
          )}
          <Button
            size="small"
            icon={<ThunderboltOutlined />}
            loading={testingId === record.id}
            onClick={() => handleTest(record.id)}
          >
            测试
          </Button>
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Card
      title="模型配置"
      extra={<Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新增配置</Button>}
    >
      <Spin spinning={loading}>
        <Table
          dataSource={configs}
          columns={columns}
          rowKey="id"
          pagination={false}
          size="middle"
        />
      </Spin>

      <Modal
        title={editingId ? '编辑配置' : '新增配置'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="配置名称" rules={[{ required: true }]}>
            <Input placeholder="如 DeepSeek V3" />
          </Form.Item>
          <Form.Item name="provider" label="供应商" rules={[{ required: !editingId }]}>
            <Select options={PROVIDERS} placeholder="选择供应商" />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL" rules={[{ required: !editingId }]}>
            <Input placeholder="https://api.deepseek.com/v1" />
          </Form.Item>
          <Form.Item name="apiKey" label="API Key" rules={[{ required: !editingId }]}>
            <Input.Password placeholder="sk-..." />
          </Form.Item>
          <Form.Item name="model" label="模型名称" rules={[{ required: !editingId }]}>
            <Input placeholder="deepseek-chat" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  )
}
