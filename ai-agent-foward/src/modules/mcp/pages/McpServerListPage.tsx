import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Button, Table, Space, Modal, Typography, Tag,
  Popconfirm, message, Empty, Spin
} from 'antd'
import {
  PlusOutlined, DeleteOutlined, DisconnectOutlined,
  LinkOutlined, ToolOutlined, FolderOpenOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { McpServer, McpServerConfig, ServerStatus } from '../types/mcp'
import { mcpAdapter } from '../api/mcpAdapter'
import ServerStatusTag from '../components/ServerStatusTag'
import ServerForm from '../components/ServerForm'

const { Title, Text } = Typography

const serverTypeMap: Record<string, { label: string; color: string }> = {
  stdio: { label: 'stdio', color: 'blue' },
  sse: { label: 'SSE', color: 'green' },
  http: { label: 'HTTP', color: 'purple' },
}

export default function McpServerListPage() {
  const navigate = useNavigate()
  const [servers, setServers] = useState<McpServer[]>([])
  const [loading, setLoading] = useState(true)
  const [operating, setOperating] = useState<number | null>(null)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingServer, setEditingServer] = useState<McpServer | null>(null)
  const [formSubmitting, setFormSubmitting] = useState(false)

  const loadServers = useCallback(async () => {
    try {
      const data = await mcpAdapter.listServers()
      setServers(data)
    } catch {
      message.error('加载服务器列表失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadServers()
  }, [loadServers])

  // 轮询 CONNECTING 状态的服务器状态
  useEffect(() => {
    const connectingServers = servers.filter(s => s.status === 'CONNECTING')
    if (connectingServers.length === 0) return

    const interval = setInterval(async () => {
      try {
        const updated = await mcpAdapter.listServers()
        setServers(prev => {
          const connectingIds = new Set(prev.filter(s => s.status === 'CONNECTING').map(s => s.id))
          return updated.map(server => {
            if (connectingIds.has(server.id)) return server
            return prev.find(p => p.id === server.id) || server
          })
        })
      } catch {
        // 忽略轮询错误
      }
    }, 5000)

    return () => clearInterval(interval)
  }, [servers])

  const handleConnect = async (id: number) => {
    setOperating(id)
    try {
      await mcpAdapter.connectServer(id)
      message.success('正在连接服务器...')
      // 触发重新加载，status 会通过轮询更新
      setTimeout(() => { void loadServers() }, 500)
    } catch {
      message.error('连接失败')
    } finally {
      setOperating(null)
    }
  }

  const handleDisconnect = async (id: number) => {
    setOperating(id)
    try {
      await mcpAdapter.disconnectServer(id)
      message.success('已断开连接')
      void loadServers()
    } catch {
      message.error('断开连接失败')
    } finally {
      setOperating(null)
    }
  }

  const handleDelete = async (id: number) => {
    setOperating(id)
    try {
      await mcpAdapter.deleteServer(id)
      message.success('已删除')
      void loadServers()
    } catch {
      message.error('删除失败')
    } finally {
      setOperating(null)
    }
  }

  const handleOpenAdd = () => {
    setEditingServer(null)
    setModalVisible(true)
  }

  const handleOpenEdit = (server: McpServer) => {
    setEditingServer(server)
    setModalVisible(true)
  }

  const handleFormSubmit = async (values: { name: string; formData: McpServerConfig; description: string }) => {
    setFormSubmitting(true)
    try {
      const payload = {
        name: values.name,
        serverType: values.formData.type as 'stdio' | 'sse' | 'http',
        configJson: JSON.stringify(values.formData),
        description: values.description,
        enabled: true,
      }

      if (editingServer) {
        await mcpAdapter.updateServer(editingServer.id, payload)
        message.success('更新成功')
      } else {
        await mcpAdapter.createServer(payload)
        message.success('创建成功')
      }
      setModalVisible(false)
      void loadServers()
    } catch {
      message.error(editingServer ? '更新失败' : '创建失败')
    } finally {
      setFormSubmitting(false)
    }
  }

  const columns: ColumnsType<McpServer> = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: McpServer) => (
        <Button
          type="link"
          className="p-0 h-auto"
          onClick={() => navigate(`/mcp/${record.id}`)}
        >
          {name}
        </Button>
      ),
    },
    {
      title: '类型',
      dataIndex: 'serverType',
      key: 'serverType',
      render: (type: string) => {
        const config = serverTypeMap[type] || { label: type, color: 'default' }
        return <Tag color={config.color}>{config.label}</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: ServerStatus) => <ServerStatusTag status={status} />,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (desc?: string) => (
        <Text type="secondary" ellipsis style={{ maxWidth: 200 }}>
          {desc || '-'}
        </Text>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          {record.status === 'CONNECTED' ? (
            <>
              <Button
                size="small"
                type="primary"
                icon={<FolderOpenOutlined />}
                onClick={() => navigate(`/mcp/${record.id}`)}
              >
                查看工具
              </Button>
              <Button
                size="small"
                danger
                ghost
                icon={<DisconnectOutlined />}
                loading={operating === record.id}
                onClick={() => void handleDisconnect(record.id)}
              >
                断开
              </Button>
            </>
          ) : (
            <Button
              size="small"
              type="primary"
              icon={<LinkOutlined />}
              loading={operating === record.id}
              disabled={record.status === 'CONNECTING'}
              onClick={() => void handleConnect(record.id)}
            >
              连接
            </Button>
          )}
          <Button
            size="small"
            icon={<ToolOutlined />}
            onClick={() => void handleOpenEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确认删除"
            description="删除后不可恢复，确定要删除该服务器吗？"
            onConfirm={() => void handleDelete(record.id)}
            okText="删除"
            okButtonProps={{ danger: true }}
          >
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              loading={operating === record.id}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      {/* Page Header */}
      <div style={{
        display: 'flex', justifyContent: 'space-between',
        alignItems: 'center', marginBottom: 24,
      }}>
        <Title level={4} style={{ margin: 0 }}>
          MCP 服务器管理
        </Title>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => void handleOpenAdd()}
        >
          添加服务器
        </Button>
      </div>

      {/* Content */}
      {loading ? (
        <div style={{ textAlign: 'center', paddingTop: 80 }}>
          <Spin size="large" />
        </div>
      ) : servers.length === 0 ? (
        <Empty
          description="还没有配置 MCP 服务器"
          style={{ paddingTop: 80 }}
        >
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => void handleOpenAdd()}
          >
            添加第一个服务器
          </Button>
        </Empty>
      ) : (
        <Table
          columns={columns}
          dataSource={servers}
          rowKey="id"
          pagination={false}
          style={{ background: '#fff', borderRadius: 8 }}
        />
      )}

      {/* Add/Edit Modal */}
      <Modal
        title={editingServer ? '编辑 MCP 服务器' : '添加 MCP 服务器'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={560}
        destroyOnClose
      >
        <div style={{ paddingTop: 16 }}>
          <ServerForm
            initialName={editingServer?.name}
            initialDescription={editingServer?.description}
            initialValues={editingServer?.config}
            onSubmit={handleFormSubmit}
            onCancel={() => setModalVisible(false)}
            loading={formSubmitting}
          />
        </div>
      </Modal>
    </div>
  )
}
