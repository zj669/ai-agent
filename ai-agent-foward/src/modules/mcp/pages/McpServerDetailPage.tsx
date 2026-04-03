import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Button, Card, Spin, Typography, Space, Tag,
  Descriptions, Empty, message, Divider
} from 'antd'
import {
  ArrowLeftOutlined, DisconnectOutlined,
  ReloadOutlined, LinkOutlined, InfoCircleOutlined
} from '@ant-design/icons'
import type { McpServer, McpTool } from '../types/mcp'
import { mcpAdapter } from '../api/mcpAdapter'
import ServerStatusTag from '../components/ServerStatusTag'

const { Title, Text, Paragraph } = Typography

const serverTypeMap: Record<string, { label: string; color: string }> = {
  stdio: { label: 'Stdio', color: 'blue' },
  sse: { label: 'SSE', color: 'green' },
  http: { label: 'HTTP', color: 'purple' },
}

function ConfigDisplay({ server }: { server: McpServer }) {
  const { config } = server
  if (!config) return null

  const items: { label: string; value: React.ReactNode }[] = []

  if (config.command) {
    items.push({ label: 'Command', value: <Text code copyable>{config.command}</Text> })
  }
  if (config.args && config.args.length > 0) {
    items.push({ label: 'Args', value: <Text code copyable>{config.args.join(' ')}</Text> })
  }
  if (config.url) {
    items.push({ label: 'URL', value: <Text code copyable>{config.url}</Text> })
  }
  if (config.endpoint) {
    items.push({ label: 'Endpoint', value: <Text code copyable>{config.endpoint}</Text> })
  }
  if (config.headers && Object.keys(config.headers).length > 0) {
    const headersStr = Object.entries(config.headers)
      .map(([k, v]) => `${k}: ${v}`)
      .join('\n')
    items.push({ label: 'Headers', value: <pre className="text-xs bg-gray-50 p-2 rounded">{headersStr}</pre> })
  }
  if (config.env && Object.keys(config.env).length > 0) {
    const envKeys = Object.keys(config.env).join(', ')
    items.push({ label: 'Env Variables', value: <Text type="secondary">{envKeys}</Text> })
  }

  if (items.length === 0) return null

  return (
    <>
      <Divider style={{ margin: '12px 0' }} />
      <Descriptions size="small" column={1} items={items} />
    </>
  )
}

interface JsonSchemaProperty {
  type?: string;
  description?: string;
  [key: string]: unknown;
}

function ToolCard({ tool }: { tool: McpTool }) {
  const [schemaExpanded, setSchemaExpanded] = useState(false)

  const inputSchema = tool.inputSchema
    ? (JSON.parse(tool.inputSchema) as { properties?: Record<string, JsonSchemaProperty>; required?: string[] } | null)
    : null
  const hasParams = inputSchema && inputSchema.properties && Object.keys(inputSchema.properties).length > 0

  return (
    <Card
      size="small"
      className="hover:shadow-md transition-shadow"
      title={
        <Space>
          <Text strong>{tool.toolName}</Text>
          <Tag color="cyan">{tool.serverName}</Tag>
        </Space>
      }
      extra={
        hasParams && (
          <Button
            size="small"
            type="text"
            onClick={() => setSchemaExpanded(v => !v)}
          >
            {schemaExpanded ? 'Hide params' : 'Show params'}
          </Button>
        )
      }
    >
      {tool.description ? (
        <Paragraph type="secondary" ellipsis={{ rows: 2 }} className="mb-3">
          {tool.description}
        </Paragraph>
      ) : (
        <Text type="secondary" className="italic">No description</Text>
      )}

      {schemaExpanded && hasParams && (
        <div className="mt-3">
          <Text strong className="text-xs text-gray-500 uppercase tracking-wide">Input Parameters</Text>
          <div className="mt-1">
            {Object.entries(inputSchema!.properties ?? {}).map(([key, prop]) => (
              <div key={key} className="flex gap-2 text-sm py-1 border-b border-gray-100 last:border-0">
                <Text code className="min-w-24">{key}</Text>
                <Text type="secondary">{prop.type}</Text>
                {prop.description && (
                  <Text type="secondary" className="truncate flex-1">{prop.description}</Text>
                )}
                {inputSchema.required?.includes(key) && (
                  <Tag color="red" className="text-xs">required</Tag>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </Card>
  )
}

export default function McpServerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const serverId = Number(id)

  const [server, setServer] = useState<McpServer | null>(null)
  const [tools, setTools] = useState<McpTool[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshingTools, setRefreshingTools] = useState(false)
  const [disconnecting, setDisconnecting] = useState(false)
  const [notFound, setNotFound] = useState(false)

  const loadServer = useCallback(async () => {
    try {
      const data = await mcpAdapter.getServer(serverId)
      setServer(data)
    } catch {
      message.error('加载服务器详情失败')
      setNotFound(true)
    }
  }, [serverId])

  const loadTools = useCallback(async () => {
    setRefreshingTools(true)
    try {
      const data = await mcpAdapter.getServerTools(serverId)
      setTools(data)
    } catch {
      message.error('加载工具列表失败')
    } finally {
      setRefreshingTools(false)
    }
  }, [serverId])

  useEffect(() => {
    if (!id || isNaN(serverId)) {
      setNotFound(true)
      return
    }
    setLoading(true)
    Promise.all([loadServer(), loadTools()]).finally(() => setLoading(false))
  }, [id, serverId, loadServer, loadTools])

  // Poll status when CONNECTING
  useEffect(() => {
    if (!server) return
    if (server.status !== 'CONNECTING') return

    const interval = setInterval(async () => {
      try {
        const status = await mcpAdapter.getServerStatus(serverId)
        setServer(prev => prev ? { ...prev, status } : prev)
        if (status === 'CONNECTED') {
          void loadTools()
        }
      } catch {
        // ignore
      }
    }, 3000)

    return () => clearInterval(interval)
  }, [server, serverId, loadTools])

  const handleDisconnect = async () => {
    setDisconnecting(true)
    try {
      await mcpAdapter.disconnectServer(serverId)
      message.success('已断开连接')
      void loadServer()
      setTools([])
    } catch {
      message.error('断开连接失败')
    } finally {
      setDisconnecting(false)
    }
  }

  const handleRefreshTools = async () => {
    await loadTools()
    message.success('工具列表已刷新')
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', paddingTop: 120 }}>
        <Spin size="large" />
      </div>
    )
  }

  if (notFound || !server) {
    return (
      <div style={{ padding: 24 }}>
        <Empty description="服务器不存在或无权访问" />
        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/mcp')}>
            返回列表
          </Button>
        </div>
      </div>
    )
  }

  const typeConfig = serverTypeMap[server.serverType] || { label: server.serverType, color: 'default' }

  return (
    <div style={{ padding: 24 }}>
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/mcp')}>
          返回
        </Button>
        <Title level={4} style={{ margin: 0, flex: 1 }}>{server.name}</Title>
        <Space>
          {server.status === 'CONNECTED' && (
            <>
              <Button
                icon={<ReloadOutlined />}
                loading={refreshingTools}
                onClick={() => void handleRefreshTools()}
              >
                刷新工具
              </Button>
              <Button
                danger
                icon={<DisconnectOutlined />}
                loading={disconnecting}
                onClick={() => void handleDisconnect()}
              >
                断开连接
              </Button>
            </>
          )}
          {server.status !== 'CONNECTED' && server.status !== 'CONNECTING' && (
            <Button
              type="primary"
              icon={<LinkOutlined />}
              onClick={async () => {
                try {
                  await mcpAdapter.connectServer(serverId)
                  message.success('正在连接...')
                  void loadServer()
                } catch {
                  message.error('连接失败')
                }
              }}
            >
              连接
            </Button>
          )}
        </Space>
      </div>

      {/* Server Info Card */}
      <Card
        size="small"
        className="mb-4"
        title={
          <Space>
            <InfoCircleOutlined />
            <span>服务器信息</span>
          </Space>
        }
      >
        <Descriptions size="small" column={2}>
          <Descriptions.Item label="名称">{server.name}</Descriptions.Item>
          <Descriptions.Item label="类型">
            <Tag color={typeConfig.color}>{typeConfig.label}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <ServerStatusTag status={server.status} />
          </Descriptions.Item>
          <Descriptions.Item label="启用状态">
            {server.enabled ? (
              <Tag color="green">启用</Tag>
            ) : (
              <Tag color="red">禁用</Tag>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="创建时间">
            {server.createTime ? new Date(server.createTime).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          <Descriptions.Item label="更新时间">
            {server.updateTime ? new Date(server.updateTime).toLocaleString('zh-CN') : '-'}
          </Descriptions.Item>
          {server.description && (
            <Descriptions.Item label="描述" span={2}>
              {server.description}
            </Descriptions.Item>
          )}
        </Descriptions>
        <ConfigDisplay server={server} />
      </Card>

      {/* Tools Section */}
      <Card
        size="small"
        title={
          <Space>
            <Text strong>工具列表</Text>
            {server.status === 'CONNECTED' && tools.length > 0 && (
              <Tag>{tools.length} 个工具</Tag>
            )}
          </Space>
        }
        extra={
          server.status === 'CONNECTED' && tools.length > 0 && (
            <Button size="small" icon={<ReloadOutlined />} loading={refreshingTools} onClick={() => void handleRefreshTools()}>
              刷新
            </Button>
          )
        }
      >
        {server.status !== 'CONNECTED' ? (
          <Empty
            description={
              server.status === 'CONNECTING'
                ? '正在连接服务器...'
                : server.status === 'ERROR'
                ? '连接失败，请检查配置后重试'
                : '服务器未连接，无法查看工具'
            }
          >
            {server.status === 'ERROR' && (
              <Button type="primary" icon={<LinkOutlined />} onClick={async () => {
                try {
                  await mcpAdapter.connectServer(serverId)
                  message.success('正在重连...')
                  void loadServer()
                } catch {
                  message.error('重连失败')
                }
              }}>
                重连
              </Button>
            )}
          </Empty>
        ) : tools.length === 0 ? (
          <Empty description="该服务器暂未暴露任何工具" />
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {tools.map(tool => (
              <ToolCard key={tool.fullName} tool={tool} />
            ))}
          </div>
        )}
      </Card>
    </div>
  )
}
